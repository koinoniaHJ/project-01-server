package com.erp.server.sales.order.dto;

import com.erp.server.sales.order.domain.SalesOrderStatus;
import com.erp.server.sales.shipment.domain.ShipmentStatus;

// ********** 주문 접수 후 생성된 주문·출고 식별자와 상태·최신 version을 반환하기 위한 응답 DTO record **********
public record SalesOrderRegisterResponse(Long salesOrderId, SalesOrderStatus salesOrderStatus,
		Long salesOrderVersion, Long shipmentId, ShipmentStatus shipmentStatus, Long shipmentVersion) {
}
