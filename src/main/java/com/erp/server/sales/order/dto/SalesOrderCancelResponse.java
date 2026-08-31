package com.erp.server.sales.order.dto;

import java.math.BigDecimal;

import com.erp.server.sales.order.domain.SalesOrderStatus;
import com.erp.server.sales.shipment.domain.ShipmentStatus;

// ********** 주문 취소 후 주문·출고 상태와 PACKED 예약 해제 결과를 반환하기 위한 응답 DTO record **********
public record SalesOrderCancelResponse(Long salesOrderId, SalesOrderStatus salesOrderStatus,
		Long shipmentId, ShipmentStatus shipmentStatus, int releasedLotCount, BigDecimal releasedQuantity) {
}
