package com.erp.server.purchase.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.repository.AppUserRepository;
import com.erp.server.purchase.order.domain.PurchaseOrder;
import com.erp.server.purchase.order.domain.PurchaseOrderEmailHistory;
import com.erp.server.purchase.order.domain.PurchaseOrderEmailStatus;
import com.erp.server.purchase.order.document.PurchaseOrderDocumentData;
import com.erp.server.purchase.order.dto.PurchaseOrderEmailHistoryResponse;
import com.erp.server.purchase.order.dto.PurchaseOrderEmailSendResponse;
import com.erp.server.purchase.order.repository.PurchaseOrderEmailHistoryRepository;
import com.erp.server.purchase.order.repository.PurchaseOrderRepository;

import lombok.RequiredArgsConstructor;

// ********** SMTP 외부 처리와 분리된 새 DB 트랜잭션에서 발주 이메일 현재 상태와 전송 시도 이력을 저장하기 위한 Service 클래스 **********
@Service
@RequiredArgsConstructor
public class PurchaseOrderEmailResultService {

	private final AppUserRepository appUserRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final PurchaseOrderEmailHistoryRepository purchaseOrderEmailHistoryRepository;

	// ========== 발주 행을 잠그고 이메일 SENT·FAILED 상태와 다음 시도 순번의 전송 이력을 별도 트랜잭션으로 저장하는 메서드 ==========
	// 호출 측에서 오류를 반환하더라도 REQUIRES_NEW 트랜잭션은 먼저 커밋되어 실패 이력이 보존된다.
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public PurchaseOrderEmailSendResponse recordEmailResult(PurchaseOrderDocumentData documentData,
			PurchaseOrderEmailStatus emailStatus, String errorMessage, Long currentUserId) {
		PurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdForUpdate(documentData.purchaseOrderId())
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "발주를 찾을 수 없습니다."));
		AppUser currentUser = appUserRepository.findById(currentUserId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));
		int attemptNo = purchaseOrderEmailHistoryRepository
				.findMaxAttemptNo(documentData.purchaseOrderId()) + 1;

		if (attemptNo > 99999) {
			throw new BusinessException(ErrorCode.CONFLICT, "발주 이메일 전송 이력의 최대 시도 횟수를 초과했습니다.");
		}

		purchaseOrder.changeEmailStatus(emailStatus);

		PurchaseOrderEmailHistory history = PurchaseOrderEmailHistory.create(purchaseOrder, attemptNo,
				documentData.recipientEmail(), emailStatus, errorMessage, currentUser);
		PurchaseOrderEmailHistory savedHistory = purchaseOrderEmailHistoryRepository.save(history);

		// 발주 현재 이메일 상태와 전송 이력 INSERT를 즉시 실행하여 같은 트랜잭션으로 함께 확정한다.
		purchaseOrderEmailHistoryRepository.flush();

		return new PurchaseOrderEmailSendResponse(purchaseOrder.getPurchaseOrderId(), purchaseOrder.getStatus(),
				purchaseOrder.getEmailStatus(), PurchaseOrderEmailHistoryResponse.from(savedHistory),
				purchaseOrder.getVersion());
	}
}
