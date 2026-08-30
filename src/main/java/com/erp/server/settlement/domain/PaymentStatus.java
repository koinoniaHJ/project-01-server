package com.erp.server.settlement.domain;

// ********** 거래처 입금의 사용 가능 여부를 PAYMENT.status 값과 동일하게 관리하기 위한 Enum **********
public enum PaymentStatus {
	// 미결 매출 전표 자동 배분에 사용할 수 있는 유효한 입금 상태
	ACTIVE,
	// 등록 오류 또는 실제 취소로 무효화되어 정산에 사용할 수 없는 종료 상태
	CANCELED
}
