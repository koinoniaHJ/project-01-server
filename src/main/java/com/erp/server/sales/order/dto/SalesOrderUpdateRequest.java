package com.erp.server.sales.order.dto;

import java.util.List;

import com.erp.server.sales.order.domain.OrderChannel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// ********** DRAFT 주문의 거래처·배송정보·품목과 최신 version을 전달하여 수정하기 위한 요청 DTO record **********
public record SalesOrderUpdateRequest(
		@NotNull(message = "거래처 식별자는 필수입니다.")
		@Positive(message = "거래처 식별자는 1 이상이어야 합니다.") Long customerId,
		@NotNull(message = "주문 접수 경로는 필수입니다.") OrderChannel channel,
		@Size(max = 10, message = "배송지 우편번호는 10자 이하로 입력해 주세요.") String deliveryPostalCode,
		@Size(max = 500, message = "배송지 주소는 500자 이하로 입력해 주세요.") String deliveryAddress,
		@Size(max = 300, message = "배송지 상세 주소는 300자 이하로 입력해 주세요.") String deliveryAddressDetail,
		@Size(max = 100, message = "수령인은 100자 이하로 입력해 주세요.") String recipientName,
		@Size(max = 30, message = "수령인 연락처는 30자 이하로 입력해 주세요.") String recipientPhone,
		@Size(max = 2000, message = "주문 메모는 2000자 이하로 입력해 주세요.") String memo,
		@NotEmpty(message = "주문 품목을 하나 이상 선택해 주세요.") List<@Valid SalesOrderItemRequest> items,
		@NotNull(message = "주문 version은 필수입니다.")
		@PositiveOrZero(message = "주문 version은 0 이상이어야 합니다.") Long version
) {
}
