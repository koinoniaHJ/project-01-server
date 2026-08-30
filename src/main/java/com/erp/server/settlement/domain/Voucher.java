package com.erp.server.settlement.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.erp.server.master.customer.domain.Customer;
import com.erp.server.master.supplier.domain.Supplier;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ********** Oracle Database의 VOUCHER 테이블과 매출·매출 반품·매입·매입 반품 전표 및 매출 정산 상태를 Java 객체로 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "VOUCHER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Voucher {

	// Oracle의 SEQ_VOUCHER에서 다음 값을 받아 PK로 사용하는 전표 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "voucherSequenceGenerator")
	@SequenceGenerator(name = "voucherSequenceGenerator", sequenceName = "SEQ_VOUCHER", allocationSize = 1)
	@Column(name = "voucher_id", nullable = false)
	private Long voucherId;

	// 전표 발생 업무를 SALES, SALES_RETURN, PURCHASE 또는 PURCHASE_RETURN 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 30)
	private VoucherType type;

	// 원본 출고·반품·입고 업무가 완료되어 전표에 반영된 업무 일자를 저장한다.
	@Column(name = "voucher_date", nullable = false)
	private LocalDate voucherDate;

	// SALES 또는 SALES_RETURN 전표의 정산 대상 CUSTOMER 거래처를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id")
	private Customer customer;

	// PURCHASE 또는 PURCHASE_RETURN 전표의 거래 대상 SUPPLIER 공급업체를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id")
	private Supplier supplier;

	// SALES_RETURN 또는 PURCHASE_RETURN 전표가 금액을 차감할 원본 SALES 또는 PURCHASE 전표를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "original_voucher_id")
	private Voucher originalVoucher;

	// SALES 전표를 발생시킨 SHIPMENT 식별자를 저장하며 다른 전표 유형에서는 null이다.
	// 출고 Entity 구현 전에도 정산 코어가 독립적으로 컴파일되도록 식별자만 보관하고 DB FK로 무결성을 보장한다.
	@Column(name = "shipment_id")
	private Long shipmentId;

	// SALES_RETURN 전표를 발생시킨 CUSTOMER_RETURN 식별자를 저장하며 다른 전표 유형에서는 null이다.
	// 거래처 반품 Entity 구현 전에도 정산 코어가 독립적으로 컴파일되도록 식별자만 보관하고 DB FK로 무결성을 보장한다.
	@Column(name = "customer_return_id")
	private Long customerReturnId;

	// PURCHASE 전표를 발생시킨 RECEIPT 식별자를 저장하며 다른 전표 유형에서는 null이다.
	// 입고 Entity 구현 전에도 정산 코어가 독립적으로 컴파일되도록 식별자만 보관하고 DB FK로 무결성을 보장한다.
	@Column(name = "receipt_id")
	private Long receiptId;

	// PURCHASE_RETURN 전표를 발생시킨 PURCHASE_RETURN 식별자를 저장하며 다른 전표 유형에서는 null이다.
	// 매입 반품 Entity 구현 전에도 정산 코어가 독립적으로 컴파일되도록 식별자만 보관하고 DB FK로 무결성을 보장한다.
	@Column(name = "purchase_return_id")
	private Long purchaseReturnId;

	// 전표 품목 금액 합계를 저장하며 매출·매입은 0 이상, 반품은 0 이하로 저장한다.
	@Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	// SALES 전표 금액에 연결된 SALES_RETURN 전표 금액을 합산한 실제 정산 대상 금액을 저장한다.
	// 매입 계열과 매출 반품 전표에서는 정산하지 않으므로 null로 유지한다.
	@Column(name = "settlement_target_amount", precision = 19, scale = 2)
	private BigDecimal settlementTargetAmount;

	// SALES 전표에 연결된 해제되지 않은 PAYMENT_ALLOCATION의 유효 배분 금액 합계를 저장한다.
	@Column(name = "allocated_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal allocatedAmount = BigDecimal.ZERO;

	// SALES 전표의 정산 대상 금액에서 유효 입금 배분액을 차감한 미수 잔액을 저장한다.
	// 매입 계열과 매출 반품 전표에서는 null로 유지한다.
	@Column(name = "outstanding_amount", precision = 19, scale = 2)
	private BigDecimal outstandingAmount;

	// SALES 전표의 계산 결과를 UNPAID, PARTIALLY_PAID 또는 PAID 문자열로 저장한다.
	// 매입 계열과 매출 반품 전표에서는 null로 유지한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "settlement_status", length = 30)
	private SettlementStatus settlementStatus;

	// 원본 완료 업무에서 전표가 자동 생성된 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 조회 당시 값과 DB 값을 비교하여 정산 금액과 입금 배분의 동시 처리 충돌을 확인한다.
	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	// 전표에 포함된 VOUCHER_ITEM 품목 스냅샷을 전표와 함께 저장한다.
	@OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL)
	private List<VoucherItem> items = new ArrayList<>();

	// ========== 신규 전표가 저장되기 전에 생성 일시와 배분 금액 기본값을 설정하는 메서드 ==========
	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		allocatedAmount = allocatedAmount == null ? BigDecimal.ZERO : allocatedAmount;
	}

	// ========== 출고 완료 결과와 거래처를 연결한 SALES 전표를 생성하는 정적 팩토리 메서드 ==========
	public static Voucher createSales(Customer customer, LocalDate voucherDate, Long shipmentId) {
		Voucher voucher = createBase(VoucherType.SALES, voucherDate);
		voucher.customer = customer;
		voucher.shipmentId = shipmentId;
		return voucher;
	}

	// ========== 거래처 반품 완료 결과와 원본 SALES 전표를 연결한 SALES_RETURN 전표를 생성하는 정적 팩토리 메서드 ==========
	public static Voucher createSalesReturn(Customer customer, Voucher originalVoucher, LocalDate voucherDate,
			Long customerReturnId) {
		Voucher voucher = createBase(VoucherType.SALES_RETURN, voucherDate);
		voucher.customer = customer;
		voucher.originalVoucher = originalVoucher;
		voucher.customerReturnId = customerReturnId;
		return voucher;
	}

	// ========== 입고 검수 완료 결과와 공급업체를 연결한 PURCHASE 전표를 생성하는 정적 팩토리 메서드 ==========
	public static Voucher createPurchase(Supplier supplier, LocalDate voucherDate, Long receiptId) {
		Voucher voucher = createBase(VoucherType.PURCHASE, voucherDate);
		voucher.supplier = supplier;
		voucher.receiptId = receiptId;
		return voucher;
	}

	// ========== 매입 반품 완료 결과와 원본 PURCHASE 전표를 연결한 PURCHASE_RETURN 전표를 생성하는 정적 팩토리 메서드 ==========
	public static Voucher createPurchaseReturn(Supplier supplier, Voucher originalVoucher, LocalDate voucherDate,
			Long purchaseReturnId) {
		Voucher voucher = createBase(VoucherType.PURCHASE_RETURN, voucherDate);
		voucher.supplier = supplier;
		voucher.originalVoucher = originalVoucher;
		voucher.purchaseReturnId = purchaseReturnId;
		return voucher;
	}

	// ========== 네 전표 유형이 공통으로 사용하는 전표 일자와 초기 금액을 설정하는 메서드 ==========
	private static Voucher createBase(VoucherType type, LocalDate voucherDate) {
		Voucher voucher = new Voucher();
		voucher.type = type;
		voucher.voucherDate = voucherDate;
		voucher.totalAmount = BigDecimal.ZERO;
		voucher.allocatedAmount = BigDecimal.ZERO;
		return voucher;
	}

	// ========== 전표 품목 스냅샷을 추가하고 최신 품목 금액 합계로 전표 총액을 다시 계산하는 메서드 ==========
	public void addItem(VoucherItem item) {
		items.add(item);
		totalAmount = items.stream().map(VoucherItem::getLineAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	// ========== 신규 SALES 전표의 정산 대상 금액·미수 잔액과 최초 정산 상태를 품목 합계로 초기화하는 메서드 ==========
	public void initializeSalesSettlement() {
		updateSettlement(totalAmount, BigDecimal.ZERO);
	}

	// ========== 연결 매출 반품과 유효 입금 배분 합계를 반영하여 SALES 전표 정산 금액과 상태를 갱신하는 메서드 ==========
	public void updateSettlement(BigDecimal settlementTargetAmount, BigDecimal allocatedAmount) {
		this.settlementTargetAmount = settlementTargetAmount;
		this.allocatedAmount = allocatedAmount;
		this.outstandingAmount = settlementTargetAmount.subtract(allocatedAmount);

		if (outstandingAmount.signum() == 0) {
			settlementStatus = SettlementStatus.PAID;
		} else if (allocatedAmount.signum() == 0) {
			settlementStatus = SettlementStatus.UNPAID;
		} else {
			settlementStatus = SettlementStatus.PARTIALLY_PAID;
		}
	}

	// ========== 신규 유효 입금 배분액을 SALES 전표에 반영하고 미수 잔액과 정산 상태를 즉시 갱신하는 메서드 ==========
	public void applyAllocation(BigDecimal amount) {
		updateSettlement(settlementTargetAmount, allocatedAmount.add(amount));
	}

	// ========== 입금 배분 해제액을 SALES 전표에서 차감하고 미수 잔액과 정산 상태를 즉시 갱신하는 메서드 ==========
	public void releaseAllocation(BigDecimal amount) {
		updateSettlement(settlementTargetAmount, allocatedAmount.subtract(amount));
	}

	// ========== 입금 자동 배분 대상인 SALES 전표인지 확인하는 메서드 ==========
	public boolean isSalesVoucher() {
		return type == VoucherType.SALES;
	}
}
