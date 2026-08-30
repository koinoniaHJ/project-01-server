package com.erp.server.master.warehouse.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.server.master.warehouse.domain.WarehouseItem;

// ********** WAREHOUSE_ITEM의 기본 CRUD와 창고·품목 조합별 안전재고 기준 조회를 처리하기 위한 Repository interface **********
public interface WarehouseItemRepository extends JpaRepository<WarehouseItem, Long> {

	// ========== 창고와 품목 식별자로 이미 등록된 안전재고 기준을 조회하는 메서드 ==========
	// 조회 결과가 없으면 version이 없는 최초 등록 요청으로 처리하고, 존재하면 최신 version을 검증하여 변경한다.
	Optional<WarehouseItem> findByWarehouseWarehouseIdAndItemItemId(Long warehouseId, Long itemId);
}
