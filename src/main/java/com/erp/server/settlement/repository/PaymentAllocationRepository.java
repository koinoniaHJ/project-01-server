package com.erp.server.settlement.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.settlement.domain.PaymentAllocation;

import jakarta.persistence.LockModeType;

// ********** PAYMENT_ALLOCATION의 배분 이력 저장과 전표·입금별 유효 배분 잠금 및 합계 조회를 처리하기 위한 Repository interface **********
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {

	// ========== SALES 전표에 연결된 활성 배분을 최근 배분 일시·큰 식별자 순으로 PESSIMISTIC_WRITE 잠금 조회하는 메서드 ==========
	// 매출 반품 초과 배분은 최근 배분부터 해제하여 먼저 이루어진 입금 정산을 최대한 유지한다.
	// 배분 일시가 같으면 paymentAllocationId가 큰 행부터 해제하여 서버의 처리 순서를 항상 동일하게 유지한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select allocation
			from PaymentAllocation allocation
			where allocation.voucher.voucherId = :voucherId
			  and allocation.releasedAt is null
			order by allocation.allocatedAt desc, allocation.paymentAllocationId desc
			""")
	List<PaymentAllocation> findActiveByVoucherIdForUpdate(@Param("voucherId") Long voucherId);

	// ========== 입금에 연결된 해제되지 않은 활성 배분을 식별자 순으로 PESSIMISTIC_WRITE 잠금 조회하는 메서드 ==========
	// 입금 취소 시 해당 입금의 배분만 해제하고 다른 활성 입금 배분은 유지한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select allocation
			from PaymentAllocation allocation
			where allocation.payment.paymentId = :paymentId
			  and allocation.releasedAt is null
			order by allocation.paymentAllocationId asc
			""")
	List<PaymentAllocation> findActiveByPaymentIdForUpdate(@Param("paymentId") Long paymentId);

	// ========== SALES 전표에 연결된 해제되지 않은 유효 입금 배분액 합계를 조회하는 메서드 ==========
	@Query("""
			select coalesce(sum(allocation.allocatedAmount), 0)
			from PaymentAllocation allocation
			where allocation.voucher.voucherId = :voucherId
			  and allocation.releasedAt is null
			""")
	BigDecimal sumActiveAllocatedAmountByVoucherId(@Param("voucherId") Long voucherId);

	// ========== ACTIVE 입금에 연결된 해제되지 않은 유효 배분액 합계를 조회하는 메서드 ==========
	@Query("""
			select coalesce(sum(allocation.allocatedAmount), 0)
			from PaymentAllocation allocation
			where allocation.payment.paymentId = :paymentId
			  and allocation.releasedAt is null
			""")
	BigDecimal sumActiveAllocatedAmountByPaymentId(@Param("paymentId") Long paymentId);
}
