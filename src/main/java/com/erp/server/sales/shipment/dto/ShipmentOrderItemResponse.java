package com.erp.server.sales.shipment.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.erp.server.common.user.domain.UserRole;
import com.erp.server.sales.order.domain.SalesOrderItem;
import com.erp.server.sales.shipment.domain.ShipmentLot;

// ********** 주문 품목 수량·역할별 판매 금액과 현재 포장 LOT 구성을 반환하기 위한 응답 DTO record **********
public record ShipmentOrderItemResponse(
		Long salesOrderItemId, Integer lineNo, Long itemId, String itemCode, String itemName, String unit,
		BigDecimal orderQuantity, BigDecimal packedQuantity, BigDecimal unitPrice, BigDecimal lineAmount,
		List<ShipmentLotResponse> lots
) {
	// ========== 주문 품목과 배정 LOT를 WAREHOUSE 역할의 금액 비공개 범위에 맞는 응답으로 변환하는 메서드 ==========
	public static ShipmentOrderItemResponse from(SalesOrderItem orderItem, List<ShipmentLot> lots,
			UserRole role, Set<Long> restrictedInventoryLotIds) {
		List<ShipmentLotResponse> lotResponses = lots.stream().map(lot -> ShipmentLotResponse.from(lot,
				restrictedInventoryLotIds.contains(lot.getInventoryLot().getInventoryLotId()))).toList();
		BigDecimal packedQuantity = lots.stream().map(ShipmentLot::getPackedQuantity)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		boolean warehouse = role == UserRole.WAREHOUSE;
		return new ShipmentOrderItemResponse(orderItem.getSalesOrderItemId(), orderItem.getLineNo(),
				orderItem.getItem().getItemId(), orderItem.getItemCodeSnapshot(), orderItem.getItemNameSnapshot(),
				orderItem.getUnitSnapshot(), orderItem.getOrderQuantity(), packedQuantity,
				warehouse ? null : orderItem.getUnitPrice(), warehouse ? null : orderItem.getLineAmount(), lotResponses);
	}
}
