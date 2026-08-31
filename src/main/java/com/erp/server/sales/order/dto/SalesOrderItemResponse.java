package com.erp.server.sales.order.dto;

import java.math.BigDecimal;

// ********** 주문 상세에 품목 스냅샷·수량·단가·금액을 반환하기 위한 응답 DTO record **********
public record SalesOrderItemResponse(Long salesOrderItemId, Integer lineNo, Long itemId, String itemCode,
		String itemName, String unit, BigDecimal orderQuantity, BigDecimal unitPrice, BigDecimal lineAmount) {
}
