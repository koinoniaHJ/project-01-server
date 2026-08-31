package com.erp.server.sales.order.domain;

// ********** SALES_ORDER.status에 저장되는 주문 처리 상태를 Java와 DB에서 동일하게 관리하기 위한 Enum **********
public enum SalesOrderStatus {
	// 거래처·배송지·품목·판매 단가를 작성하거나 수정할 수 있는 상태
	DRAFT,
	// 주문 접수가 완료되어 출고 대기 건이 생성된 상태
	REGISTERED,
	// 연결 출고가 완료되어 주문 업무까지 종료된 상태
	COMPLETED,
	// 출고 완료 전에 주문과 연결 출고가 함께 취소된 상태
	CANCELED
}
