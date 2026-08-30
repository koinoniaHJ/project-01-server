package com.erp.server.purchase.receipt.domain;

// ********** RECEIPT.status에서 사용할 입고 검수 진행 상태를 Java와 DB에서 같은 값으로 관리하기 위한 enum **********
public enum ReceiptStatus {

	PENDING,	// 검수 대기
	INSPECTING,	// 검수 중
	COMPLETED,	// 검수 완료
	CANCELED	// 검수 취소
}
