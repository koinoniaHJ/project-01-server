package com.erp.server.sales.shipment.domain;

// ********** DELIVERY_NOTE.status에 저장되는 납품서 유효 상태를 Java와 DB에서 동일하게 관리하기 위한 Enum **********
public enum DeliveryNoteStatus {
	ACTIVE,
	VOID
}
