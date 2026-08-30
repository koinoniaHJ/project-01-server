package com.erp.server.master.warehouse.dto;

import java.math.BigDecimal;

import com.erp.server.master.item.domain.ItemUnit;

// ********** 안전재고 화면에 창고·품목 기준과 가용재고 계산 결과 및 최신 version을 반환하기 위한 응답 DTO record **********
public record WarehouseItemListResponse(
		Long warehouseId,
		String warehouseCode,
		String warehouseName,
		Long itemId,
		String itemCode,
		String itemName,
		ItemUnit unit,
		String otherUnitName, // unit이 OTHER일 때 화면에 표시할 실제 단위명이다.
		BigDecimal safetyStockQuantity,
		BigDecimal availableStockQuantity,
		BigDecimal shortageQuantity,
		boolean belowSafetyStock,
		Long version // 미등록 조합은 null이며 최초 저장 후 0부터 시작한다.
) {
}
