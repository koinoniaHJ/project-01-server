package com.erp.server.sales.shipment.document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.erp.server.sales.order.domain.SalesOrder;
import com.erp.server.sales.order.domain.SalesOrderItem;
import com.erp.server.sales.shipment.domain.DeliveryNote;
import com.erp.server.sales.shipment.domain.ShipmentLot;

// ********** 납품서 PDF 생성에 필요한 발행·주문·배송·포장 LOT 스냅샷을 전달하기 위한 내부 record **********
public record DeliveryNoteDocumentData(
		Long shipmentId, Integer issueSequence, Long salesOrderId, LocalDateTime issuedAt,
		String customerCode, String customerName, String deliveryAddress, String deliveryAddressDetail,
		String recipientName, String recipientPhone, String warehouseCode, String warehouseName,
		List<Item> items
) {

	// ========== 유효 납품서와 현재 완료 또는 포장 확정 LOT를 PDF 전용 데이터로 변환하는 정적 팩토리 메서드 ==========
	public static DeliveryNoteDocumentData from(DeliveryNote deliveryNote, List<ShipmentLot> shipmentLots) {
		SalesOrder order = deliveryNote.getShipment().getSalesOrder();
		List<Item> items = shipmentLots.stream().map(shipmentLot -> {
			SalesOrderItem orderItem = shipmentLot.getSalesOrderItem();
			return new Item(orderItem.getLineNo(), orderItem.getItemCodeSnapshot(), orderItem.getItemNameSnapshot(),
					orderItem.getUnitSnapshot(), shipmentLot.getInventoryLot().getLotNumber(),
					shipmentLot.getInventoryLot().getExpiryDate(), shipmentLot.getPackedQuantity());
		}).toList();

		return new DeliveryNoteDocumentData(deliveryNote.getShipment().getShipmentId(),
				deliveryNote.getIssueSequence(), order.getSalesOrderId(), deliveryNote.getIssuedAt(),
				order.getCustomerCodeSnapshot(), order.getCustomerNameSnapshot(), order.getDeliveryAddressSnapshot(),
				order.getDeliveryAddressDetailSnapshot(), order.getRecipientNameSnapshot(),
				order.getRecipientPhoneSnapshot(), deliveryNote.getShipment().getWarehouse().getWarehouseCode(),
				deliveryNote.getShipment().getWarehouse().getWarehouseName(), items);
	}

	public record Item(Integer lineNo, String itemCode, String itemName, String unit, String lotNumber,
			LocalDate expiryDate, BigDecimal packedQuantity) {
	}
}
