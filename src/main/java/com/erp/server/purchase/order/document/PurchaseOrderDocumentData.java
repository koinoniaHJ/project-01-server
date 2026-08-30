package com.erp.server.purchase.order.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ********** DB 트랜잭션 밖에서 발주서 PDF를 생성할 수 있도록 필요한 값을 불변 형태로 보관하기 위한 문서 데이터 record **********
public record PurchaseOrderDocumentData(
		Long purchaseOrderId,
		String supplierCode,
		String supplierName,
		String recipientEmail,
		LocalDateTime orderedAt,
		String memo,
		BigDecimal totalAmount,
		List<Item> items,
		Long version
) {

	// ********** 발주서 품목 행에 출력할 순번·품목·단위·수량·단가·금액을 보관하기 위한 내부 record **********
	public record Item(
			Integer lineNo,
			String itemCode,
			String itemName,
			String unitName,
			BigDecimal orderedQuantity,
			BigDecimal unitPrice,
			BigDecimal lineAmount
	) {
	}
}
