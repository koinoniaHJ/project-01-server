package com.erp.server.purchase.returning.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.erp.server.common.user.domain.UserRole;
import com.erp.server.purchase.receipt.domain.Receipt;
import com.erp.server.purchase.returning.domain.PurchaseReturn;
import com.erp.server.purchase.returning.domain.PurchaseReturnStatus;

// ********** 매입 반품 목록의 원본 입고·공급업체·창고·상태·금액·처리 일시를 역할별로 반환하기 위한 응답 DTO record **********
public record PurchaseReturnListResponse(
		Long purchaseReturnId,
		Long receiptId,
		Long supplierId,
		String supplierCode,
		String supplierName,
		Long warehouseId,
		String warehouseCode,
		String warehouseName,
		PurchaseReturnStatus status,
		String reason,
		BigDecimal totalAmount,
		LocalDateTime createdAt,
		LocalDateTime completedAt,
		Long version
) {

	// ========== PurchaseReturn을 WAREHOUSE 금액 비공개 범위가 적용된 목록 응답으로 변환하는 정적 팩토리 메서드 ==========
	public static PurchaseReturnListResponse from(PurchaseReturn purchaseReturn, UserRole userRole) {
		Receipt receipt = purchaseReturn.getReceipt();
		boolean warehouse = userRole == UserRole.WAREHOUSE;

		return new PurchaseReturnListResponse(purchaseReturn.getPurchaseReturnId(), receipt.getReceiptId(),
				receipt.getPurchaseOrder().getSupplier().getSupplierId(),
				receipt.getPurchaseOrder().getSupplier().getSupplierCode(),
				receipt.getPurchaseOrder().getSupplier().getSupplierName(), receipt.getWarehouse().getWarehouseId(),
				receipt.getWarehouse().getWarehouseCode(), receipt.getWarehouse().getWarehouseName(),
				purchaseReturn.getStatus(), purchaseReturn.getReason(),
				warehouse ? null : purchaseReturn.getTotalAmount(), purchaseReturn.getCreatedAt(),
				purchaseReturn.getCompletedAt(), purchaseReturn.getVersion());
	}
}
