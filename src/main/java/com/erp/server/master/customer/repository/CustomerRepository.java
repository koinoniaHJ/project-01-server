package com.erp.server.master.customer.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.customer.domain.Customer;
import com.erp.server.master.customer.domain.CustomerTradeStatus;

import jakarta.persistence.LockModeType;

// ********** CUSTOMER의 기본 CRUD와 키워드·사용 상태·거래 상태 조건을 적용한 거래처 목록 조회를 처리하기 위한 Repository interface **********
public interface CustomerRepository extends JpaRepository<Customer, Long> {

	// ========== 거래처 코드·거래처명·대표 연락처와 사용 상태·거래 상태를 조건으로 거래처 목록을 페이지 조회하는 메서드
	// ==========
	// keyword는 앞뒤 공백을 제거한 검색어이며, phoneKeyword는 숫자만 남긴 검색어 또는 null을 전달한다.
	@Query("""
			select c
			from Customer c
			where (
			    :keyword is null
			    or lower(c.customerCode) like lower(concat('%', :keyword, '%'))
			    or lower(c.customerName) like lower(concat('%', :keyword, '%'))
			    or (
				    :phoneKeyword is not null
				    and cast(function('regexp_replace', c.phone, '[^0-9]', '') as String)
				        like concat('%', :phoneKeyword, '%')
				)
			)
			  and (:status is null or c.status = :status)
			  and (:tradeStatus is null or c.tradeStatus = :tradeStatus)
			""")

	Page<Customer> findAllByFilters(@Param("keyword") String keyword, @Param("phoneKeyword") String phoneKeyword,
			@Param("status") MasterStatus status, @Param("tradeStatus") CustomerTradeStatus tradeStatus,
			Pageable pageable);

	// ========== 거래처 상태 변경과 최신 상태 검증 중 동시 처리를 막기 위해 PESSIMISTIC_WRITE 비관적 잠금으로 조회하는 메서드 ==========
	// 먼저 잠금을 얻은 트랜잭션이 끝날 때까지 같은 거래처를 잠금 조회하는 다른 요청은 대기한다.
	// ACTIVE/INACTIVE 변경, NORMAL/HOLD 변경, 이후 주문 접수처럼 최신 거래처 상태 검증이 필요한 업무에서 사용한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select c
			from Customer c
			where c.customerId = :customerId
			""")
	Optional<Customer> findByIdForUpdate(@Param("customerId") Long customerId);
	
	// ========== Oracle의 거래처 업무 코드 Sequence를 이용하여 CUS + 6자리 형식의 다음 거래처 코드를 생성하는 메서드
	// ==========
	// 실제 PK용 SEQ_CUSTOMER와 업무 코드용 SEQ_CUSTOMER_CODE를 분리
	@Query(value = """
			SELECT 'CUS' || LPAD(SEQ_CUSTOMER_CODE.NEXTVAL, 6, '0')
			FROM DUAL
			""", nativeQuery = true)
	String generateCustomerCode();

	// ========== 거래처 사용 중지를 차단하는 진행 주문·반품·미결 전표·미배분 입금의 전체 건수를 조회하는 메서드 ==========
	/**
	 * DRAFT, REGISTERED 주문
	 * REGISTERED 거래처 반품
	 * UNPAID, PARTIALLY_PAID 매출 전표
	 * 미배분 금액이 남은 ACTIVE 입금
	 */
	@Query(value = """
			SELECT
			    (
			        SELECT COUNT(*)
			        FROM SALES_ORDER sales_order
			        WHERE sales_order.customer_id = :customerId
			          AND sales_order.status IN ('DRAFT', 'REGISTERED')
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM CUSTOMER_RETURN customer_return
			        JOIN SHIPMENT shipment
			          ON shipment.shipment_id = customer_return.shipment_id
			        JOIN SALES_ORDER sales_order
			          ON sales_order.sales_order_id = shipment.sales_order_id
			        WHERE sales_order.customer_id = :customerId
			          AND customer_return.status = 'REGISTERED'
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM VOUCHER voucher
			        WHERE voucher.customer_id = :customerId
			          AND voucher.type = 'SALES'
			          AND voucher.settlement_status IN ('UNPAID', 'PARTIALLY_PAID')
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM PAYMENT payment
			        WHERE payment.customer_id = :customerId
			          AND payment.status = 'ACTIVE'
			          AND payment.unallocated_amount > 0
			    )
			FROM DUAL
			""", nativeQuery = true)
	long countOngoingBusinessReferences(@Param("customerId") Long customerId);
}