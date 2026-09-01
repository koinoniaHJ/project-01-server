package com.erp.server.sales.shipment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.server.common.user.domain.UserRole;
import com.erp.server.master.customer.domain.CustomerTradeStatus;
import com.erp.server.sales.order.domain.OrderChannel;
import com.erp.server.sales.order.domain.SalesOrder;
import com.erp.server.sales.order.domain.SalesOrderItem;
import com.erp.server.sales.order.domain.SalesOrderStatus;
import com.erp.server.sales.shipment.domain.DeliveryNote;
import com.erp.server.sales.shipment.domain.Shipment;
import com.erp.server.sales.shipment.domain.ShipmentLot;
import com.erp.server.sales.shipment.domain.ShipmentStatus;

// ********** 출고·주문·배송·품목·LOT·납품서·처리 이력과 최신 version을 반환하기 위한 응답 DTO record **********
public record ShipmentDetailResponse(
		Long shipmentId, Long salesOrderId, SalesOrderStatus salesOrderStatus, Long customerId, String customerCode,
		String customerName, CustomerTradeStatus customerTradeStatus, boolean customerHold, OrderChannel channel,
		String deliveryPostalCode, String deliveryAddress, String deliveryAddressDetail, String recipientName,
		String recipientPhone, String memo, BigDecimal totalAmount, Long warehouseId, String warehouseCode,
		String warehouseName, ShipmentStatus status, Integer packingSequence, List<ShipmentOrderItemResponse> items,
		List<DeliveryNoteResponse> deliveryNotes, ShipmentActionResponse packed, ShipmentActionResponse completed,
		ShipmentActionResponse canceled, LocalDateTime createdAt, LocalDateTime updatedAt, Long version
) {
	// ========== 출고 상세 구성요소를 역할별 판매 금액 공개 범위에 맞는 하나의 응답으로 변환하는 메서드 ==========
	public static ShipmentDetailResponse from(Shipment shipment, List<SalesOrderItem> orderItems,
			List<ShipmentLot> shipmentLots, List<DeliveryNote> deliveryNotes, UserRole role,
			Set<Long> restrictedInventoryLotIds) {
		SalesOrder order = shipment.getSalesOrder();
		Map<Long, List<ShipmentLot>> lotsByOrderItem = shipmentLots.stream().collect(Collectors.groupingBy(
				lot -> lot.getSalesOrderItem().getSalesOrderItemId()));
		List<ShipmentOrderItemResponse> itemResponses = orderItems.stream()
				.map(item -> ShipmentOrderItemResponse.from(item,
						lotsByOrderItem.getOrDefault(item.getSalesOrderItemId(), List.of()), role,
						restrictedInventoryLotIds))
				.toList();

		return new ShipmentDetailResponse(shipment.getShipmentId(), order.getSalesOrderId(), order.getStatus(),
				order.getCustomer().getCustomerId(), order.getCustomerCodeSnapshot(), order.getCustomerNameSnapshot(),
				order.getCustomer().getTradeStatus(), order.getCustomer().getTradeStatus() == CustomerTradeStatus.HOLD,
				order.getChannel(), order.getDeliveryPostalCodeSnapshot(), order.getDeliveryAddressSnapshot(),
				order.getDeliveryAddressDetailSnapshot(), order.getRecipientNameSnapshot(),
				order.getRecipientPhoneSnapshot(), order.getMemo(), role == UserRole.WAREHOUSE ? null : order.getTotalAmount(),
				shipment.getWarehouse() == null ? null : shipment.getWarehouse().getWarehouseId(),
				shipment.getWarehouse() == null ? null : shipment.getWarehouse().getWarehouseCode(),
				shipment.getWarehouse() == null ? null : shipment.getWarehouse().getWarehouseName(),
				shipment.getStatus(), shipment.getPackingSequence(), itemResponses,
				deliveryNotes.stream().map(DeliveryNoteResponse::from).toList(),
				ShipmentActionResponse.from(shipment.getPackedBy(), shipment.getPackedAt()),
				ShipmentActionResponse.from(shipment.getCompletedBy(), shipment.getCompletedAt()),
				ShipmentActionResponse.from(shipment.getCanceledBy(), shipment.getCanceledAt()),
				shipment.getCreatedAt(), shipment.getUpdatedAt(), shipment.getVersion());
	}
}
