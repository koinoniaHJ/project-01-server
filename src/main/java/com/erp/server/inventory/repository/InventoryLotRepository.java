package com.erp.server.inventory.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.inventory.domain.InventoryLot;

import jakarta.persistence.LockModeType;

// ********** INVENTORY_LOT의 기본 CRUD와 일반 조회·비관적 잠금·실사/조정 제한 조회를 처리하기 위한 Repository interface **********
public interface InventoryLotRepository extends JpaRepository<InventoryLot, Long> {

	// ========== inventoryLotId로 재고 LOT와 창고·품목·공급업체 정보를 함께 일반 조회하는 메서드 ==========
	// 수량을 변경하지 않는 재고 현황·LOT 상세 조회에서 지연 로딩 추가 조회를 줄이기 위해 사용한다.
	@EntityGraph(attributePaths = { "warehouse", "item", "supplier", "createdBy" })
	@Query("""
			select inventoryLot
			from InventoryLot inventoryLot
			where inventoryLot.inventoryLotId = :inventoryLotId
			""")
	Optional<InventoryLot> findByIdWithDetails(@Param("inventoryLotId") Long inventoryLotId);

	// ========== 창고·품목·공급업체·LOT 번호가 모두 같은 기존 재고 LOT를 일반 조회하는 메서드 ==========
	// 입고 완료에서 동일 업무 키의 LOT를 재사용할 때 기존 사용기한 일치 여부를 확인하기 위해 사용한다.
	@Query("""
			select inventoryLot
			from InventoryLot inventoryLot
			where inventoryLot.warehouse.warehouseId = :warehouseId
			  and inventoryLot.item.itemId = :itemId
			  and inventoryLot.supplier.supplierId = :supplierId
			  and inventoryLot.lotNumber = :lotNumber
			""")
	Optional<InventoryLot> findByBusinessKey(@Param("warehouseId") Long warehouseId,
			@Param("itemId") Long itemId, @Param("supplierId") Long supplierId,
			@Param("lotNumber") String lotNumber);

	// ========== inventoryLotId로 재고 LOT를 PESSIMISTIC_WRITE 비관적 잠금 조회하는 메서드 ==========
	// 먼저 잠금을 얻은 트랜잭션이 끝날 때까지 같은 LOT의 수량·상태를 변경하려는 다른 요청은 대기한다.
	// 잠금 획득 후 현재·예약 수량, LOT 상태, 사용기한과 실사/조정 제한을 최신 값으로 다시 검증한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select inventoryLot
			from InventoryLot inventoryLot
			where inventoryLot.inventoryLotId = :inventoryLotId
			""")
	Optional<InventoryLot> findByIdForUpdate(@Param("inventoryLotId") Long inventoryLotId);

	// ========== 여러 재고 LOT를 inventoryLotId 오름차순의 고정 순서로 PESSIMISTIC_WRITE 잠금 조회하는 메서드 ==========
	// 출고·반품·실사처럼 여러 LOT를 한 트랜잭션에서 처리할 때 요청 순서와 관계없이 같은 잠금 순서를 사용하여 교착 상태 가능성을 줄인다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select inventoryLot
			from InventoryLot inventoryLot
			where inventoryLot.inventoryLotId in :inventoryLotIds
			order by inventoryLot.inventoryLotId asc
			""")
	List<InventoryLot> findAllByIdForUpdate(@Param("inventoryLotIds") Collection<Long> inventoryLotIds);

	// ========== 해제되지 않은 STOCKTAKE_ITEM이 대상 재고 LOT를 제한하고 있는지 건수로 확인하는 메서드 ==========
	// restricted_at이 있고 released_at이 없는 동안은 실사 또는 후속 재고 조정이 끝나지 않은 상태이다.
	@Query(value = """
			SELECT COUNT(*)
			FROM STOCKTAKE_ITEM stocktake_item
			WHERE stocktake_item.inventory_lot_id = :inventoryLotId
			  AND stocktake_item.restricted_at IS NOT NULL
			  AND stocktake_item.released_at IS NULL
			""", nativeQuery = true)
	long countUnreleasedRestrictions(@Param("inventoryLotId") Long inventoryLotId);

	// ========== Oracle의 내부 LOT 업무 코드 Sequence를 이용하여 LOT + 6자리 형식의 다음 LOT 번호를 생성하는 메서드 ==========
	// 실제 PK용 SEQ_INVENTORY_LOT와 업무 번호용 SEQ_INTERNAL_LOT_CODE를 분리한다.
	@Query(value = """
			SELECT 'LOT' || LPAD(SEQ_INTERNAL_LOT_CODE.NEXTVAL, 6, '0')
			FROM DUAL
			""", nativeQuery = true)
	String generateInternalLotNumber();
}
