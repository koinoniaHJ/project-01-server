package com.erp.server.master.warehouse.controller;

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
import com.erp.server.master.warehouse.dto.WarehouseCreateRequest;
import com.erp.server.master.warehouse.dto.WarehouseDetailResponse;
import com.erp.server.master.warehouse.dto.WarehouseListResponse;
import com.erp.server.master.warehouse.dto.WarehouseStatusRequest;
import com.erp.server.master.warehouse.dto.WarehouseUpdateRequest;
import com.erp.server.master.warehouse.service.WarehouseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// ********** 창고 REST 요청을 받아 입력값과 현재 사용자 정보를 WarehouseService에 전달하기 위한 Controller 클래스 **********
@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

	private final WarehouseService warehouseService;

	// ========== 키워드·사용 상태 조건을 적용하여 창고 목록을 조회하는 메서드 ==========
	// keyword와 status는 선택 조건이며 별도 정렬 요청이 없으면 창고 코드 오름차순으로 조회한다.
	@GetMapping
	public ApiResponse<List<WarehouseListResponse>> getWarehouses(
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "status", required = false) MasterStatus status,
			@PageableDefault(size = 20, sort = "warehouseCode", direction = Sort.Direction.ASC) Pageable pageable) {

		Page<WarehouseListResponse> warehouses = warehouseService.getWarehouses(keyword, status, pageable);

		return ApiResponse.success(warehouses.getContent(), PageMeta.from(warehouses));
	}

	// ========== 신규 창고를 등록하는 메서드 ==========
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<WarehouseDetailResponse> createWarehouse(@Valid @RequestBody WarehouseCreateRequest request,
			Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(warehouseService.createWarehouse(request, currentUser.getUserId()));
	}

	// ========== warehouseId로 창고 상세정보를 조회하는 메서드 ==========
	// 존재하지 않는 warehouseId는 404 RESOURCE_NOT_FOUND를 반환한다.
	@GetMapping("/{warehouseId}")
	public ApiResponse<WarehouseDetailResponse> getWarehouse(
			@PathVariable(name = "warehouseId") Long warehouseId) {

		return ApiResponse.success(warehouseService.getWarehouse(warehouseId));
	}

	// ========== 창고 기본정보와 주소를 수정하는 메서드 ==========
	@PatchMapping("/{warehouseId}")
	public ApiResponse<WarehouseDetailResponse> updateWarehouse(
			@PathVariable(name = "warehouseId") Long warehouseId,
			@Valid @RequestBody WarehouseUpdateRequest request, Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(
				warehouseService.updateWarehouse(warehouseId, request, currentUser.getUserId()));
	}

	// ========== 창고 ACTIVE·INACTIVE 사용 상태를 변경하는 메서드 ==========
	@PostMapping("/{warehouseId}/status")
	public ApiResponse<WarehouseDetailResponse> changeStatus(
			@PathVariable(name = "warehouseId") Long warehouseId,
			@Valid @RequestBody WarehouseStatusRequest request, Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(warehouseService.changeStatus(warehouseId, request, currentUser.getUserId()));
	}
}
