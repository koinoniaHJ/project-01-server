package com.erp.server.purchase.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// ********** 발주 삭제·승인 요청·승인·확정·이메일 재전송 시 동시 처리 충돌을 확인할 version을 전달받기 위한 요청 DTO record **********
public record PurchaseOrderVersionRequest(

		@NotNull(message = "version은 필수입니다.")
		@PositiveOrZero(message = "version은 0 이상이어야 합니다.")
		Long version
) {
}
