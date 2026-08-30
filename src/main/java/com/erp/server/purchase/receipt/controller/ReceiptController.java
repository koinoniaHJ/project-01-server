package com.erp.server.purchase.receipt.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.erp.server.common.response.ApiResponse;
import com.erp.server.common.response.PageMeta;
import com.erp.server.common.security.AppUserDetails;
import com.erp.server.purchase.receipt.domain.ReceiptStatus;
import com.erp.server.purchase.receipt.dto.ReceiptCancelRequest;
import com.erp.server.purchase.receipt.dto.ReceiptCompleteRequest;
import com.erp.server.purchase.receipt.dto.ReceiptCompleteResponse;
import com.erp.server.purchase.receipt.dto.ReceiptCreateRequest;
import com.erp.server.purchase.receipt.dto.ReceiptDetailResponse;
import com.erp.server.purchase.receipt.dto.ReceiptInspectionRequest;
import com.erp.server.purchase.receipt.dto.ReceiptListResponse;
import com.erp.server.purchase.receipt.dto.ReceiptVersionRequest;
import com.erp.server.purchase.receipt.dto.ReceiptWarehouseUpdateRequest;
import com.erp.server.purchase.receipt.service.ReceiptService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// ********** 입고 REST 요청을 받아 목록·상세·등록·검수 저장·완료·취소 업무 Service에 전달하기 위한 Controller 클래스 **********
@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
public class ReceiptController {

	private final ReceiptService receiptService;

	// ========== 발주·검수 상태·입고 등록 기간 조건으로 입고 목록을 페이지 조회하는 메서드 ==========
	@GetMapping
	public ApiResponse<List<ReceiptListResponse>> getReceipts(
			@RequestParam(name = "purchaseOrderId", required = false) Long purchaseOrderId,
			@RequestParam(name = "status", required = false) ReceiptStatus status,
			@RequestParam(name = "startDate", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(name = "endDate", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(name = "page", defaultValue = "0") int page) {
		Page<ReceiptListResponse> receipts = receiptService.getReceipts(purchaseOrderId, status, startDate, endDate,
				page);

		return ApiResponse.success(receipts.getContent(), PageMeta.from(receipts));
	}

	// ========== ORDERED 발주와 ACTIVE 창고를 선택하여 PENDING 입고를 등록하는 메서드 ==========
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ReceiptDetailResponse> createReceipt(@Valid @RequestBody ReceiptCreateRequest request,
			Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);

		return ApiResponse.success(receiptService.createReceipt(request, currentUser.getUserId()));
	}

	// ========== receiptId로 입고·발주·창고·검수 품목·LOT·처리 이력을 상세 조회하는 메서드 ==========
	@GetMapping("/{receiptId}")
	public ApiResponse<ReceiptDetailResponse> getReceipt(@PathVariable(name = "receiptId") Long receiptId) {
		return ApiResponse.success(receiptService.getReceipt(receiptId));
	}

	// ========== PENDING 입고의 반영 창고를 다른 ACTIVE 창고로 변경하는 메서드 ==========
	@PatchMapping("/{receiptId}")
	public ApiResponse<ReceiptDetailResponse> updateWarehouse(@PathVariable(name = "receiptId") Long receiptId,
			@Valid @RequestBody ReceiptWarehouseUpdateRequest request) {
		return ApiResponse.success(receiptService.updateWarehouse(receiptId, request));
	}

	// ========== PENDING 입고를 INSPECTING 상태로 변경하고 검수 시작 이력을 저장하는 메서드 ==========
	@PostMapping("/{receiptId}/start-inspection")
	public ApiResponse<ReceiptDetailResponse> startInspection(@PathVariable(name = "receiptId") Long receiptId,
			@Valid @RequestBody ReceiptVersionRequest request, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);

		return ApiResponse.success(
				receiptService.startInspection(receiptId, request.version(), currentUser.getUserId()));
	}

	// ========== INSPECTING 입고의 품목별 수량·메모·LOT 검수 결과를 전체 교체 저장하는 메서드 ==========
	@PutMapping("/{receiptId}/inspection")
	public ApiResponse<ReceiptDetailResponse> saveInspection(@PathVariable(name = "receiptId") Long receiptId,
			@Valid @RequestBody ReceiptInspectionRequest request) {
		return ApiResponse.success(receiptService.saveInspection(receiptId, request));
	}

	// ========== 검수 결과를 재고·변동 이력·매입 전표·발주 누적 수량과 함께 완료 처리하는 메서드 ==========
	@PostMapping("/{receiptId}/complete")
	public ApiResponse<ReceiptCompleteResponse> completeReceipt(@PathVariable(name = "receiptId") Long receiptId,
			@Valid @RequestBody ReceiptCompleteRequest request, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);

		return ApiResponse.success(
				receiptService.completeReceipt(receiptId, request, currentUser.getUserId()));
	}

	// ========== PENDING 또는 INSPECTING 입고를 사유와 함께 CANCELED 상태로 변경하는 메서드 ==========
	@PostMapping("/{receiptId}/cancel")
	public ApiResponse<ReceiptDetailResponse> cancelReceipt(@PathVariable(name = "receiptId") Long receiptId,
			@Valid @RequestBody ReceiptCancelRequest request, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);

		return ApiResponse.success(receiptService.cancelReceipt(receiptId, request, currentUser.getUserId()));
	}

	// ========== 인증 객체에서 현재 ERP 사용자 상세정보를 반환하는 메서드 ==========
	private AppUserDetails getCurrentUser(Authentication authentication) {
		return (AppUserDetails) authentication.getPrincipal();
	}
}
