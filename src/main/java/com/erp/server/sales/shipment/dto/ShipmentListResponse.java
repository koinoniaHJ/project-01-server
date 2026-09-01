package com.erp.server.sales.shipment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.erp.server.common.user.domain.UserRole;
import com.erp.server.master.customer.domain.CustomerTradeStatus;
import com.erp.server.sales.order.domain.SalesOrder;
import com.erp.server.sales.shipment.domain.Shipment;
import com.erp.server.sales.shipment.domain.ShipmentStatus;

// ********** 출고 목록의 주문·거래처·창고·상태·처리 일시와 역할별 공개 금액을 반환하기 위한 응답 DTO record **********
public record ShipmentListResponse(
		Long shipmentId, Long salesOrderId, Long customerId, String customerCode, String customerName,
		CustomerTradeStatus customerTradeStatus, boolean customerHold, Long warehouseId, String warehouseCode,
		String warehouseName, ShipmentStatus status, Integer packingSequence, BigDecimal totalAmount,
		LocalDateTime registeredAt, LocalDateTime packedAt, LocalDateTime completedAt, Long version
) {
	// ========== Shipment Entity를 WAREHOUSE 역할의 판매 금액 비공개 범위에 맞는 목록 응답으로 변환하는 메서드 ==========
	public static ShipmentListResponse from(Shipment shipment, UserRole role) {
		SalesOrder order = shipment.getSalesOrder();
		return new ShipmentListResponse(shipment.getShipmentId(), order.getSalesOrderId(),
				order.getCustomer().getCustomerId(), order.getCustomerCodeSnapshot(), order.getCustomerNameSnapshot(),
				order.getCustomer().getTradeStatus(), order.getCustomer().getTradeStatus() == CustomerTradeStatus.HOLD,
				shipment.getWarehouse() == null ? null : shipment.getWarehouse().getWarehouseId(),
				shipment.getWarehouse() == null ? null : shipment.getWarehouse().getWarehouseCode(),
				shipment.getWarehouse() == null ? null : shipment.getWarehouse().getWarehouseName(),
				shipment.getStatus(), shipment.getPackingSequence(),
				role == UserRole.WAREHOUSE ? null : order.getTotalAmount(), order.getRegisteredAt(),
				shipment.getPackedAt(), shipment.getCompletedAt(), shipment.getVersion());
	}
}
