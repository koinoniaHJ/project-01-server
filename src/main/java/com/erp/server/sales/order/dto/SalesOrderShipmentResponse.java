package com.erp.server.sales.order.dto;

import com.erp.server.sales.shipment.domain.ShipmentStatus;

// ********** 주문 상세에 1:1 연결 출고의 식별자·창고·상태·version을 반환하기 위한 응답 DTO record **********
public record SalesOrderShipmentResponse(Long shipmentId, Long warehouseId, String warehouseCode,
		String warehouseName, ShipmentStatus status, Integer packingSequence, Long version) {
}
