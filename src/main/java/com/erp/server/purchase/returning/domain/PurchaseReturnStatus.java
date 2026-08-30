package com.erp.server.purchase.returning.domain;

// ********** 매입 반품의 등록·완료·취소 상태를 PURCHASE_RETURN.status 값과 동일하게 관리하기 위한 Enum **********
public enum PurchaseReturnStatus {
	// 반품 품목과 수량을 등록하여 완료 또는 취소 전까지 수정할 수 있는 상태
	REGISTERED,
	// 재고 감소·변동 이력·매입 반품 전표 반영을 모두 마친 종료 상태
	COMPLETED,
	// 완료 전에 취소하여 재고와 전표에 반영하지 않은 종료 상태
	CANCELED
}
