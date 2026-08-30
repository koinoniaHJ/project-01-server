package com.erp.server.purchase.receipt.dto;

import java.math.BigDecimal;
import java.util.List;

import com.erp.server.master.item.domain.Item;
import com.erp.server.master.item.domain.ItemUnit;
import com.erp.server.purchase.order.domain.PurchaseOrderItem;
import com.erp.server.purchase.receipt.domain.ReceiptItem;

// ********** 입고 품목의 발주·누적·잔여·검수 수량과 LOT 구성을 반환하기 위한 응답 DTO record **********
public record ReceiptItemResponse(
		Long receiptItemId,
		Long purchaseOrderItemId,
		Integer lineNo,
		Long itemId,
		String itemCode,
		String itemName,
		ItemUnit unit,
		String otherUnitName,
		BigDecimal orderedQuantity,
		BigDecimal cumulativeReceivedQuantity,
		BigDecimal remainingQuantity,
		BigDecimal actualQuantity,
		BigDecimal normalQuantity,
		BigDecimal rejectedQuantity,
		String note,
		List<ReceiptLotResponse> lots
) {

	// ========== ReceiptItem과 원본 발주 품목을 수량·품목·LOT 상세 응답으로 변환하는 정적 팩토리 메서드 ==========
	public static ReceiptItemResponse from(ReceiptItem receiptItem) {
		PurchaseOrderItem purchaseOrderItem = receiptItem.getPurchaseOrderItem();
		Item item = purchaseOrderItem.getItem();
		List<ReceiptLotResponse> lots = receiptItem.getLots().stream().map(ReceiptLotResponse::from).toList();

		return new ReceiptItemResponse(receiptItem.getReceiptItemId(), purchaseOrderItem.getPurchaseOrderItemId(),
				purchaseOrderItem.getLineNo(), item.getItemId(), item.getItemCode(), item.getItemName(), item.getUnit(),
				item.getOtherUnitName(), purchaseOrderItem.getOrderedQuantity(),
				purchaseOrderItem.getReceivedQuantity(), purchaseOrderItem.calculateRemainingQuantity(),
				receiptItem.getActualQuantity(), receiptItem.getNormalQuantity(), receiptItem.getRejectedQuantity(),
				receiptItem.getNote(), lots);
	}
}
