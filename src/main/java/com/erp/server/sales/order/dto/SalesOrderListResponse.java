package com.erp.server.sales.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.erp.server.master.customer.domain.CustomerTradeStatus;
import com.erp.server.sales.order.domain.OrderChannel;
import com.erp.server.sales.order.domain.SalesOrderStatus;

// ********** 주문 목록에 표시할 거래처·접수 경로·상태·거래 중지 경고·금액을 반환하기 위한 응답 DTO record **********
public record SalesOrderListResponse(Long salesOrderId, Long customerId, String customerCode, String customerName,
		OrderChannel channel, SalesOrderStatus status, CustomerTradeStatus customerTradeStatus,
		boolean tradeHoldWarning, BigDecimal totalAmount, LocalDateTime createdAt, LocalDateTime registeredAt,
		Long version) {
}
