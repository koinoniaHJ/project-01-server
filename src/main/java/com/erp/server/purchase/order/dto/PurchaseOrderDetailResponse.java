package com.erp.server.purchase.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.erp.server.common.user.domain.UserRole;
import com.erp.server.purchase.order.domain.PurchaseOrder;
import com.erp.server.purchase.order.domain.PurchaseOrderEmailStatus;
import com.erp.server.purchase.order.domain.PurchaseOrderItem;
import com.erp.server.purchase.order.domain.PurchaseOrderStatus;

// ********** 발주 기본정보·품목·입고 현황·처리 이력과 최신 version을 반환하고 역할별 민감 정보를 제한하기 위한 응답 DTO record **********
public record PurchaseOrderDetailResponse(
		Long purchaseOrderId,
		Long supplierId,
		String supplierCode,
		String supplierName,
		String supplierEmail,
		PurchaseOrderStatus status,
		PurchaseOrderEmailStatus emailStatus,
		BigDecimal totalAmount,
		String memo,
		List<PurchaseOrderItemResponse> items,
		PurchaseOrderActionResponse submitted,
		PurchaseOrderActionResponse approved,
		PurchaseOrderActionResponse ordered,
		PurchaseOrderActionResponse canceled,
		String cancelReason,
		PurchaseOrderActionResponse closed,
		String closeReason,
		PurchaseOrderActionResponse supplierCancelConfirmed,
		PurchaseOrderActionResponse created,
		LocalDateTime updatedAt,
		Long version
) {

	// ========== PurchaseOrder와 표시 순서대로 조회한 품목을 역할별 상세 응답으로 변환하는 정적 팩토리 메서드 ==========
	public static PurchaseOrderDetailResponse from(PurchaseOrder purchaseOrder,
			List<PurchaseOrderItem> purchaseOrderItems, UserRole userRole) {
		boolean warehouse = userRole == UserRole.WAREHOUSE;
		List<PurchaseOrderItemResponse> items = purchaseOrderItems.stream()
				.map(item -> PurchaseOrderItemResponse.from(item, userRole))
				.toList();

		return new PurchaseOrderDetailResponse(purchaseOrder.getPurchaseOrderId(),
				purchaseOrder.getSupplier().getSupplierId(), purchaseOrder.getSupplier().getSupplierCode(),
				purchaseOrder.getSupplier().getSupplierName(), purchaseOrder.getSupplier().getEmail(),
				purchaseOrder.getStatus(), warehouse ? null : purchaseOrder.getEmailStatus(),
				warehouse ? null : purchaseOrder.getTotalAmount(), purchaseOrder.getMemo(), items,
				PurchaseOrderActionResponse.from(purchaseOrder.getSubmittedBy(), purchaseOrder.getSubmittedAt()),
				PurchaseOrderActionResponse.from(purchaseOrder.getApprovedBy(), purchaseOrder.getApprovedAt()),
				PurchaseOrderActionResponse.from(purchaseOrder.getOrderedBy(), purchaseOrder.getOrderedAt()),
				PurchaseOrderActionResponse.from(purchaseOrder.getCanceledBy(), purchaseOrder.getCanceledAt()),
				purchaseOrder.getCancelReason(),
				PurchaseOrderActionResponse.from(purchaseOrder.getClosedBy(), purchaseOrder.getClosedAt()),
				purchaseOrder.getCloseReason(),
				PurchaseOrderActionResponse.from(purchaseOrder.getSupplierCancelConfirmedBy(),
						purchaseOrder.getSupplierCancelConfirmedAt()),
				PurchaseOrderActionResponse.from(purchaseOrder.getCreatedBy(), purchaseOrder.getCreatedAt()),
				purchaseOrder.getUpdatedAt(), purchaseOrder.getVersion());
	}
}
