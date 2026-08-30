package com.erp.server.purchase.receipt.dto;

import java.time.LocalDateTime;

import com.erp.server.purchase.receipt.domain.Receipt;
import com.erp.server.purchase.receipt.domain.ReceiptStatus;

// ********** 입고 목록의 발주·공급업체·창고·검수 상태와 주요 처리 일시를 반환하기 위한 응답 DTO record **********
public record ReceiptListResponse(
		Long receiptId,
		Long purchaseOrderId,
		Long supplierId,
		String supplierCode,
		String supplierName,
		Long warehouseId,
		String warehouseCode,
		String warehouseName,
		ReceiptStatus status,
		LocalDateTime createdAt,
		LocalDateTime inspectionStartedAt,
		LocalDateTime completedAt,
		Long version
) {

	// ========== Receipt Entity를 입고 목록 한 행 응답으로 변환하는 정적 팩토리 메서드 ==========
	public static ReceiptListResponse from(Receipt receipt) {
		return new ReceiptListResponse(receipt.getReceiptId(), receipt.getPurchaseOrder().getPurchaseOrderId(),
				receipt.getPurchaseOrder().getSupplier().getSupplierId(),
				receipt.getPurchaseOrder().getSupplier().getSupplierCode(),
				receipt.getPurchaseOrder().getSupplier().getSupplierName(), receipt.getWarehouse().getWarehouseId(),
				receipt.getWarehouse().getWarehouseCode(), receipt.getWarehouse().getWarehouseName(),
				receipt.getStatus(), receipt.getCreatedAt(), receipt.getInspectionStartedAt(), receipt.getCompletedAt(),
				receipt.getVersion());
	}
}
