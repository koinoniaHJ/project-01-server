package com.erp.server.purchase.receipt.domain;

// ********** RECEIPT.remainder_action에서 사용할 잔여 미입고 수량 처리 방식을 Java와 DB에서 같은 값으로 관리하기 위한 enum **********
public enum ReceiptRemainderAction {

	ADDITIONAL_RECEIPT,	// 잔여 수량을 이후 입고에서 추가 검수
	CLOSE_REMAINDER		// 일부 정상 입고 후 잔여 미입고 수량 종료
}
