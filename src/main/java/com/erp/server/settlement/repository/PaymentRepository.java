package com.erp.server.settlement.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.settlement.domain.Payment;

import jakarta.persistence.LockModeType;

// ********** PAYMENT의 기본 CRUD와 입금 취소·미배분 입금 자동 배분용 비관적 잠금 조회를 처리하기 위한 Repository interface **********
public interface PaymentRepository extends JpaRepository<Payment, Long> {

	// ========== paymentId로 입금을 PESSIMISTIC_WRITE 비관적 잠금 조회하는 메서드 ==========
	// 입금 취소와 배분 변경 전에 최신 상태·미배분 금액·version을 확인하기 위해 사용한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select payment
			from Payment payment
			where payment.paymentId = :paymentId
			""")
	Optional<Payment> findByIdForUpdate(@Param("paymentId") Long paymentId);

	// ========== 여러 입금을 paymentId 오름차순의 고정 순서로 PESSIMISTIC_WRITE 잠금 조회하는 메서드 ==========
	// 매출 반품 초과 배분이 여러 입금에 걸쳐 있더라도 같은 잠금 순서를 사용하여 교착 상태 가능성을 줄인다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select payment
			from Payment payment
			where payment.paymentId in :paymentIds
			order by payment.paymentId asc
			""")
	List<Payment> findAllByIdForUpdate(@Param("paymentIds") Collection<Long> paymentIds);

	// ========== 거래처의 ACTIVE 미배분 입금을 입금일·식별자가 오래된 순서로 PESSIMISTIC_WRITE 잠금 조회하는 메서드 ==========
	// CUSTOMER와 VOUCHER 잠금을 먼저 획득한 후 이 조회를 실행하여 공통 잠금 순서를 유지한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select payment
			from Payment payment
			where payment.customer.customerId = :customerId
			  and payment.status = com.erp.server.settlement.domain.PaymentStatus.ACTIVE
			  and payment.unallocatedAmount > 0
			order by payment.paymentDate asc, payment.paymentId asc
			""")
	List<Payment> findUnallocatedPaymentsForUpdate(@Param("customerId") Long customerId);
}
