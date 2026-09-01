package com.erp.server.sales.shipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// ********** PACKED 출고의 포장 취소 사유와 최신 version을 전달하기 위한 요청 DTO record **********
public record ShipmentUnpackRequest(
		@NotBlank(message = "포장 취소 사유를 입력해 주세요.") @Size(max = 1000, message = "포장 취소 사유는 1000자 이하로 입력해 주세요.") String reason,
		@NotNull(message = "출고 version이 필요합니다.") @PositiveOrZero(message = "출고 version이 올바르지 않습니다.") Long version
) {
}
