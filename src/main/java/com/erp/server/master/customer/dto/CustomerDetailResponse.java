package com.erp.server.master.customer.dto;

import java.math.BigDecimal;

import com.erp.server.common.user.domain.UserRole;
import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.customer.domain.Customer;
import com.erp.server.master.customer.domain.CustomerTradeStatus;

// ********** 거래처 상세정보와 최신 version을 반환하고 WAREHOUSE 역할에는 거래처 메모와 총미수금을 숨기기 위한 응답 DTO record **********
// 거래처 상세 조회와 등록·수정·상태 변경 응답에 사용할 DTO
// WAREHOUSE 역할에는 설계에 따라 거래처 메모와 총미수금을 숨긴다. 거래 상태 변경 이력은 별도 API로 조회하므로 포함하지 않는다.
public record CustomerDetailResponse(
        Long customerId,
        String customerCode, // customerCode는 조회만 가능하며 수정 요청에는 포함되지 않는다.
        String customerName,
        String phone,
        String email,
        String postalCode,
        String address,
        String addressDetail,
        String deliveryPostalCode,
        String deliveryAddress,
        String deliveryAddressDetail,
        String recipientName,
        String recipientPhone,
        String memo,	// 예외 판매 단가 등 참고사항
        MasterStatus status,
        CustomerTradeStatus tradeStatus,
        BigDecimal totalReceivableAmount,
        Long version // version은 수정과 상태 변경 요청에 사용
) {

    // ========== Customer Entity를 역할별 조회 범위가 적용된 CustomerDetailResponse로 변환하는 정적 팩토리 메서드 ==========
    public static CustomerDetailResponse from(Customer customer, UserRole userRole) {

        boolean warehouseUser = userRole == UserRole.WAREHOUSE;

        String memo = warehouseUser ? null : customer.getMemo();
        BigDecimal totalReceivableAmount = warehouseUser ? null : customer.getTotalReceivableAmount();

        return new CustomerDetailResponse(
                customer.getCustomerId(),
                customer.getCustomerCode(),
                customer.getCustomerName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getPostalCode(),
                customer.getAddress(),
                customer.getAddressDetail(),
                customer.getDeliveryPostalCode(),
                customer.getDeliveryAddress(),
                customer.getDeliveryAddressDetail(),
                customer.getRecipientName(),
                customer.getRecipientPhone(),
                memo,
                customer.getStatus(),
                customer.getTradeStatus(),
                totalReceivableAmount,
                customer.getVersion()
        );
    }
}