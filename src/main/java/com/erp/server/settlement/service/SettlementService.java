package com.erp.server.settlement.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.user.domain.AppUser;
import com.erp.server.master.customer.domain.Customer;
import com.erp.server.master.customer.repository.CustomerRepository;
import com.erp.server.master.supplier.domain.Supplier;
import com.erp.server.master.supplier.repository.SupplierRepository;
import com.erp.server.settlement.domain.Payment;
import com.erp.server.settlement.domain.PaymentAllocation;
import com.erp.server.settlement.domain.PaymentMethod;
import com.erp.server.settlement.domain.PaymentStatus;
import com.erp.server.settlement.domain.Voucher;
import com.erp.server.settlement.domain.VoucherItem;
import com.erp.server.settlement.domain.VoucherType;
import com.erp.server.settlement.repository.PaymentAllocationRepository;
import com.erp.server.settlement.repository.PaymentRepository;
import com.erp.server.settlement.repository.VoucherRepository;

import lombok.RequiredArgsConstructor;

// ********** 원본 완료 업무의 전표 생성과 매출 전표 정산·입금 자동 배분·거래처 총미수금 동기화를 공통 처리하기 위한 Service 클래스 **********
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

	private static final int QUANTITY_SCALE = 3;
	private static final int AMOUNT_SCALE = 2;
	private static final String SALES_RETURN_RELEASE_REASON = "매출 반품에 따른 초과 입금 배분 자동 해제";

	private final CustomerRepository customerRepository;
	private final SupplierRepository supplierRepository;
	private final VoucherRepository voucherRepository;
	private final PaymentRepository paymentRepository;
	private final PaymentAllocationRepository paymentAllocationRepository;

	// ========== 출고 완료 결과로 SALES 전표를 생성하고 미배분 입금 자동 배분과 거래처 총미수금을 함께 갱신하는 메서드 ==========
	@Transactional
	public Voucher createSalesVoucher(Long customerId, LocalDate voucherDate, Long shipmentId,
			List<VoucherItemInput> itemInputs, AppUser processedBy) {

		validateVoucherCommonInput(voucherDate, shipmentId, itemInputs);
		validateProcessedBy(processedBy);
		Customer customer = findCustomerForUpdate(customerId);
		validateShipmentVoucherDoesNotExist(shipmentId);

		Voucher voucher = Voucher.createSales(customer, voucherDate, shipmentId);
		addVoucherItems(voucher, itemInputs);
		voucher.initializeSalesSettlement();
		saveVoucher(voucher);

		allocateUnallocatedPayments(customer, processedBy);
		updateCustomerTotalReceivable(customer, processedBy);
		return voucher;
	}

	// ========== 거래처 반품 완료 결과로 SALES_RETURN 전표를 생성하고 초과 배분 재조정과 총미수금을 함께 갱신하는 메서드 ==========
	// 초과 배분은 최근 배분부터 필요한 금액만 해제하고 다른 오래된 미결 SALES 전표에 자동 재배분한다.
	@Transactional
	public Voucher createSalesReturnVoucher(Long originalSalesVoucherId, LocalDate voucherDate,
			Long customerReturnId, List<VoucherItemInput> itemInputs, AppUser processedBy) {

		validateVoucherCommonInput(voucherDate, customerReturnId, itemInputs);
		validateProcessedBy(processedBy);

		Voucher originalSnapshot = findVoucher(originalSalesVoucherId);
		validateSalesVoucher(originalSnapshot);
		Customer customer = findCustomerForUpdate(originalSnapshot.getCustomer().getCustomerId());
		List<Voucher> salesVouchers = voucherRepository.findSalesVouchersForUpdate(customer.getCustomerId());
		Voucher originalVoucher = findLockedSalesVoucher(salesVouchers, originalSalesVoucherId);
		validateOriginalSalesVoucher(originalVoucher, customer);
		validateCustomerReturnVoucherDoesNotExist(customerReturnId);

		Voucher voucher = Voucher.createSalesReturn(customer, originalVoucher, voucherDate, customerReturnId);
		addVoucherItems(voucher, itemInputs);
		saveVoucher(voucher);

		adjustSalesReturnSettlement(originalVoucher, salesVouchers, processedBy);
		updateCustomerTotalReceivable(customer, processedBy);
		return voucher;
	}

	// ========== 입고 검수 완료 결과로 PURCHASE 전표를 생성하는 메서드 ==========
	@Transactional
	public Voucher createPurchaseVoucher(Long supplierId, LocalDate voucherDate, Long receiptId,
			List<VoucherItemInput> itemInputs) {

		validateVoucherCommonInput(voucherDate, receiptId, itemInputs);
		Supplier supplier = findSupplier(supplierId);
		validateReceiptVoucherDoesNotExist(receiptId);

		Voucher voucher = Voucher.createPurchase(supplier, voucherDate, receiptId);
		addVoucherItems(voucher, itemInputs);
		saveVoucher(voucher);
		return voucher;
	}

	// ========== 매입 반품 완료 결과로 PURCHASE_RETURN 전표를 생성하고 원본 PURCHASE 전표를 연결하는 메서드 ==========
	@Transactional
	public Voucher createPurchaseReturnVoucher(Long originalPurchaseVoucherId, LocalDate voucherDate,
			Long purchaseReturnId, List<VoucherItemInput> itemInputs) {

		validateVoucherCommonInput(voucherDate, purchaseReturnId, itemInputs);
		Voucher originalVoucher = findVoucherForUpdate(originalPurchaseVoucherId);
		validateOriginalPurchaseVoucher(originalVoucher);
		validatePurchaseReturnVoucherDoesNotExist(purchaseReturnId);

		Voucher voucher = Voucher.createPurchaseReturn(originalVoucher.getSupplier(), originalVoucher,
				voucherDate, purchaseReturnId);
		addVoucherItems(voucher, itemInputs);
		saveVoucher(voucher);
		return voucher;
	}

	// ========== 연결 매출 반품과 유효 입금 배분 합계로 단일 SALES 전표와 거래처 총미수금을 다시 계산하는 메서드 ==========
	// CUSTOMER를 먼저 잠근 뒤 VOUCHER를 잠가 공통 정산 잠금 순서를 유지한다.
	@Transactional
	public Voucher recalculateSalesVoucher(Long voucherId, AppUser processedBy) {
		validateProcessedBy(processedBy);
		Voucher voucherSnapshot = findVoucher(voucherId);
		validateSalesVoucher(voucherSnapshot);
		Customer customer = findCustomerForUpdate(voucherSnapshot.getCustomer().getCustomerId());
		Voucher voucher = findVoucherForUpdate(voucherId);
		recalculateSalesVoucher(voucher);
		updateCustomerTotalReceivable(customer, processedBy);
		return voucher;
	}

	// ========== 거래처의 모든 SALES 전표를 다시 계산하고 미수 잔액 합계로 CUSTOMER.total_receivable_amount를 동기화하는 메서드 ==========
	@Transactional
	public BigDecimal recalculateCustomerTotalReceivable(Long customerId, AppUser processedBy) {
		validateProcessedBy(processedBy);
		Customer customer = findCustomerForUpdate(customerId);
		List<Voucher> salesVouchers = voucherRepository.findSalesVouchersForUpdate(customerId);

		for (Voucher voucher : salesVouchers) {
			recalculateSalesVoucher(voucher);
		}

		BigDecimal totalReceivable = salesVouchers.stream().map(Voucher::getOutstandingAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		customer.updateTotalReceivableAmount(totalReceivable, processedBy);
		return totalReceivable;
	}

	// ========== 거래처의 ACTIVE 미배분 입금을 오래된 순서로 조회하여 오래된 미결 SALES 전표부터 자동 배분하는 메서드 ==========
	@Transactional
	public void allocateUnallocatedPayments(Long customerId, AppUser processedBy) {
		validateProcessedBy(processedBy);
		Customer customer = findCustomerForUpdate(customerId);
		allocateUnallocatedPayments(customer, processedBy);
		updateCustomerTotalReceivable(customer, processedBy);
	}

	// ========== 신규 거래처 입금을 등록하고 오래된 미결 SALES 전표부터 자동 배분한 뒤 총미수금을 갱신하는 메서드 ==========
	@Transactional
	public Payment createAndAllocatePayment(Long customerId, LocalDate paymentDate, BigDecimal amount,
			PaymentMethod method, String memo, AppUser createdBy) {

		validatePaymentInput(paymentDate, amount, method, memo, createdBy);
		Customer customer = findCustomerForUpdate(customerId);
		List<Voucher> outstandingVouchers = voucherRepository.findOutstandingSalesVouchersForUpdate(customerId);
		BigDecimal totalOutstandingAmount = sumOutstandingAmount(outstandingVouchers);

		if (amount.compareTo(totalOutstandingAmount) > 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "입금액은 거래처의 최신 총미수금을 초과할 수 없습니다.");
		}

		Payment payment = Payment.create(customer, paymentDate, amount, method, normalizeMemo(memo), createdBy);
		paymentRepository.saveAndFlush(payment);
		allocatePaymentToVouchers(payment, outstandingVouchers, createdBy);
		updateCustomerTotalReceivable(customer, createdBy);
		return payment;
	}

	// ========== ACTIVE 입금을 취소하고 해당 입금의 활성 배분만 해제한 뒤 전표 정산과 총미수금을 갱신하는 메서드 ==========
	@Transactional
	public Payment cancelPayment(Long paymentId, String reason, AppUser canceledBy) {
		validateIdentifier(paymentId);
		validateProcessedBy(canceledBy);
		String normalizedReason = requireReason(reason);

		Payment paymentSnapshot = paymentRepository.findById(paymentId)
				.orElseThrow(() -> createPaymentNotFoundException());
		Long customerId = paymentSnapshot.getCustomer().getCustomerId();

		Customer customer = findCustomerForUpdate(customerId);
		voucherRepository.findSalesVouchersForUpdate(customerId);
		Payment payment = paymentRepository.findByIdForUpdate(paymentId)
				.orElseThrow(() -> createPaymentNotFoundException());
		validateActivePayment(payment);

		List<PaymentAllocation> allocations = paymentAllocationRepository
				.findActiveByPaymentIdForUpdate(paymentId);
		for (PaymentAllocation allocation : allocations) {
			Voucher voucher = allocation.getVoucher();
			allocation.release(canceledBy, normalizedReason);
			voucher.releaseAllocation(allocation.getAllocatedAmount());
		}

		payment.cancel(canceledBy, normalizedReason);
		updateCustomerTotalReceivable(customer, canceledBy);
		return payment;
	}

	// ========== CUSTOMER 잠금 이후 미결 VOUCHER와 미배분 PAYMENT를 고정 순서로 잠가 자동 배분하는 메서드 ==========
	private void allocateUnallocatedPayments(Customer customer, AppUser processedBy) {
		List<Voucher> outstandingVouchers = voucherRepository
				.findOutstandingSalesVouchersForUpdate(customer.getCustomerId());
		List<Payment> unallocatedPayments = paymentRepository
				.findUnallocatedPaymentsForUpdate(customer.getCustomerId());

		for (Payment payment : unallocatedPayments) {
			allocatePaymentToVouchers(payment, outstandingVouchers, processedBy);
		}
	}

	// ========== 한 입금의 미배분 금액을 오래된 미결 SALES 전표부터 미수 잔액 범위 안에서 배분하는 메서드 ==========
	private void allocatePaymentToVouchers(Payment payment, List<Voucher> outstandingVouchers,
			AppUser processedBy) {

		for (Voucher voucher : outstandingVouchers) {
			if (!payment.hasUnallocatedAmount()) {
				break;
			}

			if (voucher.getOutstandingAmount().signum() <= 0) {
				continue;
			}

			BigDecimal allocatedAmount = payment.getUnallocatedAmount().min(voucher.getOutstandingAmount());
			PaymentAllocation allocation = PaymentAllocation.create(payment, voucher, allocatedAmount, processedBy);

			payment.allocate(allocatedAmount);
			voucher.applyAllocation(allocatedAmount);
			paymentAllocationRepository.save(allocation);
		}
	}

	// ========== 매출 반품 반영 후 원본 SALES 전표의 초과 배분을 해제하고 다른 미결 전표에 재배분하는 메서드 ==========
	private void adjustSalesReturnSettlement(Voucher originalVoucher, List<Voucher> salesVouchers,
			AppUser processedBy) {

		BigDecimal settlementTargetAmount = calculateSettlementTargetAmount(originalVoucher);
		BigDecimal allocatedAmount = paymentAllocationRepository
				.sumActiveAllocatedAmountByVoucherId(originalVoucher.getVoucherId());

		if (allocatedAmount.compareTo(settlementTargetAmount) <= 0) {
			originalVoucher.updateSettlement(settlementTargetAmount, allocatedAmount);
			return;
		}

		BigDecimal excessAmount = allocatedAmount.subtract(settlementTargetAmount);
		List<PaymentAllocation> activeAllocations = paymentAllocationRepository
				.findActiveByVoucherIdForUpdate(originalVoucher.getVoucherId());
		List<Payment> affectedPayments = lockAllocationPayments(activeAllocations);

		List<Payment> releasedPayments = releaseExcessAllocations(
				activeAllocations, affectedPayments, excessAmount, processedBy);
		recalculateSalesVoucher(originalVoucher);

		List<Voucher> otherOutstandingVouchers = salesVouchers.stream()
				.filter(voucher -> !voucher.getVoucherId().equals(originalVoucher.getVoucherId()))
				.filter(voucher -> voucher.getOutstandingAmount().signum() > 0)
				.toList();
		releasedPayments.stream()
				.sorted(Comparator.comparing(Payment::getPaymentDate).thenComparing(Payment::getPaymentId))
				.forEach(payment -> allocatePaymentToVouchers(payment, otherOutstandingVouchers, processedBy));
	}

	// ========== 초과 배분에 연결된 PAYMENT를 식별자 오름차순의 고정 순서로 잠그고 Entity 목록을 반환하는 메서드 ==========
	private List<Payment> lockAllocationPayments(List<PaymentAllocation> allocations) {
		List<Long> paymentIds = allocations.stream().map(allocation -> allocation.getPayment().getPaymentId())
				.collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), List::copyOf));
		List<Payment> payments = paymentRepository.findAllByIdForUpdate(paymentIds);

		if (payments.size() != paymentIds.size()) {
			throw createPaymentNotFoundException();
		}
		return payments;
	}

	// ========== 최근 활성 배분부터 초과 금액만 전액 또는 일부 해제하고 해당 입금의 미배분 금액으로 복원하는 메서드 ==========
	private List<Payment> releaseExcessAllocations(List<PaymentAllocation> allocations, List<Payment> payments,
			BigDecimal excessAmount, AppUser processedBy) {

		Map<Long, Payment> paymentById = payments.stream()
				.collect(Collectors.toMap(Payment::getPaymentId, Function.identity()));
		LinkedHashSet<Long> releasedPaymentIds = new LinkedHashSet<>();
		BigDecimal remainingExcessAmount = excessAmount;

		for (PaymentAllocation allocation : allocations) {
			if (remainingExcessAmount.signum() == 0) {
				break;
			}

			BigDecimal releasedAmount = remainingExcessAmount.min(allocation.getAllocatedAmount());
			Payment payment = paymentById.get(allocation.getPayment().getPaymentId());

			if (releasedAmount.compareTo(allocation.getAllocatedAmount()) == 0) {
				allocation.release(processedBy, SALES_RETURN_RELEASE_REASON);
			} else {
				PaymentAllocation releasedAllocation = allocation.splitReleasedAmount(
						releasedAmount, processedBy, SALES_RETURN_RELEASE_REASON);
				paymentAllocationRepository.save(releasedAllocation);
			}

			payment.restoreUnallocatedAmount(releasedAmount);
			releasedPaymentIds.add(payment.getPaymentId());
			remainingExcessAmount = remainingExcessAmount.subtract(releasedAmount);
		}

		if (remainingExcessAmount.signum() > 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "해제할 수 있는 활성 입금 배분액이 부족합니다.");
		}

		return payments.stream().filter(payment -> releasedPaymentIds.contains(payment.getPaymentId())).toList();
	}

	// ========== SALES 전표의 원금·연결 반품·활성 배분 합계로 정산 대상·미수 잔액·상태를 갱신하는 메서드 ==========
	private void recalculateSalesVoucher(Voucher voucher) {
		BigDecimal settlementTargetAmount = calculateSettlementTargetAmount(voucher);
		BigDecimal allocatedAmount = paymentAllocationRepository
				.sumActiveAllocatedAmountByVoucherId(voucher.getVoucherId());

		if (allocatedAmount.compareTo(settlementTargetAmount) > 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "유효 입금 배분액이 전표의 정산 대상 금액을 초과합니다.");
		}

		voucher.updateSettlement(settlementTargetAmount, allocatedAmount);
	}

	// ========== SALES 전표 금액에 연결 SALES_RETURN 금액을 합산하여 최신 정산 대상 금액을 계산하는 메서드 ==========
	private BigDecimal calculateSettlementTargetAmount(Voucher voucher) {
		BigDecimal salesReturnAmount = voucherRepository.sumSalesReturnAmount(voucher.getVoucherId());
		BigDecimal settlementTargetAmount = voucher.getTotalAmount().add(salesReturnAmount);

		if (settlementTargetAmount.signum() < 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "매출 반품 금액은 원본 매출 전표 금액을 초과할 수 없습니다.");
		}
		return settlementTargetAmount;
	}

	// ========== 거래처 잠금 후 확보한 SALES 전표 목록에서 대상 원본 전표를 찾는 메서드 ==========
	private Voucher findLockedSalesVoucher(List<Voucher> salesVouchers, Long voucherId) {
		return salesVouchers.stream().filter(voucher -> voucher.getVoucherId().equals(voucherId))
				.findFirst().orElseThrow(() -> createVoucherNotFoundException());
	}

	// ========== 전표 품목 입력 순서대로 1부터 lineNo를 부여하여 VOUCHER_ITEM 스냅샷을 생성하는 메서드 ==========
	private void addVoucherItems(Voucher voucher, List<VoucherItemInput> itemInputs) {
		for (int index = 0; index < itemInputs.size(); index++) {
			VoucherItemInput input = itemInputs.get(index);
			validateVoucherItemInput(input);
			voucher.addItem(VoucherItem.create(voucher, index + 1, input.item(), input.quantity(), input.unitPrice()));
		}
	}

	// ========== 전표와 품목 스냅샷을 즉시 저장하여 원본 업무별 UNIQUE 제약조건 충돌을 확인하는 메서드 ==========
	private void saveVoucher(Voucher voucher) {
		try {
			voucherRepository.saveAndFlush(voucher);
		} catch (DataIntegrityViolationException exception) {
			throw new BusinessException(ErrorCode.CONFLICT, "해당 원본 업무의 전표가 이미 생성되었습니다.");
		}
	}

	// ========== 현재 전표별 미수 잔액 합계를 CUSTOMER.total_receivable_amount에 저장하는 메서드 ==========
	private void updateCustomerTotalReceivable(Customer customer, AppUser updatedBy) {
		BigDecimal totalReceivable = voucherRepository
				.sumOutstandingAmountByCustomerId(customer.getCustomerId());
		customer.updateTotalReceivableAmount(totalReceivable, updatedBy);
	}

	// ========== 목록에 포함된 미결 SALES 전표의 최신 미수 잔액을 합산하는 메서드 ==========
	private BigDecimal sumOutstandingAmount(List<Voucher> vouchers) {
		return vouchers.stream().map(Voucher::getOutstandingAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	// ========== customerId로 거래처를 비관적 잠금 조회하고 없으면 404 예외를 발생시키는 메서드 ==========
	private Customer findCustomerForUpdate(Long customerId) {
		validateIdentifier(customerId);
		return customerRepository.findByIdForUpdate(customerId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "거래처를 찾을 수 없습니다."));
	}

	// ========== supplierId로 공급업체를 조회하고 없으면 404 예외를 발생시키는 메서드 ==========
	private Supplier findSupplier(Long supplierId) {
		validateIdentifier(supplierId);
		return supplierRepository.findById(supplierId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "공급업체를 찾을 수 없습니다."));
	}

	// ========== voucherId로 전표를 일반 조회하고 없으면 404 예외를 발생시키는 메서드 ==========
	private Voucher findVoucher(Long voucherId) {
		validateIdentifier(voucherId);
		return voucherRepository.findById(voucherId).orElseThrow(() -> createVoucherNotFoundException());
	}

	// ========== voucherId로 전표를 비관적 잠금 조회하고 없으면 404 예외를 발생시키는 메서드 ==========
	private Voucher findVoucherForUpdate(Long voucherId) {
		validateIdentifier(voucherId);
		return voucherRepository.findByIdForUpdate(voucherId)
				.orElseThrow(() -> createVoucherNotFoundException());
	}

	// ========== 신규 전표의 전표 일자·원본 업무 식별자·품목 목록을 공통 검증하는 메서드 ==========
	private void validateVoucherCommonInput(LocalDate voucherDate, Long originId,
			List<VoucherItemInput> itemInputs) {

		if (voucherDate == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "전표 일자를 확인할 수 없습니다.");
		}
		validateIdentifier(originId);

		if (itemInputs == null || itemInputs.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "전표 품목은 하나 이상이어야 합니다.");
		}

		if (itemInputs.size() > 99999) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "전표 품목 순번 범위를 초과했습니다.");
		}
	}

	// ========== 전표 품목의 품목·수량·단가와 DB 소수 자릿수를 검증하는 메서드 ==========
	private void validateVoucherItemInput(VoucherItemInput input) {
		if (input == null || input.item() == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "전표 품목을 확인할 수 없습니다.");
		}

		if (input.quantity() == null || input.quantity().signum() <= 0
				|| input.quantity().stripTrailingZeros().scale() > QUANTITY_SCALE) {
			throw new BusinessException(ErrorCode.INVALID_INPUT,
					"전표 품목 수량은 0보다 크고 소수점 셋째 자리 이하여야 합니다.");
		}

		if (input.unitPrice() == null || input.unitPrice().signum() < 0
				|| input.unitPrice().stripTrailingZeros().scale() > AMOUNT_SCALE) {
			throw new BusinessException(ErrorCode.INVALID_INPUT,
					"전표 품목 단가는 0 이상이고 소수점 둘째 자리 이하여야 합니다.");
		}
	}

	// ========== 신규 입금의 입금일·금액·방법·메모·등록 사용자를 검증하는 메서드 ==========
	private void validatePaymentInput(LocalDate paymentDate, BigDecimal amount, PaymentMethod method,
			String memo, AppUser createdBy) {

		if (paymentDate == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "입금일을 입력해 주세요.");
		}

		if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > AMOUNT_SCALE) {
			throw new BusinessException(ErrorCode.INVALID_INPUT,
					"입금액은 0보다 크고 소수점 둘째 자리 이하여야 합니다.");
		}

		if (method == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "입금 방법을 선택해 주세요.");
		}

		if (memo != null && memo.trim().length() > 2000) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "입금 메모는 2000자 이하로 입력해 주세요.");
		}
		validateProcessedBy(createdBy);
	}

	// ========== 원본 전표가 거래처와 일치하는 SALES 유형인지 검증하는 메서드 ==========
	private void validateOriginalSalesVoucher(Voucher voucher, Customer customer) {
		validateSalesVoucher(voucher);
		if (!voucher.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
			throw new BusinessException(ErrorCode.CONFLICT, "원본 매출 전표의 거래처가 일치하지 않습니다.");
		}
	}

	// ========== 전표가 정산 대상 SALES 유형인지 검증하는 메서드 ==========
	private void validateSalesVoucher(Voucher voucher) {
		if (voucher.getType() != VoucherType.SALES) {
			throw new BusinessException(ErrorCode.CONFLICT, "매출 전표만 매출 정산 대상으로 처리할 수 있습니다.");
		}
	}

	// ========== 원본 전표가 PURCHASE_RETURN과 연결할 PURCHASE 유형인지 검증하는 메서드 ==========
	private void validateOriginalPurchaseVoucher(Voucher voucher) {
		if (voucher.getType() != VoucherType.PURCHASE) {
			throw new BusinessException(ErrorCode.CONFLICT, "매입 전표만 매입 반품 원본으로 사용할 수 있습니다.");
		}
	}

	// ========== 입금이 취소 가능한 ACTIVE 상태인지 검증하는 메서드 ==========
	private void validateActivePayment(Payment payment) {
		if (payment.getStatus() != PaymentStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 취소된 입금은 다시 취소할 수 없습니다.");
		}
	}

	// ========== 출고별 SALES 전표 중복 생성을 사전에 검증하는 메서드 ==========
	private void validateShipmentVoucherDoesNotExist(Long shipmentId) {
		if (voucherRepository.findByShipmentId(shipmentId).isPresent()) {
			throw new BusinessException(ErrorCode.CONFLICT, "해당 출고의 매출 전표가 이미 생성되었습니다.");
		}
	}

	// ========== 거래처 반품별 SALES_RETURN 전표 중복 생성을 사전에 검증하는 메서드 ==========
	private void validateCustomerReturnVoucherDoesNotExist(Long customerReturnId) {
		if (voucherRepository.findByCustomerReturnId(customerReturnId).isPresent()) {
			throw new BusinessException(ErrorCode.CONFLICT, "해당 거래처 반품의 매출 반품 전표가 이미 생성되었습니다.");
		}
	}

	// ========== 입고별 PURCHASE 전표 중복 생성을 사전에 검증하는 메서드 ==========
	private void validateReceiptVoucherDoesNotExist(Long receiptId) {
		if (voucherRepository.findByReceiptId(receiptId).isPresent()) {
			throw new BusinessException(ErrorCode.CONFLICT, "해당 입고의 매입 전표가 이미 생성되었습니다.");
		}
	}

	// ========== 매입 반품별 PURCHASE_RETURN 전표 중복 생성을 사전에 검증하는 메서드 ==========
	private void validatePurchaseReturnVoucherDoesNotExist(Long purchaseReturnId) {
		if (voucherRepository.findByPurchaseReturnId(purchaseReturnId).isPresent()) {
			throw new BusinessException(ErrorCode.CONFLICT, "해당 매입 반품의 매입 반품 전표가 이미 생성되었습니다.");
		}
	}

	// ========== 정산 처리 사용자 또는 입금 처리 사용자가 존재하는지 검증하는 메서드 ==========
	private void validateProcessedBy(AppUser processedBy) {
		if (processedBy == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "정산 처리 사용자를 확인할 수 없습니다.");
		}
	}

	// ========== 전표·입금·거래처 등의 식별자가 양수인지 검증하는 메서드 ==========
	private void validateIdentifier(Long identifier) {
		if (identifier == null || identifier <= 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "대상 식별자가 올바르지 않습니다.");
		}
	}

	// ========== 선택 입력인 입금 메모의 앞뒤 공백을 제거하고 빈 값은 null로 변환하는 메서드 ==========
	private String normalizeMemo(String memo) {
		return memo == null || memo.isBlank() ? null : memo.trim();
	}

	// ========== 입금 취소 또는 배분 해제의 필수 사유를 검증하고 정규화된 값을 반환하는 메서드 ==========
	private String requireReason(String reason) {
		if (reason == null || reason.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "처리 사유를 입력해 주세요.");
		}

		String normalizedReason = reason.trim();
		if (normalizedReason.length() > 1000) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "처리 사유는 1000자 이하로 입력해 주세요.");
		}
		return normalizedReason;
	}

	// ========== 존재하지 않는 전표에 사용할 공통 404 업무 예외를 생성하는 메서드 ==========
	private BusinessException createVoucherNotFoundException() {
		return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "전표를 찾을 수 없습니다.");
	}

	// ========== 존재하지 않는 입금에 사용할 공통 404 업무 예외를 생성하는 메서드 ==========
	private BusinessException createPaymentNotFoundException() {
		return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "입금 정보를 찾을 수 없습니다.");
	}
}
