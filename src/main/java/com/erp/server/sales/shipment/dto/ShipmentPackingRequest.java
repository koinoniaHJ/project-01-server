package com.erp.server.sales.shipment.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

// ********** PENDING 출고의 단일 창고·LOT별 포장 수량과 최신 version을 전달하기 위한 요청 DTO record **********
public record ShipmentPackingRequest(
		@NotNull(message = "출고 창고를 선택해 주세요.") @Positive(message = "창고 식별자가 올바르지 않습니다.") Long warehouseId,
		@NotEmpty(message = "포장 LOT를 하나 이상 입력해 주세요.") List<@Valid ShipmentLotAllocationRequest> lotAllocations,
		@NotNull(message = "출고 version이 필요합니다.") @PositiveOrZero(message = "출고 version이 올바르지 않습니다.") Long version
) {
}
