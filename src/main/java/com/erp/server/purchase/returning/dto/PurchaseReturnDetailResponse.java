package com.erp.server.purchase.returning.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.erp.server.purchase.returning.domain.PurchaseReturnStatus;

// ********** 매입 반품 기본정보·원본 입고·공급업체·창고·LOT 품목·처리 이력과 최신 version을 반환하기 위한 응답 DTO record **********
public record PurchaseReturnDetailResponse(
		Long purchaseReturnId,
		Long receiptId,
		Long purchaseOrderId,
		Long supplierId,
		String supplierCode,
		String supplierName,
		Long warehouseId,
		String warehouseCode,
		String warehouseName,
		PurchaseReturnStatus status,
		String reason,
		BigDecimal totalAmount,
		List<PurchaseReturnItemResponse> items,
		PurchaseReturnActionResponse created,
		PurchaseReturnActionResponse completed,
		PurchaseReturnActionResponse canceled,
		String cancelReason,
		Long purchaseReturnVoucherId,
		LocalDateTime updatedAt,
		Long version
) {
}
