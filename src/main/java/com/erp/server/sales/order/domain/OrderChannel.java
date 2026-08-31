package com.erp.server.sales.order.domain;

// ********** SALES_ORDER.channel에 저장되는 주문 접수 경로를 Java와 DB에서 동일하게 관리하기 위한 Enum **********
public enum OrderChannel {
	// 대표가 거래처를 방문하여 현장에서 접수한 주문
	VISIT,
	// 전화 통화로 접수한 주문
	PHONE,
	// 문자 또는 메시지로 접수한 주문
	MESSAGE
}
