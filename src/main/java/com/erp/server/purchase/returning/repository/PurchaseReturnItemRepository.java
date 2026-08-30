package com.erp.server.purchase.returning.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.purchase.returning.domain.PurchaseReturnItem;

// ********** PURCHASE_RETURN_ITEM의 기본 CRUD와 반품 상세·원본 입고 LOT별 완료 반품 누계 조회를 처리하기 위한 Repository interface **********
public interface PurchaseReturnItemRepository extends JpaRepository<PurchaseReturnItem, Long> {

	// ========== 한 매입 반품의 품목·원본 입고 LOT·재고 LOT·품목을 원본 발주 순번과 LOT 식별자 순으로 조회하는 메서드 ==========
	@Query("""
			select purchaseReturnItem
			from PurchaseReturnItem purchaseReturnItem
			join fetch purchaseReturnItem.receiptLot receiptLot
			join fetch receiptLot.receiptItem receiptItem
			join fetch receiptItem.purchaseOrderItem purchaseOrderItem
			join fetch purchaseReturnItem.inventoryLot
			join fetch purchaseReturnItem.item
			where purchaseReturnItem.purchaseReturn.purchaseReturnId = :purchaseReturnId
			order by purchaseOrderItem.lineNo asc, receiptLot.receiptLotId asc
			""")
	List<PurchaseReturnItem> findAllByPurchaseReturnIdWithDetails(
			@Param("purchaseReturnId") Long purchaseReturnId);

	// ========== 선택한 원본 입고 LOT별 COMPLETED 매입 반품 누적 수량을 한 번에 조회하는 메서드 ==========
	// REGISTERED와 CANCELED 반품은 실제 재고에 반영되지 않았으므로 누계에서 제외한다.
	@Query("""
			select purchaseReturnItem.receiptLot.receiptLotId as receiptLotId,
			       sum(purchaseReturnItem.returnQuantity) as returnedQuantity
			from PurchaseReturnItem purchaseReturnItem
			where purchaseReturnItem.receiptLot.receiptLotId in :receiptLotIds
			  and purchaseReturnItem.purchaseReturn.status = com.erp.server.purchase.returning.domain.PurchaseReturnStatus.COMPLETED
			group by purchaseReturnItem.receiptLot.receiptLotId
			""")
	List<CompletedReturnQuantityProjection> sumCompletedQuantitiesByReceiptLotIds(
			@Param("receiptLotIds") Collection<Long> receiptLotIds);

	// 원본 입고 LOT 식별자와 해당 LOT의 완료 반품 누적 수량을 조회 결과로 받는다.
	interface CompletedReturnQuantityProjection {
		Long getReceiptLotId();
		BigDecimal getReturnedQuantity();
	}
}
