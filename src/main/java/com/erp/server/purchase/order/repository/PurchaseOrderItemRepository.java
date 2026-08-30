package com.erp.server.purchase.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.purchase.order.domain.PurchaseOrderItem;

// ********** PURCHASE_ORDER_ITEM의 기본 CRUD와 발주별 품목 상세 조회를 처리하기 위한 Repository interface **********
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

	// ========== 한 발주의 품목과 ITEM 기본정보를 발주서 표시 순번 오름차순으로 조회하는 메서드 ==========
	@Query("""
			select poi
			from PurchaseOrderItem poi
			join fetch poi.item
			where poi.purchaseOrder.purchaseOrderId = :purchaseOrderId
			order by poi.lineNo asc
			""")
	List<PurchaseOrderItem> findAllByPurchaseOrderId(@Param("purchaseOrderId") Long purchaseOrderId);

	// ========== DRAFT 발주 수정 시 기존 품목을 새 요청 품목으로 교체하기 위해 자식 행 전체를 삭제하는 메서드 ==========
	// 삭제 Query를 먼저 실행한 후 같은 트랜잭션에서 새 품목을 저장하여 품목·표시 순번 UNIQUE 충돌을 방지한다.
	@Modifying
	@Query("""
			delete from PurchaseOrderItem poi
			where poi.purchaseOrder.purchaseOrderId = :purchaseOrderId
			""")
	int deleteAllByPurchaseOrderId(@Param("purchaseOrderId") Long purchaseOrderId);
}
