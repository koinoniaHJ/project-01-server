package com.erp.server.master.item.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.item.dto.ItemCreateRequest;
import com.erp.server.master.item.dto.ItemDetailResponse;
import com.erp.server.master.item.dto.ItemListResponse;
import com.erp.server.master.item.dto.ItemStatusRequest;
import com.erp.server.master.item.dto.ItemUpdateRequest;
import com.erp.server.master.item.dto.SupplierItemRequest;
import com.erp.server.master.item.dto.SupplierItemResponse;
import com.erp.server.master.item.service.ItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// ********** 품목 REST 요청을 받아 입력값과 현재 사용자 정보를 ItemService에 전달하기 위한 Controller 클래스 **********
@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

	private final ItemService itemService;

	// ========== 키워드·사용 상태·취급 공급업체 조건을 적용하여 품목 목록을 조회하는 메서드 ==========
	// keyword, status, supplierId는 선택 조건이며 별도 정렬 요청이 없으면 품목 코드 오름차순으로 조회한다.
	@GetMapping
	public ApiResponse<List<ItemListResponse>> getItems(
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "status", required = false) MasterStatus status,
			@RequestParam(name = "supplierId", required = false) Long supplierId,
			@PageableDefault(size = 20, sort = "itemCode", direction = Sort.Direction.ASC) Pageable pageable,
			Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		Page<ItemListResponse> items = itemService.getItems(keyword, status, supplierId, currentUser.getRole(),
				pageable);

		return ApiResponse.success(items.getContent(), PageMeta.from(items));
	}

	// ========== 신규 품목을 등록하는 메서드 ==========
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ItemDetailResponse> createItem(@Valid @RequestBody ItemCreateRequest request,
			Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(itemService.createItem(request, currentUser.getUserId()));
	}

	// ========== 품목 기본정보와 기본 판매가격을 수정하는 메서드 ==========
	@PatchMapping("/{itemId}")
	public ApiResponse<ItemDetailResponse> updateItem(@PathVariable(name = "itemId") Long itemId,
			@Valid @RequestBody ItemUpdateRequest request, Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(itemService.updateItem(itemId, request, currentUser.getUserId()));
	}

	// ========== 품목 ACTIVE·INACTIVE 사용 상태를 변경하는 메서드 ==========
	@PostMapping("/{itemId}/status")
	public ApiResponse<ItemDetailResponse> changeStatus(@PathVariable(name = "itemId") Long itemId,
			@Valid @RequestBody ItemStatusRequest request, Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(itemService.changeStatus(itemId, request, currentUser.getUserId()));
	}

	// ========== 품목에 ACTIVE 공급업체의 취급 관계를 등록하는 메서드 ==========
	@PostMapping("/{itemId}/suppliers")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SupplierItemResponse> addSupplier(@PathVariable(name = "itemId") Long itemId,
			@Valid @RequestBody SupplierItemRequest request, Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(itemService.addSupplier(itemId, request, currentUser.getUserId()));
	}

	// ========== 품목에 등록된 취급 공급업체 관계를 해제하는 메서드 ==========
	@DeleteMapping("/{itemId}/suppliers/{supplierId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeSupplier(@PathVariable(name = "itemId") Long itemId,
			@PathVariable(name = "supplierId") Long supplierId) {

		itemService.removeSupplier(itemId, supplierId);
	}

	// ========== itemId로 품목 상세정보와 취급 공급업체 목록을 조회하는 메서드 ==========
	// WAREHOUSE에는 Service 응답 변환 과정에서 기본 판매가격이 null로 반환된다.
	// 존재하지 않는 itemId는 404 RESOURCE_NOT_FOUND를 반환한다.
	@GetMapping("/{itemId}")
	public ApiResponse<ItemDetailResponse> getItem(@PathVariable(name = "itemId") Long itemId,
			Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(itemService.getItem(itemId, currentUser.getRole()));
	}
}
