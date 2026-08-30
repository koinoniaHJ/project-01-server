package com.erp.server.purchase.receipt.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.erp.server.purchase.order.domain.PurchaseOrderStatus;
import com.erp.server.purchase.receipt.domain.Receipt;
import com.erp.server.purchase.receipt.domain.ReceiptItem;
import com.erp.server.purchase.receipt.domain.ReceiptRemainderAction;
import com.erp.server.purchase.receipt.domain.ReceiptStatus;

// ********** 입고 기본정보·발주·공급업체·창고·검수 품목·LOT·처리 이력과 최신 version을 반환하기 위한 응답 DTO record **********
public record ReceiptDetailResponse(
		Long receiptId,
		Long purchaseOrderId,
		PurchaseOrderStatus purchaseOrderStatus,
		Long supplierId,
		String supplierCode,
		String supplierName,
		Long warehouseId,
		String warehouseCode,
		String warehouseName,
		ReceiptStatus status,
		ReceiptRemainderAction remainderAction,
		String remainderReason,
		List<ReceiptItemResponse> items,
		ReceiptActionResponse inspectionStarted,
		ReceiptActionResponse completed,
		ReceiptActionResponse canceled,
		String cancelReason,
		ReceiptActionResponse created,
		Long purchaseVoucherId,
		LocalDateTime updatedAt,
		Long version
) {

	// ========== Receipt와 발주 품목 순서대로 조회한 품목·전표 식별자를 상세 응답으로 변환하는 정적 팩토리 메서드 ==========
	public static ReceiptDetailResponse from(Receipt receipt, List<ReceiptItem> receiptItems,
			Long purchaseVoucherId) {
		List<ReceiptItemResponse> items = receiptItems.stream().map(ReceiptItemResponse::from).toList();

		return new ReceiptDetailResponse(receipt.getReceiptId(), receipt.getPurchaseOrder().getPurchaseOrderId(),
				receipt.getPurchaseOrder().getStatus(), receipt.getPurchaseOrder().getSupplier().getSupplierId(),
				receipt.getPurchaseOrder().getSupplier().getSupplierCode(),
				receipt.getPurchaseOrder().getSupplier().getSupplierName(), receipt.getWarehouse().getWarehouseId(),
				receipt.getWarehouse().getWarehouseCode(), receipt.getWarehouse().getWarehouseName(),
				receipt.getStatus(), receipt.getRemainderAction(), receipt.getRemainderReason(), items,
				ReceiptActionResponse.from(receipt.getInspectionStartedBy(), receipt.getInspectionStartedAt()),
				ReceiptActionResponse.from(receipt.getCompletedBy(), receipt.getCompletedAt()),
				ReceiptActionResponse.from(receipt.getCanceledBy(), receipt.getCanceledAt()),
				receipt.getCancelReason(), ReceiptActionResponse.from(receipt.getCreatedBy(), receipt.getCreatedAt()),
				purchaseVoucherId, receipt.getUpdatedAt(), receipt.getVersion());
	}
}
