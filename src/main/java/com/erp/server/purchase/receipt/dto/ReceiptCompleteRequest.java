package com.erp.server.purchase.receipt.dto;

import com.erp.server.purchase.receipt.domain.ReceiptRemainderAction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// ********** 검수 완료 시 잔여 수량 처리 방식·사유와 최신 version을 전달하기 위한 요청 DTO record **********
public record ReceiptCompleteRequest(
		ReceiptRemainderAction remainderAction,
		@Size(max = 1000, message = "잔여 수량 처리 사유는 1000자 이하로 입력해 주세요.") String remainderReason,
		Boolean cancelPurchaseOrder,
		@Size(max = 1000, message = "발주 취소 사유는 1000자 이하로 입력해 주세요.") String cancelReason,
		Boolean supplierCancelConfirmed,
		@NotNull(message = "입고 version이 필요합니다.") @PositiveOrZero(message = "입고 version이 올바르지 않습니다.") Long version
) {
}
