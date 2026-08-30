package com.erp.server.settlement.domain;

// ********** 거래처 입금 수단을 PAYMENT.method 값과 동일하게 관리하기 위한 Enum **********
public enum PaymentMethod {
	BANK_TRANSFER, // 계좌 이체
	CASH,          // 현금
	CARD,          // 카드
	OTHER          // 그 밖의 입금 방법
}
