package com.erp.server.purchase.returning.dto;

import java.util.List;

// ********** 신규 매입 반품 등록에 필요한 완료 입고·공급업체·창고·원본 LOT별 가능 수량을 반환하기 위한 응답 DTO record **********
public record PurchaseReturnSourceResponse(
		Long receiptId,
		Long purchaseOrderId,
		Long supplierId,
		String supplierCode,
		String supplierName,
		Long warehouseId,
		String warehouseCode,
		String warehouseName,
		List<PurchaseReturnSourceItemResponse> items
) {
}
