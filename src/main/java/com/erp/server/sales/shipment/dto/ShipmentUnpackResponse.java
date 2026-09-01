package com.erp.server.sales.shipment.dto;

import java.math.BigDecimal;

import com.erp.server.sales.shipment.domain.ShipmentStatus;

// ********** 포장 취소 후 출고 상태와 해제한 LOT·예약 수량을 반환하기 위한 응답 DTO record **********
public record ShipmentUnpackResponse(Long shipmentId, ShipmentStatus status, int releasedLotCount,
		BigDecimal releasedQuantity, Long version) {
}
