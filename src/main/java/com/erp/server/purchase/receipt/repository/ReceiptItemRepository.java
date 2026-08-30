package com.erp.server.purchase.receipt.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.purchase.receipt.domain.ReceiptItem;

// ********** RECEIPT_ITEM의 기본 CRUD와 입고별 발주 품목·LOT 상세 조회를 처리하기 위한 Repository interface **********
public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {

	// ========== 한 입고의 품목·원본 발주 품목·품목 기본정보·입고 LOT를 발주 품목 순번 오름차순으로 조회하는 메서드 ==========
	@Query("""
			select distinct receiptItem
			from ReceiptItem receiptItem
			join fetch receiptItem.purchaseOrderItem purchaseOrderItem
			join fetch purchaseOrderItem.item
			left join fetch receiptItem.lots receiptLot
			left join fetch receiptLot.inventoryLot
			where receiptItem.receipt.receiptId = :receiptId
			order by purchaseOrderItem.lineNo asc
			""")
	List<ReceiptItem> findAllByReceiptIdWithDetails(@Param("receiptId") Long receiptId);
}
