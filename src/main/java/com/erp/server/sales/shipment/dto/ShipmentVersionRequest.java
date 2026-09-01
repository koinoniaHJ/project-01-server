package com.erp.server.sales.shipment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// ********** 포장 확정·출고 완료처럼 추가 입력 없이 최신 출고 version을 검증하기 위한 요청 DTO record **********
public record ShipmentVersionRequest(
		@NotNull(message = "출고 version이 필요합니다.") @PositiveOrZero(message = "출고 version이 올바르지 않습니다.") Long version
) {
}
