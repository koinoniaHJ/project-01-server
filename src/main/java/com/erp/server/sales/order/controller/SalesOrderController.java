package com.erp.server.sales.order.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.erp.server.sales.order.domain.SalesOrderStatus;
import com.erp.server.sales.order.dto.SalesOrderCancelRequest;
import com.erp.server.sales.order.dto.SalesOrderCancelResponse;
import com.erp.server.sales.order.dto.SalesOrderCreateRequest;
import com.erp.server.sales.order.dto.SalesOrderDetailResponse;
import com.erp.server.sales.order.dto.SalesOrderListResponse;
import com.erp.server.sales.order.dto.SalesOrderRegisterResponse;
import com.erp.server.sales.order.dto.SalesOrderUpdateRequest;
import com.erp.server.sales.order.dto.SalesOrderVersionRequest;
import com.erp.server.sales.order.service.SalesOrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// ********** 주문 REST 요청을 받아 목록·상세·작성·접수·취소 업무 Service에 전달하기 위한 Controller 클래스 **********
@RestController
@RequestMapping("/api/v1/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {

	private final SalesOrderService salesOrderService;

	// ========== 거래처·상태·등록 기간 조건으로 주문 목록을 페이지 조회하는 메서드 ==========
	@GetMapping
	public ApiResponse<List<SalesOrderListResponse>> getSalesOrders(
			@RequestParam(name = "customerId", required = false) Long customerId,
			@RequestParam(name = "status", required = false) SalesOrderStatus status,
			@RequestParam(name = "startDate", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(name = "endDate", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(name = "page", defaultValue = "0") int page, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);
		Page<SalesOrderListResponse> salesOrders = salesOrderService.getSalesOrders(customerId, status,
				startDate, endDate, page, currentUser.getRole());
		return ApiResponse.success(salesOrders.getContent(), PageMeta.from(salesOrders));
	}

	// ========== ACTIVE 거래처·품목과 배송정보로 DRAFT 주문을 생성하는 메서드 ==========
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SalesOrderDetailResponse> createSalesOrder(
			@Valid @RequestBody SalesOrderCreateRequest request, Authentication authentication) {
		return ApiResponse.success(salesOrderService.createSalesOrder(request,
				getCurrentUser(authentication).getUserId()));
	}

	// ========== salesOrderId로 주문 기본정보·품목·처리 이력·연결 출고를 상세 조회하는 메서드 ==========
	@GetMapping("/{salesOrderId}")
	public ApiResponse<SalesOrderDetailResponse> getSalesOrder(
			@PathVariable(name = "salesOrderId") Long salesOrderId, Authentication authentication) {
		return ApiResponse.success(salesOrderService.getSalesOrder(salesOrderId,
				getCurrentUser(authentication).getRole()));
	}

	// ========== DRAFT 주문의 거래처·배송정보·품목을 수정하는 메서드 ==========
	@PatchMapping("/{salesOrderId}")
	public ApiResponse<SalesOrderDetailResponse> updateSalesOrder(
			@PathVariable(name = "salesOrderId") Long salesOrderId,
			@Valid @RequestBody SalesOrderUpdateRequest request, Authentication authentication) {
		return ApiResponse.success(salesOrderService.updateSalesOrder(salesOrderId, request,
				getCurrentUser(authentication).getUserId()));
	}

	// ========== DRAFT 주문과 주문 품목을 물리 삭제하는 메서드 ==========
	@DeleteMapping("/{salesOrderId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteSalesOrder(@PathVariable(name = "salesOrderId") Long salesOrderId,
			@Valid @RequestBody SalesOrderVersionRequest request) {
		salesOrderService.deleteSalesOrder(salesOrderId, request.version());
	}

	// ========== 최신 기준정보를 검증하여 주문을 접수하고 1:1 PENDING 출고를 생성하는 메서드 ==========
	@PostMapping("/{salesOrderId}/register")
	public ApiResponse<SalesOrderRegisterResponse> registerSalesOrder(
			@PathVariable(name = "salesOrderId") Long salesOrderId,
			@Valid @RequestBody SalesOrderVersionRequest request, Authentication authentication) {
		return ApiResponse.success(salesOrderService.registerSalesOrder(salesOrderId, request.version(),
				getCurrentUser(authentication).getUserId()));
	}

	// ========== 연결 출고 상태를 확인하여 REGISTERED 주문과 출고를 함께 취소하는 메서드 ==========
	@PostMapping("/{salesOrderId}/cancel")
	public ApiResponse<SalesOrderCancelResponse> cancelSalesOrder(
			@PathVariable(name = "salesOrderId") Long salesOrderId,
			@Valid @RequestBody SalesOrderCancelRequest request, Authentication authentication) {
		return ApiResponse.success(salesOrderService.cancelSalesOrder(salesOrderId, request,
				getCurrentUser(authentication).getUserId()));
	}

	// ========== 인증 객체에서 현재 ERP 사용자 상세정보를 반환하는 메서드 ==========
	private AppUserDetails getCurrentUser(Authentication authentication) {
		return (AppUserDetails) authentication.getPrincipal();
	}
}
