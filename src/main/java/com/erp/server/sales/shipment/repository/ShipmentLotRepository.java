package com.erp.server.sales.shipment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.sales.shipment.domain.ShipmentLot;

import jakarta.persistence.LockModeType;

// ********** SHIPMENT_LOT의 기본 CRUD와 PACKED 주문 취소 시 활성 예약 LOT 잠금 조회를 처리하기 위한 Repository interface **********
public interface ShipmentLotRepository extends JpaRepository<ShipmentLot, Long> {

	// ========== 출고 상세·납품서에 표시할 현재 포장 LOT를 주문 품목·LOT 순서로 조회하는 메서드 ==========
	@Query("""
			select shipmentLot
			from ShipmentLot shipmentLot
			join fetch shipmentLot.salesOrderItem salesOrderItem
			join fetch salesOrderItem.item
			join fetch shipmentLot.inventoryLot inventoryLot
			join fetch inventoryLot.warehouse
			join fetch inventoryLot.supplier
			where shipmentLot.shipment.shipmentId = :shipmentId
			order by salesOrderItem.lineNo asc, inventoryLot.expiryDate asc,
			         inventoryLot.createdAt asc, inventoryLot.inventoryLotId asc
			""")
	List<ShipmentLot> findAllByShipmentIdWithDetails(@Param("shipmentId") Long shipmentId);

	// ========== PENDING 출고의 기존 포장안을 전체 교체하기 전에 SHIPMENT_LOT 행을 삭제하는 메서드 ==========
	@Modifying(clearAutomatically = false, flushAutomatically = true)
	@Query("""
			delete from ShipmentLot shipmentLot
			where shipmentLot.shipment.shipmentId = :shipmentId
			""")
	void deleteAllByShipmentId(@Param("shipmentId") Long shipmentId);

	// ========== 포장 확정·출고 완료 중 현재 포장 LOT를 재고 LOT 식별자 고정 순서로 잠금 조회하는 메서드 ==========
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select shipmentLot
			from ShipmentLot shipmentLot
			join fetch shipmentLot.salesOrderItem salesOrderItem
			join fetch salesOrderItem.item
			join fetch shipmentLot.inventoryLot inventoryLot
			where shipmentLot.shipment.shipmentId = :shipmentId
			order by inventoryLot.inventoryLotId asc
			""")
	List<ShipmentLot> findAllByShipmentIdForUpdate(@Param("shipmentId") Long shipmentId);

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
