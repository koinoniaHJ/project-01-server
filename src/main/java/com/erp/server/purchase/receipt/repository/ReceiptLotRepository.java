package com.erp.server.purchase.receipt.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.purchase.receipt.domain.ReceiptLot;

// ********** RECEIPT_LOT의 기본 CRUD와 검수 저장·완료 후 매입 반품에서 사용할 입고 LOT 데이터를 관리하기 위한 Repository interface **********
public interface ReceiptLotRepository extends JpaRepository<ReceiptLot, Long> {

	// ========== 완료 입고의 정상 LOT·재고 LOT·발주 품목·품목을 매입 반품 등록 기준으로 조회하는 메서드 ==========
	// 원본 발주 품목 순번과 입고 LOT 식별자 오름차순으로 반환하여 화면과 저장 순서를 고정한다.
	@Query("""
			select receiptLot
			from ReceiptLot receiptLot
			join fetch receiptLot.receiptItem receiptItem
			join fetch receiptItem.purchaseOrderItem purchaseOrderItem
			join fetch purchaseOrderItem.item
			join fetch receiptLot.inventoryLot inventoryLot
			where receiptItem.receipt.receiptId = :receiptId
			  and receiptItem.receipt.status = com.erp.server.purchase.receipt.domain.ReceiptStatus.COMPLETED
			  and receiptLot.normalQuantity > 0
			order by purchaseOrderItem.lineNo asc, receiptLot.receiptLotId asc
			""")
	List<ReceiptLot> findAllReturnableByReceiptId(@Param("receiptId") Long receiptId);
}
