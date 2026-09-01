package com.erp.server.sales.shipment.dto;

import com.erp.server.sales.shipment.domain.ShipmentStatus;

// ********** 포장 확정 후 출고 상태·회차와 새로 발행한 납품서 식별자를 반환하기 위한 응답 DTO record **********
public record ShipmentPackResponse(Long shipmentId, ShipmentStatus status, Integer packingSequence,
		Long deliveryNoteId, Integer deliveryNoteIssueSequence, Long version) {
}
