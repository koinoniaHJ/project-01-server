package com.erp.server.settlement.domain;

// ********** 원본 완료 업무에서 자동 생성하는 전표 유형을 VOUCHER.type 값과 동일하게 관리하기 위한 Enum **********
public enum VoucherType {
	// 출고 완료 금액을 거래처 미수금에 반영하는 매출 전표
	SALES,
	// 거래처 반품 완료 금액을 원본 매출에서 차감하는 매출 반품 전표
	SALES_RETURN,
	// 입고 검수 완료 금액을 공급업체 매입으로 기록하는 매입 전표
	PURCHASE,
	// 매입 반품 완료 금액을 원본 매입에서 차감하는 매입 반품 전표
	PURCHASE_RETURN;

	// ========== 거래처 미수금 정산에 사용하는 매출 계열 전표인지 확인하는 메서드 ==========
	public boolean isSalesType() {
		return this == SALES || this == SALES_RETURN;
	}

	// ========== 공급업체 매입 현황에 사용하는 매입 계열 전표인지 확인하는 메서드 ==========
	public boolean isPurchaseType() {
		return this == PURCHASE || this == PURCHASE_RETURN;
	}

	// ========== 원본 전표 금액을 차감하는 반품 전표인지 확인하는 메서드 ==========
	public boolean isReturnType() {
		return this == SALES_RETURN || this == PURCHASE_RETURN;
	}
}
