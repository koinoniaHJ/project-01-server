package com.erp.server.sales.shipment.domain;

// ********** SHIPMENT.status에 저장되는 출고 처리 상태를 Java와 DB에서 동일하게 관리하기 위한 Enum **********
public enum ShipmentStatus {
	PENDING,
	PACKED,
	COMPLETED,
	CANCELED
}
