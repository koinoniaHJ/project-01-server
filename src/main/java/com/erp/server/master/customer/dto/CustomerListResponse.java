package com.erp.server.master.customer.dto;

import java.math.BigDecimal;

import com.erp.server.common.user.domain.UserRole;
import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.customer.domain.Customer;
import com.erp.server.master.customer.domain.CustomerTradeStatus;

// ********** 거래처 목록 화면에 필요한 기본정보를 반환하고 WAREHOUSE 역할에는 총미수금을 숨기기 위한 응답 DTO record **********
// 목록과 상세 응답을 분리 / 목록과 상세 화면의 표시 필드가 다름.
// => 목록에서 WAREHOUSE 역할에는 총미수금을 전달하면 안 됨. 목록 API에서 주소·배송지·메모까지 불필요하게 조회·전달할 필요가 없음.
// 목록 수정에 version을 사용하지 않고, 거래처 선택 시 상세 API에서 최신 정보와 version을 조회
public record CustomerListResponse(Long customerId, String customerCode, String customerName, String phone,
		MasterStatus status, CustomerTradeStatus tradeStatus, BigDecimal totalReceivableAmount) {

	// ========== Customer Entity를 역할별 조회 범위가 적용된 CustomerListResponse로 변환하는 정적 팩토리 메서드 ==========
	public static CustomerListResponse from(Customer customer, UserRole userRole) {

		BigDecimal totalReceivableAmount = userRole == UserRole.WAREHOUSE ? null : customer.getTotalReceivableAmount();

		return new CustomerListResponse(customer.getCustomerId(), customer.getCustomerCode(),
				customer.getCustomerName(), customer.getPhone(), customer.getStatus(), customer.getTradeStatus(),
				totalReceivableAmount);
	}
}