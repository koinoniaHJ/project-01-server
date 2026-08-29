package com.erp.server.master.supplier.controller;

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
import com.erp.server.master.supplier.dto.SupplierCreateRequest;
import com.erp.server.master.supplier.dto.SupplierDetailResponse;
import com.erp.server.master.supplier.dto.SupplierListResponse;
import com.erp.server.master.supplier.dto.SupplierStatusRequest;
import com.erp.server.master.supplier.dto.SupplierUpdateRequest;
import com.erp.server.master.supplier.service.SupplierService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// ********** 공급업체 REST 요청을 받아 입력값과 현재 사용자 정보를 SupplierService에 전달하기 위한 Controller 클래스 **********
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

	private final SupplierService supplierService;

	// ========== 키워드·사용 상태 조건을 적용하여 공급업체 목록을 조회하는 메서드 ==========
	// keyword와 status는 선택 조건이며, itemId 조건은 ITEM·SUPPLIER_ITEM 구현 후 추가한다.
	@GetMapping
	public ApiResponse<List<SupplierListResponse>> getSuppliers(
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "status", required = false) MasterStatus status,
			@PageableDefault(size = 20, sort = "supplierId", direction = Sort.Direction.DESC) Pageable pageable) {

		Page<SupplierListResponse> suppliers = supplierService.getSuppliers(keyword, status, pageable);

		return ApiResponse.success(suppliers.getContent(), PageMeta.from(suppliers));
	}

	// ========== 신규 공급업체를 등록하는 메서드 ==========
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SupplierDetailResponse> createSupplier(@Valid @RequestBody SupplierCreateRequest request,
			Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(supplierService.createSupplier(request, currentUser.getUserId()));
	}

	// ========== supplierId로 공급업체 상세정보를 조회하는 메서드 ==========
	// WAREHOUSE에는 Service 응답 변환 과정에서 공급업체 메모가 null로 반환된다.
	// 존재하지 않는 supplierId는 404 RESOURCE_NOT_FOUND를 반환한다.
	@GetMapping("/{supplierId}")
	public ApiResponse<SupplierDetailResponse> getSupplier(@PathVariable(name = "supplierId") Long supplierId,
			Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(supplierService.getSupplier(supplierId, currentUser.getRole()));
	}

	// ========== 공급업체 기본정보와 주소를 수정하는 메서드 ==========
	@PatchMapping("/{supplierId}")
	public ApiResponse<SupplierDetailResponse> updateSupplier(@PathVariable(name = "supplierId") Long supplierId,
			@Valid @RequestBody SupplierUpdateRequest request, Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(supplierService.updateSupplier(supplierId, request, currentUser.getUserId()));
	}

	// ========== 공급업체 ACTIVE·INACTIVE 사용 상태를 변경하는 메서드 ==========
	@PostMapping("/{supplierId}/status")
	public ApiResponse<SupplierDetailResponse> changeStatus(@PathVariable(name = "supplierId") Long supplierId,
			@Valid @RequestBody SupplierStatusRequest request, Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(supplierService.changeStatus(supplierId, request, currentUser.getUserId()));
	}
}
