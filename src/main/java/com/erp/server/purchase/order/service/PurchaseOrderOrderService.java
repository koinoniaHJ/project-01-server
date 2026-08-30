package com.erp.server.purchase.order.service;

import org.springframework.stereotype.Service;

import com.erp.server.purchase.order.dto.PurchaseOrderEmailSendResponse;

import lombok.RequiredArgsConstructor;

// ********** 발주 확정 DB 트랜잭션을 먼저 완료한 뒤 PDF 생성과 SMTP 자동 전송을 순서대로 실행하기 위한 조정 Service 클래스 **********
@Service
@RequiredArgsConstructor
public class PurchaseOrderOrderService {

	private final PurchaseOrderService purchaseOrderService;
	private final PurchaseOrderEmailService purchaseOrderEmailService;

	// ========== APPROVED 발주를 ORDERED로 커밋한 후 발주서 이메일을 자동 전송하는 메서드 ==========
	// PurchaseOrderService의 트랜잭션 프록시 호출이 반환된 시점에 발주 확정이 커밋되므로 이후 PDF·SMTP 실패가 ORDERED를 롤백하지 않는다.
	public PurchaseOrderEmailSendResponse orderAndSendPurchaseOrder(Long purchaseOrderId, Long requestVersion,
			Long currentUserId) {
		purchaseOrderService.orderPurchaseOrder(purchaseOrderId, requestVersion, currentUserId);

		return purchaseOrderEmailService.sendAfterOrder(purchaseOrderId, currentUserId);
	}
}
