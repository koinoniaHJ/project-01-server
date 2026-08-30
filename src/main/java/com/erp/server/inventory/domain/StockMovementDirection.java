package com.erp.server.inventory.domain;

import java.math.BigDecimal;

// ********** 재고 변동 유형별 현재 재고 증감 방향과 이력 수량의 부호를 일관되게 적용하기 위한 Enum **********
public enum StockMovementDirection {
	// 입고·판매 반품 입고·증가 조정처럼 현재 재고를 증가시키는 방향
	INCREASE(1),
	// 출고·매입 반품·감소 조정·폐기처럼 현재 재고를 감소시키는 방향
	DECREASE(-1);

	private final int sign;

	// ========== 재고 증감 방향마다 STOCK_MOVEMENT.change_quantity에 적용할 부호를 설정하는 생성자 ==========
	StockMovementDirection(int sign) {
		this.sign = sign;
	}

	// ========== 양수 수량에 증감 방향의 부호를 적용하여 재고 변동 이력용 수량을 반환하는 메서드 ==========
	public BigDecimal apply(BigDecimal quantity) {
		return quantity.multiply(BigDecimal.valueOf(sign));
	}

	// ========== 현재 재고를 증가시키는 방향인지 확인하는 메서드 ==========
	public boolean isIncrease() {
		return this == INCREASE;
	}

	// ========== 현재 재고를 감소시키는 방향인지 확인하는 메서드 ==========
	public boolean isDecrease() {
		return this == DECREASE;
	}
}
