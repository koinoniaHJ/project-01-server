package com.erp.server.purchase.returning.controller;

import java.util.List;

import org.springframework.data.domain.Page;
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
import com.erp.server.purchase.returning.domain.PurchaseReturnStatus;
import com.erp.server.purchase.returning.dto.PurchaseReturnCancelRequest;
import com.erp.server.purchase.returning.dto.PurchaseReturnCompleteResponse;
import com.erp.server.purchase.returning.dto.PurchaseReturnCreateRequest;
import com.erp.server.purchase.returning.dto.PurchaseReturnDetailResponse;
import com.erp.server.purchase.returning.dto.PurchaseReturnListResponse;
import com.erp.server.purchase.returning.dto.PurchaseReturnSourceResponse;
import com.erp.server.purchase.returning.dto.PurchaseReturnUpdateRequest;
import com.erp.server.purchase.returning.dto.PurchaseReturnVersionRequest;
import com.erp.server.purchase.returning.service.PurchaseReturnService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// ********** 매입 반품 REST 요청을 받아 목록·원본·상세·등록·수정·완료·취소 업무를 Service에 전달하기 위한 Controller 클래스 **********
@RestController
@RequestMapping("/api/v1/purchase-returns")
@RequiredArgsConstructor
public class PurchaseReturnController {

	private final PurchaseReturnService purchaseReturnService;

	// ========== 원본 입고와 반품 상태 조건으로 매입 반품 목록을 최신 등록 순서로 페이지 조회하는 메서드 ==========
	@GetMapping
	public ApiResponse<List<PurchaseReturnListResponse>> getPurchaseReturns(
			@RequestParam(name = "receiptId", required = false) Long receiptId,
			@RequestParam(name = "status", required = false) PurchaseReturnStatus status,
			@RequestParam(name = "page", defaultValue = "0") int page, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);
		Page<PurchaseReturnListResponse> purchaseReturns = purchaseReturnService.getPurchaseReturns(receiptId, status,
				page, currentUser.getRole());

		return ApiResponse.success(purchaseReturns.getContent(), PageMeta.from(purchaseReturns));
	}

	// ========== 완료 입고의 LOT별 원본 수량·기완료 반품·현재 재고·반품 가능 수량을 신규 등록 기준으로 조회하는 메서드 ==========
	@GetMapping("/source/{receiptId}")
	public ApiResponse<PurchaseReturnSourceResponse> getPurchaseReturnSource(
			@PathVariable(name = "receiptId") Long receiptId, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);

		return ApiResponse.success(purchaseReturnService.getPurchaseReturnSource(receiptId, currentUser.getRole()));
	}

	// ========== purchaseReturnId로 매입 반품·원본 입고·LOT별 수량·금액·처리 이력을 상세 조회하는 메서드 ==========
	@GetMapping("/{purchaseReturnId}")
	public ApiResponse<PurchaseReturnDetailResponse> getPurchaseReturn(
			@PathVariable(name = "purchaseReturnId") Long purchaseReturnId, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);

		return ApiResponse.success(purchaseReturnService.getPurchaseReturn(purchaseReturnId, currentUser.getRole()));
	}

	// ========== 완료 입고와 원본 LOT별 반품 수량·사유로 REGISTERED 매입 반품을 등록하는 메서드 ==========
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<PurchaseReturnDetailResponse> createPurchaseReturn(
			@Valid @RequestBody PurchaseReturnCreateRequest request, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);

		return ApiResponse.success(purchaseReturnService.createPurchaseReturn(request, currentUser.getUserId(),
				currentUser.getRole()));
	}

	// ========== REGISTERED 매입 반품의 LOT별 수량·사유를 version 검증 후 전체 교체하는 메서드 ==========
	@PatchMapping("/{purchaseReturnId}")
	public ApiResponse<PurchaseReturnDetailResponse> updatePurchaseReturn(
			@PathVariable(name = "purchaseReturnId") Long purchaseReturnId,
			@Valid @RequestBody PurchaseReturnUpdateRequest request, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);

		return ApiResponse.success(
				purchaseReturnService.updatePurchaseReturn(purchaseReturnId, request, currentUser.getRole()));
	}

	// ========== REGISTERED 매입 반품의 재고 감소·변동 이력·마이너스 매입 전표를 하나의 트랜잭션으로 완료하는 메서드 ==========
	@PostMapping("/{purchaseReturnId}/complete")
	public ApiResponse<PurchaseReturnCompleteResponse> completePurchaseReturn(
			@PathVariable(name = "purchaseReturnId") Long purchaseReturnId,
			@Valid @RequestBody PurchaseReturnVersionRequest request, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);

		return ApiResponse.success(purchaseReturnService.completePurchaseReturn(purchaseReturnId, request.version(),
				currentUser.getUserId()));
	}

	// ========== REGISTERED 매입 반품을 재고·전표 변경 없이 사유와 함께 CANCELED 상태로 변경하는 메서드 ==========
	@PostMapping("/{purchaseReturnId}/cancel")
	public ApiResponse<PurchaseReturnDetailResponse> cancelPurchaseReturn(
			@PathVariable(name = "purchaseReturnId") Long purchaseReturnId,
			@Valid @RequestBody PurchaseReturnCancelRequest request, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);
		return ApiResponse.success(purchaseReturnService.cancelPurchaseReturn(purchaseReturnId, request,
				currentUser.getUserId(), currentUser.getRole()));
	}

	// ========== 인증 객체에서 현재 ERP 사용자의 식별자와 역할을 가진 상세정보를 반환하는 메서드 ==========
	private AppUserDetails getCurrentUser(Authentication authentication) {
		return (AppUserDetails) authentication.getPrincipal();
	}
}
