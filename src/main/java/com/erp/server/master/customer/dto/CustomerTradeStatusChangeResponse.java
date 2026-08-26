package com.erp.server.master.customer.dto;

import com.erp.server.common.user.domain.UserRole;
import com.erp.server.master.customer.domain.Customer;
import com.erp.server.master.customer.domain.CustomerTradeStatusHistory;

// ********** 거래처 거래 상태 변경 결과와 새로 생성된 변경 이력을 함께 반환하기 위한 응답 DTO record **********
public record CustomerTradeStatusChangeResponse(
        CustomerDetailResponse customer, // 변경 후 최신 거래처 상태와 version
        CustomerTradeStatusHistoryResponse history // 이번 요청으로 생성된 변경 이력
) {

    // ========== 변경된 Customer와 생성된 거래 상태 이력을 하나의 응답으로 변환하는 정적 팩토리 메서드 ==========
    public static CustomerTradeStatusChangeResponse from(Customer customer,
            CustomerTradeStatusHistory history, UserRole currentUserRole) {

        return new CustomerTradeStatusChangeResponse(
                CustomerDetailResponse.from(customer, currentUserRole),
                CustomerTradeStatusHistoryResponse.from(history)
        );
    }
}