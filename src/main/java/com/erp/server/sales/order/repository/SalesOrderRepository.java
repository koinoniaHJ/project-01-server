package com.erp.server.sales.order.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.sales.order.domain.SalesOrder;
import com.erp.server.sales.order.domain.SalesOrderStatus;

import jakarta.persistence.LockModeType;

// ********** SALES_ORDER의 기본 CRUD와 거래처·상태·기간별 목록·상세·비관적 잠금 조회를 처리하기 위한 Repository interface **********
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

	// ========== 거래처·주문 상태·등록 기간 조건을 함께 적용하여 주문 목록을 페이지 조회하는 메서드 ==========
	// 기간은 createdAt 기준 양끝을 포함하고 정렬은 Service에서 등록 일시·주문 식별자 내림차순으로 고정한다.
	@Query(value = """
			select salesOrder
			from SalesOrder salesOrder
			join fetch salesOrder.customer
			where (:customerId is null or salesOrder.customer.customerId = :customerId)
			  and (:status is null or salesOrder.status = :status)
			  and (:startDateTime is null or salesOrder.createdAt >= :startDateTime)
			  and (:endDateTime is null or salesOrder.createdAt < :endDateTime)
			""",
			countQuery = """
			select count(salesOrder)
			from SalesOrder salesOrder
			where (:customerId is null or salesOrder.customer.customerId = :customerId)
			  and (:status is null or salesOrder.status = :status)
			  and (:startDateTime is null or salesOrder.createdAt >= :startDateTime)
			  and (:endDateTime is null or salesOrder.createdAt < :endDateTime)
			""")
	Page<SalesOrder> findAllByFilters(@Param("customerId") Long customerId,
			@Param("status") SalesOrderStatus status, @Param("startDateTime") LocalDateTime startDateTime,
			@Param("endDateTime") LocalDateTime endDateTime, Pageable pageable);

	// ========== salesOrderId로 주문·거래처·처리 사용자를 한 번에 상세 조회하는 메서드 ==========
	// 주문 품목과 연결 출고는 컬렉션 중복 행을 피하기 위해 각각의 Repository에서 별도로 조회한다.
	@Query("""
			select salesOrder
			from SalesOrder salesOrder
			join fetch salesOrder.customer
			join fetch salesOrder.createdBy
			left join fetch salesOrder.registeredBy
			left join fetch salesOrder.canceledBy
			where salesOrder.salesOrderId = :salesOrderId
			""")
	Optional<SalesOrder> findDetailById(@Param("salesOrderId") Long salesOrderId);

	// ========== 수정·삭제·접수·취소 중 같은 주문의 동시 처리를 막기 위해 PESSIMISTIC_WRITE 잠금으로 조회하는 메서드 ==========
	// 잠금 획득 후 허용 상태와 최신 version을 다시 검증한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select salesOrder
			from SalesOrder salesOrder
			where salesOrder.salesOrderId = :salesOrderId
			""")
	Optional<SalesOrder> findByIdForUpdate(@Param("salesOrderId") Long salesOrderId);
}
