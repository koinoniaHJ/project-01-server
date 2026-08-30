package com.erp.server.inventory.domain;

// ********** 실제 현재 재고가 변한 원인을 STOCK_MOVEMENT.type 값과 동일하게 관리하기 위한 Enum **********
public enum StockMovementType {
	// 입고 검수 완료로 정상 입고 수량이 증가한 변동
	RECEIPT(StockMovementDirection.INCREASE),
	// 출고 완료로 포장된 수량이 감소한 변동
	SHIPMENT(StockMovementDirection.DECREASE),
	// 매입 반품 완료로 공급업체에 반환한 수량이 감소한 변동
	PURCHASE_RETURN(StockMovementDirection.DECREASE),
	// 거래처 반품 완료로 재판매 가능한 수량이 증가한 변동
	RETURN_IN(StockMovementDirection.INCREASE),
	// 재고 조정 승인으로 실사 차이만큼 증가한 변동
	ADJUSTMENT_IN(StockMovementDirection.INCREASE),
	// 재고 조정 승인으로 실사 차이만큼 감소한 변동
	ADJUSTMENT_OUT(StockMovementDirection.DECREASE),
	// 사용기한 경과 또는 출고 제한 LOT를 업무 사유와 함께 폐기한 변동
	DISPOSAL(StockMovementDirection.DECREASE);

	private final StockMovementDirection direction;

	// ========== 재고 변동 유형마다 허용된 증감 방향을 고정하는 생성자 ==========
	StockMovementType(StockMovementDirection direction) {
		this.direction = direction;
	}

	// ========== 재고 변동 유형에 고정된 증감 방향을 반환하는 메서드 ==========
	public StockMovementDirection getDirection() {
		return direction;
	}
}
