package com.erp.server.purchase.order.dto;

import java.math.BigDecimal;

import com.erp.server.common.user.domain.UserRole;
import com.erp.server.master.item.domain.ItemUnit;
import com.erp.server.purchase.order.domain.PurchaseOrderItem;

// ********** 발주 상세의 품목·단위·수량·단가·금액·입고 현황을 반환하고 WAREHOUSE 역할에는 금액 정보를 숨기기 위한 응답 DTO record **********
public record PurchaseOrderItemResponse(
		Long purchaseOrderItemId,
		Integer lineNo,
		Long itemId,
		String itemCode,
		String itemName,
		ItemUnit unit,
		String otherUnitName,
		BigDecimal orderedQuantity,
		BigDecimal unitPrice,
		BigDecimal lineAmount,
		BigDecimal receivedQuantity,
		BigDecimal remainingQuantity
) {

	// ========== PurchaseOrderItem Entity를 역할별 조회 범위가 적용된 품목 응답으로 변환하는 정적 팩토리 메서드 ==========
	public static PurchaseOrderItemResponse from(PurchaseOrderItem purchaseOrderItem, UserRole userRole) {
		boolean warehouse = userRole == UserRole.WAREHOUSE;

		return new PurchaseOrderItemResponse(purchaseOrderItem.getPurchaseOrderItemId(),
				purchaseOrderItem.getLineNo(), purchaseOrderItem.getItem().getItemId(),
				purchaseOrderItem.getItem().getItemCode(), purchaseOrderItem.getItem().getItemName(),
				purchaseOrderItem.getItem().getUnit(), purchaseOrderItem.getItem().getOtherUnitName(),
				purchaseOrderItem.getOrderedQuantity(), warehouse ? null : purchaseOrderItem.getUnitPrice(),
				warehouse ? null : purchaseOrderItem.getLineAmount(), purchaseOrderItem.getReceivedQuantity(),
				purchaseOrderItem.getOrderedQuantity().subtract(purchaseOrderItem.getReceivedQuantity()));
	}
}
