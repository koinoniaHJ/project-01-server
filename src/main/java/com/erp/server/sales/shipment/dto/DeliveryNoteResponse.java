package com.erp.server.sales.shipment.dto;

import com.erp.server.sales.shipment.domain.DeliveryNote;
import com.erp.server.sales.shipment.domain.DeliveryNoteStatus;

// ********** 출고 포장 회차별 납품서 발행·무효 이력과 PDF 조회 회차를 반환하기 위한 응답 DTO record **********
public record DeliveryNoteResponse(Long deliveryNoteId, Integer issueSequence, DeliveryNoteStatus status,
		ShipmentActionResponse issued, ShipmentActionResponse voided, String voidReason) {
	public static DeliveryNoteResponse from(DeliveryNote deliveryNote) {
		return new DeliveryNoteResponse(deliveryNote.getDeliveryNoteId(), deliveryNote.getIssueSequence(),
				deliveryNote.getStatus(), ShipmentActionResponse.from(deliveryNote.getIssuedBy(), deliveryNote.getIssuedAt()),
				ShipmentActionResponse.from(deliveryNote.getVoidedBy(), deliveryNote.getVoidedAt()),
				deliveryNote.getVoidReason());
	}
}
