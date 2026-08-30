package com.erp.server.settlement.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.settlement.domain.Voucher;

import jakarta.persistence.LockModeType;

// ********** VOUCHER의 기본 CRUD와 원본 업무 조회·매출 정산용 잠금·반품 및 미수금 합계 조회를 처리하기 위한 Repository interface **********
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

	// ========== voucherId로 전표와 거래처·공급업체·원본 전표·품목 스냅샷을 함께 일반 조회하는 메서드 ==========
	@EntityGraph(attributePaths = { "customer", "supplier", "originalVoucher", "items", "items.item" })
	@Query("""
			select distinct voucher
			from Voucher voucher
			where voucher.voucherId = :voucherId
			""")
	Optional<Voucher> findByIdWithDetails(@Param("voucherId") Long voucherId);

	// ========== voucherId로 전표를 PESSIMISTIC_WRITE 비관적 잠금 조회하는 메서드 ==========
	// 매출 반품·입금 배분·배분 해제에서 최신 정산 대상 금액과 미수 잔액을 다시 검증하기 위해 사용한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select voucher
			from Voucher voucher
			where voucher.voucherId = :voucherId
			""")
	Optional<Voucher> findByIdForUpdate(@Param("voucherId") Long voucherId);

	// ========== 거래처의 모든 SALES 전표를 전표 일자·식별자 오름차순으로 PESSIMISTIC_WRITE 잠금 조회하는 메서드 ==========
	// 거래처 잠금 다음에 전표를 고정 순서로 잠가 정산 재계산과 입금 배분의 교착 상태 가능성을 줄인다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select voucher
			from Voucher voucher
			where voucher.customer.customerId = :customerId
			  and voucher.type = com.erp.server.settlement.domain.VoucherType.SALES
			order by voucher.voucherDate asc, voucher.voucherId asc
			""")
	List<Voucher> findSalesVouchersForUpdate(@Param("customerId") Long customerId);

	// ========== 거래처의 미결 SALES 전표를 오래된 전표 일자·식별자 순으로 PESSIMISTIC_WRITE 잠금 조회하는 메서드 ==========
	// UNPAID·PARTIALLY_PAID 전표만 자동 배분 대상으로 조회하고 PAID 전표는 제외한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select voucher
			from Voucher voucher
			where voucher.customer.customerId = :customerId
			  and voucher.type = com.erp.server.settlement.domain.VoucherType.SALES
			  and voucher.settlementStatus in (
			      com.erp.server.settlement.domain.SettlementStatus.UNPAID,
			      com.erp.server.settlement.domain.SettlementStatus.PARTIALLY_PAID
			  )
			order by voucher.voucherDate asc, voucher.voucherId asc
			""")
	List<Voucher> findOutstandingSalesVouchersForUpdate(@Param("customerId") Long customerId);

	// ========== 원본 SALES 전표에 연결된 SALES_RETURN 전표 금액 합계를 조회하는 메서드 ==========
	// 매출 반품 품목 금액은 음수이므로 원본 매출 금액에 합산하면 최신 정산 대상 금액이 된다.
	@Query("""
			select coalesce(sum(voucher.totalAmount), 0)
			from Voucher voucher
			where voucher.originalVoucher.voucherId = :originalVoucherId
			  and voucher.type = com.erp.server.settlement.domain.VoucherType.SALES_RETURN
			""")
	BigDecimal sumSalesReturnAmount(@Param("originalVoucherId") Long originalVoucherId);

	// ========== 거래처의 SALES 전표별 미수 잔액을 모두 합산하여 총미수금을 조회하는 메서드 ==========
	@Query("""
			select coalesce(sum(voucher.outstandingAmount), 0)
			from Voucher voucher
			where voucher.customer.customerId = :customerId
			  and voucher.type = com.erp.server.settlement.domain.VoucherType.SALES
			""")
	BigDecimal sumOutstandingAmountByCustomerId(@Param("customerId") Long customerId);

	// ========== 원본 출고에서 이미 생성된 SALES 전표를 조회하는 메서드 ==========
	Optional<Voucher> findByShipmentId(Long shipmentId);

	// ========== 원본 거래처 반품에서 이미 생성된 SALES_RETURN 전표를 조회하는 메서드 ==========
	Optional<Voucher> findByCustomerReturnId(Long customerReturnId);

	// ========== 원본 입고에서 이미 생성된 PURCHASE 전표를 조회하는 메서드 ==========
	Optional<Voucher> findByReceiptId(Long receiptId);

	// ========== 원본 매입 반품에서 이미 생성된 PURCHASE_RETURN 전표를 조회하는 메서드 ==========
	Optional<Voucher> findByPurchaseReturnId(Long purchaseReturnId);
}
