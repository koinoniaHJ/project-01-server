package com.erp.server.sales.shipment.dto;

import java.math.BigDecimal;

import com.erp.server.sales.order.domain.SalesOrderStatus;
import com.erp.server.sales.shipment.domain.ShipmentStatus;
import com.erp.server.settlement.domain.SettlementStatus;

// ********** 실제 출고 완료 후 주문·출고 상태와 생성된 매출 전표 정산 결과를 반환하기 위한 응답 DTO record **********
public record ShipmentCompleteResponse(Long shipmentId, ShipmentStatus shipmentStatus, Long salesOrderId,
		SalesOrderStatus salesOrderStatus, Long salesVoucherId, BigDecimal voucherTotalAmount,
		BigDecimal allocatedAmount, BigDecimal outstandingAmount, SettlementStatus settlementStatus, Long version) {
}
