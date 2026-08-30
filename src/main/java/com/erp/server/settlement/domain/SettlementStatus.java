package com.erp.server.settlement.domain;

// ********** SALES 전표의 입금 배분 결과에 따른 정산 상태를 VOUCHER.settlement_status 값과 동일하게 관리하기 위한 Enum **********
public enum SettlementStatus {
	// 유효 입금 배분액이 없고 미수 잔액이 남은 상태
	UNPAID,
	// 입금이 일부 배분되었지만 미수 잔액이 남은 상태
	PARTIALLY_PAID,
	// 정산 대상 금액이 모두 배분되어 미수 잔액이 없는 상태
	PAID
}
