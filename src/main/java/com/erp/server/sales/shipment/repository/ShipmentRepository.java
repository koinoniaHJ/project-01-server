package com.erp.server.sales.shipment.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.sales.shipment.domain.Shipment;
import com.erp.server.sales.shipment.domain.ShipmentStatus;

import jakarta.persistence.LockModeType;

// ********** SHIPMENT의 기본 CRUD와 주문별 단일 출고 조회·비관적 잠금 조회를 처리하기 위한 Repository interface **********
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

	// ========== 거래처·출고 상태·주문 등록 기간 조건으로 출고 목록을 페이지 조회하는 메서드 ==========
	@Query(value = """
			select shipment
			from Shipment shipment
			join fetch shipment.salesOrder salesOrder
			join fetch salesOrder.customer
			left join fetch shipment.warehouse
			where (:customerId is null or salesOrder.customer.customerId = :customerId)
			  and (:status is null or shipment.status = :status)
			  and (:startDateTime is null or salesOrder.registeredAt >= :startDateTime)
			  and (:endDateTime is null or salesOrder.registeredAt < :endDateTime)
			""",
			countQuery = """
			select count(shipment)
			from Shipment shipment
			where (:customerId is null or shipment.salesOrder.customer.customerId = :customerId)
			  and (:status is null or shipment.status = :status)
			  and (:startDateTime is null or shipment.salesOrder.registeredAt >= :startDateTime)
			  and (:endDateTime is null or shipment.salesOrder.registeredAt < :endDateTime)
			""")
	Page<Shipment> findAllByFilters(@Param("customerId") Long customerId,
			@Param("status") ShipmentStatus status, @Param("startDateTime") LocalDateTime startDateTime,
			@Param("endDateTime") LocalDateTime endDateTime, Pageable pageable);

	// ========== shipmentId로 주문·거래처·창고·처리 사용자를 출고 상세 조회하는 메서드 ==========
	@Query("""
			select shipment
			from Shipment shipment
			join fetch shipment.salesOrder salesOrder
			join fetch salesOrder.customer
			left join fetch shipment.warehouse
			left join fetch shipment.packedBy
			left join fetch shipment.completedBy
			left join fetch shipment.canceledBy
			where shipment.shipmentId = :shipmentId
			""")
	Optional<Shipment> findDetailById(@Param("shipmentId") Long shipmentId);

	// ========== 포장·완료 중 출고 상태와 version이 바뀌지 않도록 PESSIMISTIC_WRITE 잠금 조회하는 메서드 ==========
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select shipment
			from Shipment shipment
			where shipment.shipmentId = :shipmentId
			""")
	Optional<Shipment> findByIdForUpdate(@Param("shipmentId") Long shipmentId);

	// ========== 주문 상세 화면에서 연결 출고와 선택 창고를 함께 조회하는 메서드 ==========
	@Query("""
			select shipment
			from Shipment shipment
			left join fetch shipment.warehouse
			where shipment.salesOrder.salesOrderId = :salesOrderId
			""")
	Optional<Shipment> findBySalesOrderId(@Param("salesOrderId") Long salesOrderId);

	// ========== 주문 취소 중 연결 출고의 상태가 동시에 바뀌지 않도록 PESSIMISTIC_WRITE 잠금 조회하는 메서드 ==========
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select shipment
			from Shipment shipment
			where shipment.salesOrder.salesOrderId = :salesOrderId
			""")
	Optional<Shipment> findBySalesOrderIdForUpdate(@Param("salesOrderId") Long salesOrderId);
}
