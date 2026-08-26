package com.erp.server.master.customer.dto;

import com.erp.server.master.customer.domain.CustomerTradeStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// ********** 거래처 거래 상태 변경값·변경 사유와 동시 수정 확인용 version을 전달받기 위한 요청 DTO record **********
// 거래처 거래 상태를 NORMAL 또는 HOLD로 변경할 때 변경 사유와 동시 수정 확인용 version을 전달받는다.
// 거래 상태가 실제로 변경되지 않는 요청은 이후 Service에서 차단
public record CustomerTradeStatusRequest(

        @NotNull(message = "거래처 거래 상태는 필수입니다.")
        CustomerTradeStatus tradeStatus,

        @NotBlank(message = "거래 상태 변경 사유는 필수입니다.")
        @Size(max = 1000, message = "거래 상태 변경 사유는 1000자 이하여야 합니다.")
        String reason, // reason은 CUSTOMER_TRADE_STATUS_HISTORY.reason에 보존

        @NotNull(message = "version은 필수입니다.")
        @PositiveOrZero(message = "version은 0 이상이어야 합니다.")
        Long version

) {
}