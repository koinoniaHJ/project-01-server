package com.erp.server.master.warehouse.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.server.common.response.ApiResponse;
import com.erp.server.common.response.PageMeta;
import com.erp.server.common.security.AppUserDetails;
import com.erp.server.master.warehouse.dto.WarehouseItemListResponse;
import com.erp.server.master.warehouse.dto.WarehouseItemUpdateRequest;
import com.erp.server.master.warehouse.service.WarehouseItemService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

// ********** 안전재고 REST 요청을 받아 조회 조건·입력값과 현재 사용자 정보를 WarehouseItemService에 전달하기 위한 Controller 클래스 **********
@RestController
@RequestMapping("/api/v1/warehouse-items")
@RequiredArgsConstructor
@Validated
public class WarehouseItemController {

	private final WarehouseItemService warehouseItemService;

	// ========== 창고·품목·안전재고 미달 조건으로 안전재고 목록을 조회하는 메서드 ==========
	// 목록은 클라이언트 정렬 파라미터를 받지 않고 첫 페이지부터 20건씩 창고 코드·품목 코드 오름차순으로 조회한다.
	@GetMapping
	public ApiResponse<List<WarehouseItemListResponse>> getWarehouseItems(
			@RequestParam(name = "warehouseId", required = false) @Positive Long warehouseId,
			@RequestParam(name = "itemId", required = false) @Positive Long itemId,
			@RequestParam(name = "belowSafetyStock", required = false) Boolean belowSafetyStock,
			@RequestParam(name = "page", defaultValue = "0") @Min(0) int page) {

		Page<WarehouseItemListResponse> warehouseItems = warehouseItemService.getWarehouseItems(warehouseId, itemId,
				belowSafetyStock, page);

		return ApiResponse.success(warehouseItems.getContent(), PageMeta.from(warehouseItems));
	}

	// ========== 창고·품목 조합의 안전재고를 최초 등록하거나 기존 값을 변경하는 메서드 ==========
	// version이 없으면 신규 등록하고, 기존 행은 조회 당시 version을 검증하여 같은 PUT 경로에서 변경한다.
	@PutMapping("/{warehouseId}/{itemId}")
	public ApiResponse<WarehouseItemListResponse> saveSafetyStock(
			@PathVariable(name = "warehouseId") @Positive Long warehouseId,
			@PathVariable(name = "itemId") @Positive Long itemId,
			@Valid @RequestBody WarehouseItemUpdateRequest request, Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(
				warehouseItemService.saveSafetyStock(warehouseId, itemId, request, currentUser.getUserId()));
	}
}
