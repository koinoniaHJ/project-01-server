package com.erp.server.purchase.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.erp.server.common.user.domain.UserRole;
import com.erp.server.purchase.order.domain.PurchaseOrderEmailStatus;
import com.erp.server.purchase.order.domain.PurchaseOrderStatus;
import com.erp.server.purchase.order.repository.PurchaseOrderRepository.PurchaseOrderListProjection;

// ********** 발주 목록의 공급업체·상태·수량·금액·등록·확정 일시를 반환하고 역할별 민감 정보를 제한하기 위한 응답 DTO record **********
public record PurchaseOrderListResponse(
		Long purchaseOrderId,
		Long supplierId,
		String supplierCode,
		String supplierName,
		PurchaseOrderStatus status,
		PurchaseOrderEmailStatus emailStatus,
		BigDecimal totalOrderedQuantity,
		BigDecimal totalReceivedQuantity,
		BigDecimal totalRemainingQuantity,
		BigDecimal totalAmount,
		LocalDateTime createdAt,
		LocalDateTime orderedAt,
		Long version
) {

	// ========== 목록 Query 결과를 역할별 조회 범위가 적용된 PurchaseOrderListResponse로 변환하는 정적 팩토리 메서드 ==========
	public static PurchaseOrderListResponse from(PurchaseOrderListProjection row, UserRole userRole) {
		boolean warehouse = userRole == UserRole.WAREHOUSE;

		return new PurchaseOrderListResponse(row.getPurchaseOrderId(), row.getSupplierId(), row.getSupplierCode(),
				row.getSupplierName(), row.getStatus(), warehouse ? null : row.getEmailStatus(),
				row.getTotalOrderedQuantity(), row.getTotalReceivedQuantity(), row.getTotalRemainingQuantity(),
				warehouse ? null : row.getTotalAmount(), row.getCreatedAt(), row.getOrderedAt(), row.getVersion());
	}
}
