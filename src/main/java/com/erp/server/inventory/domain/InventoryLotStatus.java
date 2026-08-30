package com.erp.server.inventory.domain;

// ********** 재고 LOT의 출고 가능 여부를 INVENTORY_LOT.status 값과 동일하게 관리하기 위한 Enum **********
public enum InventoryLotStatus {
	// 출고 LOT 선택과 가용재고 계산에 포함할 수 있는 상태
	AVAILABLE,
	// 품질 이상 등의 사유로 출고 LOT 선택과 가용재고 계산에서 제외하는 상태
	BLOCKED
}
