package com.erp.server.purchase.order.controller;

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

import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.response.ApiResponse;
import com.erp.server.common.response.PageMeta;
import com.erp.server.common.security.AppUserDetails;
import com.erp.server.purchase.order.domain.PurchaseOrderEmailStatus;
import com.erp.server.purchase.order.domain.PurchaseOrderStatus;
import com.erp.server.purchase.order.dto.PurchaseOrderCancelRequest;
import com.erp.server.purchase.order.dto.PurchaseOrderCreateRequest;
import com.erp.server.purchase.order.dto.PurchaseOrderDetailResponse;
import com.erp.server.purchase.order.dto.PurchaseOrderEmailHistoryResponse;
import com.erp.server.purchase.order.dto.PurchaseOrderEmailSendResponse;
import com.erp.server.purchase.order.dto.PurchaseOrderListResponse;
import com.erp.server.purchase.order.dto.PurchaseOrderUpdateRequest;
import com.erp.server.purchase.order.dto.PurchaseOrderVersionRequest;
import com.erp.server.purchase.order.service.PurchaseOrderEmailService;
import com.erp.server.purchase.order.service.PurchaseOrderOrderService;
import com.erp.server.purchase.order.service.PurchaseOrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// ********** 발주 REST 요청을 받아 조회·작성·상태 처리와 이메일 업무 Service에 전달하기 위한 Controller 클래스 **********
@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

	private final PurchaseOrderService purchaseOrderService;
	private final PurchaseOrderOrderService purchaseOrderOrderService;
	private final PurchaseOrderEmailService purchaseOrderEmailService;

	// ========== 상태·이메일 상태·공급업체·등록 기간 조건으로 발주 목록을 페이지 조회하는 메서드 ==========
	@GetMapping
	public ApiResponse<List<PurchaseOrderListResponse>> getPurchaseOrders(
			@RequestParam(name = "status", required = false) PurchaseOrderStatus status,
			@RequestParam(name = "emailStatus", required = false) PurchaseOrderEmailStatus emailStatus,
			@RequestParam(name = "supplierId", required = false) Long supplierId,
			@RequestParam(name = "startDate", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(name = "endDate", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(name = "page", defaultValue = "0") int page,
			Authentication authentication) {

		AppUserDetails currentUser = getCurrentUser(authentication);
		Page<PurchaseOrderListResponse> purchaseOrders = purchaseOrderService.getPurchaseOrders(status,
				emailStatus, supplierId, startDate, endDate, page, currentUser.getRole());

		return ApiResponse.success(purchaseOrders.getContent(), PageMeta.from(purchaseOrders));
	}

	// ========== ACTIVE 공급업체와 취급 품목으로 DRAFT 발주를 등록하는 메서드 ==========
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<PurchaseOrderDetailResponse> createPurchaseOrder(
			@Valid @RequestBody PurchaseOrderCreateRequest request, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);

		return ApiResponse.success(purchaseOrderService.createPurchaseOrder(request, currentUser.getUserId()));
	}

	// ========== purchaseOrderId로 발주 기본정보·품목·입고 현황·처리 이력을 상세 조회하는 메서드 ==========
	@GetMapping("/{purchaseOrderId}")
	public ApiResponse<PurchaseOrderDetailResponse> getPurchaseOrder(
			@PathVariable(name = "purchaseOrderId") Long purchaseOrderId, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);

		return ApiResponse.success(purchaseOrderService.getPurchaseOrder(purchaseOrderId, currentUser.getRole()));
	}

	// ========== DRAFT 발주의 공급업체·품목·메모를 수정하는 메서드 ==========
	@PatchMapping("/{purchaseOrderId}")
	public ApiResponse<PurchaseOrderDetailResponse> updatePurchaseOrder(
			@PathVariable(name = "purchaseOrderId") Long purchaseOrderId,
			@Valid @RequestBody PurchaseOrderUpdateRequest request, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);

		return ApiResponse.success(
				purchaseOrderService.updatePurchaseOrder(purchaseOrderId, request, currentUser.getUserId()));
	}

	// ========== DRAFT 발주와 발주 품목을 물리 삭제하는 메서드 ==========
	@DeleteMapping("/{purchaseOrderId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deletePurchaseOrder(@PathVariable(name = "purchaseOrderId") Long purchaseOrderId,
			@Valid @RequestBody PurchaseOrderVersionRequest request) {
		purchaseOrderService.deletePurchaseOrder(purchaseOrderId, request.version());
	}

	// ========== DRAFT 발주를 SUBMITTED 승인 대기 상태로 변경하는 메서드 ==========
	@PostMapping("/{purchaseOrderId}/submit")
	public ApiResponse<PurchaseOrderDetailResponse> submitPurchaseOrder(
			@PathVariable(name = "purchaseOrderId") Long purchaseOrderId,
			@Valid @RequestBody PurchaseOrderVersionRequest request, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);

		return ApiResponse.success(purchaseOrderService.submitPurchaseOrder(purchaseOrderId, request.version(),
				currentUser.getUserId()));
	}

	// ========== SUBMITTED 발주를 ADMIN 승인하여 APPROVED 상태로 변경하는 메서드 ==========
	@PostMapping("/{purchaseOrderId}/approve")
	public ApiResponse<PurchaseOrderDetailResponse> approvePurchaseOrder(
			@PathVariable(name = "purchaseOrderId") Long purchaseOrderId,
			@Valid @RequestBody PurchaseOrderVersionRequest request, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);

		return ApiResponse.success(purchaseOrderService.approvePurchaseOrder(purchaseOrderId, request.version(),
				currentUser.getUserId()));
	}

	// ========== APPROVED 발주를 ORDERED로 먼저 커밋한 후 발주서 이메일을 자동 전송하는 메서드 ==========
	// 이메일 전송 실패는 발주 확정 성공 응답을 유지하고 MAIL_SEND_FAILED 경고 코드로 전달한다.
	@PostMapping("/{purchaseOrderId}/order")
	public ApiResponse<PurchaseOrderEmailSendResponse> orderPurchaseOrder(
			@PathVariable(name = "purchaseOrderId") Long purchaseOrderId,
			@Valid @RequestBody PurchaseOrderVersionRequest request, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);
		PurchaseOrderEmailSendResponse response = purchaseOrderOrderService.orderAndSendPurchaseOrder(
				purchaseOrderId, request.version(), currentUser.getUserId());
		List<String> warnings = response.emailStatus() == PurchaseOrderEmailStatus.FAILED
				? List.of(ErrorCode.MAIL_SEND_FAILED.getCode())
				: List.of();

		return ApiResponse.success(response, warnings);
	}

	// ========== ORDERED 발주의 발주서 PDF를 생성하여 공급업체 이메일로 재전송하는 메서드 ==========
	@PostMapping("/{purchaseOrderId}/email/resend")
	public ApiResponse<PurchaseOrderEmailSendResponse> resendPurchaseOrderEmail(
			@PathVariable(name = "purchaseOrderId") Long purchaseOrderId,
			@Valid @RequestBody PurchaseOrderVersionRequest request, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);

		return ApiResponse.success(purchaseOrderEmailService.resendPurchaseOrderEmail(purchaseOrderId,
				request.version(), currentUser.getUserId()));
	}

	// ========== 한 발주의 이메일 전송 이력을 최근 시도 순서로 페이지 조회하는 메서드 ==========
	@GetMapping("/{purchaseOrderId}/email-history")
	public ApiResponse<List<PurchaseOrderEmailHistoryResponse>> getEmailHistory(
			@PathVariable(name = "purchaseOrderId") Long purchaseOrderId,
			@RequestParam(name = "page", defaultValue = "0") int page) {
		Page<PurchaseOrderEmailHistoryResponse> emailHistory = purchaseOrderEmailService
				.getEmailHistory(purchaseOrderId, page);

		return ApiResponse.success(emailHistory.getContent(), PageMeta.from(emailHistory));
	}

	// ========== 허용된 상태와 입고·공급업체 확인 조건을 검증하여 발주를 CANCELED로 변경하는 메서드 ==========
	@PostMapping("/{purchaseOrderId}/cancel")
	public ApiResponse<PurchaseOrderDetailResponse> cancelPurchaseOrder(
			@PathVariable(name = "purchaseOrderId") Long purchaseOrderId,
			@Valid @RequestBody PurchaseOrderCancelRequest request, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);

		return ApiResponse.success(purchaseOrderService.cancelPurchaseOrder(purchaseOrderId, request,
				currentUser.getUserId()));
	}

	// ========== 인증 객체에서 현재 ERP 사용자 상세정보를 반환하는 메서드 ==========
	private AppUserDetails getCurrentUser(Authentication authentication) {
		return (AppUserDetails) authentication.getPrincipal();
	}
}
