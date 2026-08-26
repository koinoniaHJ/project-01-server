package com.erp.server.master.customer.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.erp.server.common.response.ApiResponse;
import com.erp.server.common.response.PageMeta;
import com.erp.server.common.security.AppUserDetails;
import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.customer.domain.CustomerTradeStatus;
import com.erp.server.master.customer.dto.CustomerCreateRequest;
import com.erp.server.master.customer.dto.CustomerDetailResponse;
import com.erp.server.master.customer.dto.CustomerListResponse;
import com.erp.server.master.customer.dto.CustomerStatusRequest;
import com.erp.server.master.customer.dto.CustomerTradeStatusChangeResponse;
import com.erp.server.master.customer.dto.CustomerTradeStatusHistoryResponse;
import com.erp.server.master.customer.dto.CustomerTradeStatusRequest;
import com.erp.server.master.customer.dto.CustomerUpdateRequest;
import com.erp.server.master.customer.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// ********** 거래처 REST 요청을 받아 입력값과 현재 사용자 정보를 CustomerService에 전달하기 위한 Controller 클래스 **********
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerService customerService;

	// ========== 키워드·사용 상태·거래 상태 조건을 적용하여 거래처 목록을 조회하는 메서드 ==========
	// keyword, status, tradeStatus는 선택 조건
	@GetMapping
	public ApiResponse<List<CustomerListResponse>> getCustomers(
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "status", required = false) MasterStatus status,
			@RequestParam(name = "tradeStatus", required = false) CustomerTradeStatus tradeStatus,
			@PageableDefault(size = 20, sort = "customerId", direction = Sort.Direction.DESC) Pageable pageable,
			Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		Page<CustomerListResponse> customers = customerService.getCustomers(keyword, status, tradeStatus,
				currentUser.getRole(), pageable);

		return ApiResponse.success(customers.getContent(), PageMeta.from(customers));
	}

	// ========== 신규 거래처를 등록하는 메서드 ==========
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<CustomerDetailResponse> createCustomer(@Valid @RequestBody CustomerCreateRequest request,
			Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(customerService.createCustomer(request, currentUser.getUserId()));
	}

	// ========== customerId로 거래처 상세정보를 조회하는 메서드 ==========
	// WAREHOUSE에는 Service 응답 변환 과정에서 거래처 메모와 총미수금이 null로 반환
	// 존재하지 않는 customerId는 404 RESOURCE_NOT_FOUND를 반환
	@GetMapping("/{customerId}")
	public ApiResponse<CustomerDetailResponse> getCustomer(@PathVariable(name = "customerId") Long customerId,
			Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(customerService.getCustomer(customerId, currentUser.getRole()));
	}

	// ========== 거래처 기본정보와 기본 배송정보를 수정하는 메서드 ==========
	@PatchMapping("/{customerId}")
	public ApiResponse<CustomerDetailResponse> updateCustomer(@PathVariable(name = "customerId") Long customerId,
			@Valid @RequestBody CustomerUpdateRequest request, Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(customerService.updateCustomer(customerId, request, currentUser.getUserId()));
	}

	// ========== 거래처 ACTIVE·INACTIVE 사용 상태를 변경하는 메서드 ==========
	@PostMapping("/{customerId}/status")
	public ApiResponse<CustomerDetailResponse> changeStatus(@PathVariable(name = "customerId") Long customerId,
			@Valid @RequestBody CustomerStatusRequest request, Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(customerService.changeStatus(customerId, request, currentUser.getUserId()));
	}

	// ========== 거래처 NORMAL·HOLD 거래 상태를 변경하고 생성된 변경 이력을 반환하는 메서드 ==========
	@PostMapping("/{customerId}/trade-status")
	public ApiResponse<CustomerTradeStatusChangeResponse> changeTradeStatus(
			@PathVariable(name = "customerId") Long customerId, @Valid @RequestBody CustomerTradeStatusRequest request,
			Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(customerService.changeTradeStatus(customerId, request, currentUser.getUserId()));
	}

	// ========== 거래처별 거래 상태 변경 이력을 최근 변경 순서로 페이지 조회하는 메서드 ==========
	@GetMapping("/{customerId}/trade-status-history")
	public ApiResponse<List<CustomerTradeStatusHistoryResponse>> getTradeStatusHistory(
			@PathVariable(name = "customerId") Long customerId, @PageableDefault(size = 20) Pageable pageable) {

		Page<CustomerTradeStatusHistoryResponse> histories = customerService.getTradeStatusHistory(customerId,
				pageable);

		return ApiResponse.success(histories.getContent(), PageMeta.from(histories));
	}
}