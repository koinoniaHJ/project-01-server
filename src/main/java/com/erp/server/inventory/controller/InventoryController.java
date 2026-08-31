package com.erp.server.inventory.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.server.common.response.ApiResponse;
import com.erp.server.inventory.domain.InventoryLotStatus;
import com.erp.server.inventory.dto.InventoryLotListResponse;
import com.erp.server.inventory.service.InventoryService;

import lombok.RequiredArgsConstructor;

// ********** 재고 LOT 조회 요청을 받아 공통 재고 Service에 전달하기 위한 Controller 클래스 **********
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

	private final InventoryService inventoryService;

	// ========== 창고·품목·LOT 상태·사용기한 조건으로 LOT별 재고와 출고 가능 수량을 조회하는 메서드 ==========
	@GetMapping("/lots")
	public ApiResponse<List<InventoryLotListResponse>> getInventoryLots(
			@RequestParam(name = "warehouseId", required = false) Long warehouseId,
			@RequestParam(name = "itemId", required = false) Long itemId,
			@RequestParam(name = "status", required = false) InventoryLotStatus status,
			@RequestParam(name = "expiry", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiry) {
		return ApiResponse.success(inventoryService.getInventoryLots(warehouseId, itemId, status, expiry));
	}
}
