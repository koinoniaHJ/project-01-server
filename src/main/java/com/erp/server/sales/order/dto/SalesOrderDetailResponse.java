package com.erp.server.sales.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.customer.domain.CustomerTradeStatus;
import com.erp.server.sales.order.domain.OrderChannel;
import com.erp.server.sales.order.domain.SalesOrderStatus;

// ********** 주문 기본정보·배송 스냅샷·품목·처리 이력·연결 출고를 반환하기 위한 상세 응답 DTO record **********
public record SalesOrderDetailResponse(Long salesOrderId, Long customerId, String customerCode, String customerName,
		MasterStatus customerStatus, CustomerTradeStatus customerTradeStatus, boolean tradeHoldWarning,
		OrderChannel channel, SalesOrderStatus status, String deliveryPostalCode, String deliveryAddress,
		String deliveryAddressDetail, String recipientName, String recipientPhone, BigDecimal totalAmount,
		String memo, List<SalesOrderItemResponse> items, SalesOrderShipmentResponse shipment,
		Long createdBy, String createdByName, LocalDateTime createdAt, Long registeredBy, String registeredByName,
		LocalDateTime registeredAt, Long canceledBy, String canceledByName, LocalDateTime canceledAt,
		String cancelReason, LocalDateTime updatedAt, Long version) {
}
