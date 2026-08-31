package com.erp.server.sales.shipment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.sales.shipment.domain.Shipment;

import jakarta.persistence.LockModeType;

// ********** SHIPMENT의 기본 CRUD와 주문별 단일 출고 조회·비관적 잠금 조회를 처리하기 위한 Repository interface **********
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

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
