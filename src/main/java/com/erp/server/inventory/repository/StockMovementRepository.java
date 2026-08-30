package com.erp.server.inventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.server.inventory.domain.StockMovement;

// ********** STOCK_MOVEMENT의 재고 변동 이력 저장과 LOT별 최근 변동 순서 조회를 처리하기 위한 Repository interface **********
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

	// ========== inventoryLotId에 해당하는 재고 변동 이력을 처리 일시와 식별자가 큰 최근 순서로 페이지 조회하는 메서드 ==========
	Page<StockMovement> findByInventoryLotInventoryLotIdOrderByProcessedAtDescStockMovementIdDesc(
			Long inventoryLotId, Pageable pageable);
}
