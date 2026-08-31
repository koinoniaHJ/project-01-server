package com.erp.server.sales.shipment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.sales.shipment.domain.ShipmentLot;

import jakarta.persistence.LockModeType;

// ********** SHIPMENT_LOT의 기본 CRUD와 PACKED 주문 취소 시 활성 예약 LOT 잠금 조회를 처리하기 위한 Repository interface **********
public interface ShipmentLotRepository extends JpaRepository<ShipmentLot, Long> {

	// ========== 연결 출고의 예약 반영 LOT를 재고 LOT 식별자 순서로 PESSIMISTIC_WRITE 잠금 조회하는 메서드 ==========
	// 이후 InventoryLot도 같은 식별자 순서로 잠가 동시 포장·취소 사이의 교착 가능성을 줄인다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select shipmentLot
			from ShipmentLot shipmentLot
			join fetch shipmentLot.inventoryLot
			where shipmentLot.shipment.shipmentId = :shipmentId
			  and shipmentLot.reservedYn = 'Y'
			order by shipmentLot.inventoryLot.inventoryLotId asc
			""")
	List<ShipmentLot> findReservedByShipmentIdForUpdate(@Param("shipmentId") Long shipmentId);
}
