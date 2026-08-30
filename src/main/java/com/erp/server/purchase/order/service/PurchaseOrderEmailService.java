package com.erp.server.purchase.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;
import com.erp.server.purchase.order.document.PurchaseOrderDocumentData;
import com.erp.server.purchase.order.document.PurchaseOrderDocumentService;
import com.erp.server.purchase.order.document.PurchaseOrderPdfService;
import com.erp.server.purchase.order.domain.PurchaseOrderEmailStatus;
import com.erp.server.purchase.order.dto.PurchaseOrderEmailHistoryResponse;
import com.erp.server.purchase.order.dto.PurchaseOrderEmailSendResponse;
import com.erp.server.purchase.order.mail.PurchaseOrderMailSender;
import com.erp.server.purchase.order.repository.PurchaseOrderEmailHistoryRepository;
import com.erp.server.purchase.order.repository.PurchaseOrderRepository;

import lombok.RequiredArgsConstructor;

// ********** 발주서 문서 조회·PDF 생성·SMTP 전송과 자동 전송·재전송 결과 처리를 조정하기 위한 Service 클래스 **********
@Service
@RequiredArgsConstructor
public class PurchaseOrderEmailService {

	private static final Logger log = LoggerFactory.getLogger(PurchaseOrderEmailService.class);
	private static final int EMAIL_HISTORY_PAGE_SIZE = 20;
	private static final int ERROR_MESSAGE_MAX_LENGTH = 2000;

	private final PurchaseOrderRepository purchaseOrderRepository;
	private final PurchaseOrderEmailHistoryRepository purchaseOrderEmailHistoryRepository;
	private final PurchaseOrderDocumentService purchaseOrderDocumentService;
	private final PurchaseOrderPdfService purchaseOrderPdfService;
	private final PurchaseOrderMailSender purchaseOrderMailSender;
	private final PurchaseOrderEmailResultService purchaseOrderEmailResultService;

	// ========== 발주 확정 커밋 후 발주서 PDF를 자동 전송하고 성공·실패 결과를 반환하는 메서드 ==========
	// 전송 실패도 결과 이력을 저장한 뒤 정상 반환하여 Controller가 HTTP 200과 MAIL_SEND_FAILED 경고를 구성할 수 있게 한다.
	public PurchaseOrderEmailSendResponse sendAfterOrder(Long purchaseOrderId, Long currentUserId) {
		PurchaseOrderDocumentData documentData = purchaseOrderDocumentService
				.getPurchaseOrderDocument(purchaseOrderId, null);

		return sendAndRecord(documentData, currentUserId);
	}

	// ========== 최신 version을 검증하고 발주서 이메일을 재전송하는 메서드 ==========
	// 재전송 실패는 이력을 먼저 저장한 후 이메일 전송 자체의 실패를 나타내는 502 업무 예외를 반환한다.
	public PurchaseOrderEmailSendResponse resendPurchaseOrderEmail(Long purchaseOrderId, Long requestVersion,
			Long currentUserId) {
		PurchaseOrderDocumentData documentData = purchaseOrderDocumentService
				.getPurchaseOrderDocument(purchaseOrderId, requestVersion);
		PurchaseOrderEmailSendResponse response = sendAndRecord(documentData, currentUserId);

		if (response.emailStatus() == PurchaseOrderEmailStatus.FAILED) {
			throw new BusinessException(ErrorCode.MAIL_SEND_FAILED, "발주서 이메일 재전송에 실패했습니다.");
		}

		return response;
	}

	// ========== 한 발주의 이메일 전송 이력을 최근 시도 순서로 페이지 조회하는 메서드 ==========
	public Page<PurchaseOrderEmailHistoryResponse> getEmailHistory(Long purchaseOrderId, int page) {
		if (page < 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "페이지 번호는 0 이상이어야 합니다.");
		}

		if (!purchaseOrderRepository.existsById(purchaseOrderId)) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "발주를 찾을 수 없습니다.");
		}

		PageRequest pageable = PageRequest.of(page, EMAIL_HISTORY_PAGE_SIZE,
				Sort.by(Sort.Order.desc("attemptedAt"), Sort.Order.desc("emailHistoryId")));

		return purchaseOrderEmailHistoryRepository.findAllByPurchaseOrderId(purchaseOrderId, pageable)
				.map(PurchaseOrderEmailHistoryResponse::from);
	}

	// ========== PDF 생성과 SMTP 전송을 수행하고 결과와 오류 내용을 별도 DB 트랜잭션에 저장하는 메서드 ==========
	private PurchaseOrderEmailSendResponse sendAndRecord(PurchaseOrderDocumentData documentData,
			Long currentUserId) {
		PurchaseOrderEmailStatus emailStatus;
		String errorMessage = null;

		try {
			byte[] pdfBytes = purchaseOrderPdfService.createPurchaseOrderPdf(documentData);
			purchaseOrderMailSender.sendPurchaseOrder(documentData, pdfBytes);
			emailStatus = PurchaseOrderEmailStatus.SENT;

		} catch (Exception exception) {
			emailStatus = PurchaseOrderEmailStatus.FAILED;
			errorMessage = createErrorMessage(exception);
			log.warn("발주서 이메일 전송에 실패했습니다. purchaseOrderId={}",
					documentData.purchaseOrderId(), exception);
		}

		return purchaseOrderEmailResultService.recordEmailResult(documentData, emailStatus, errorMessage,
				currentUserId);
	}

	// ========== 외부 처리 예외 메시지를 한 줄로 정리하고 DB 컬럼에 저장 가능한 길이로 제한하는 메서드 ==========
	private String createErrorMessage(Exception exception) {
		String message = exception.getMessage();

		if (message == null || message.isBlank()) {
			message = exception.getClass().getSimpleName();
		}

		String normalizedMessage = message.replaceAll("[\\r\\n]+", " ").trim();

		return normalizedMessage.length() <= ERROR_MESSAGE_MAX_LENGTH
				? normalizedMessage
				: normalizedMessage.substring(0, ERROR_MESSAGE_MAX_LENGTH);
	}
}
