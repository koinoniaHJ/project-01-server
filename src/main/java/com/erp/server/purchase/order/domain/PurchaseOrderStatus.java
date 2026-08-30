package com.erp.server.purchase.order.domain;

// ********** PURCHASE_ORDER.status에서 사용할 발주 진행 상태를 Java와 DB에서 같은 값으로 관리하기 위한 enum **********
public enum PurchaseOrderStatus {

	DRAFT,		// 작성 중
	SUBMITTED,	// 승인 대기
	APPROVED,	// 승인 완료
	ORDERED,	// 발주 확정
	CANCELED,	// 발주 취소
	RECEIVED,	// 전량 입고 완료
	CLOSED		// 잔여 미입고 종료
}
