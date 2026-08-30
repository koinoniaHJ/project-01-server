package com.erp.server.purchase.order.domain;

// ********** 발주 이메일의 현재 상태와 전송 이력 상태를 Java와 DB에서 같은 값으로 관리하기 위한 enum **********
public enum PurchaseOrderEmailStatus {

	SENT,	// 전송 성공
	FAILED	// 전송 실패
}
