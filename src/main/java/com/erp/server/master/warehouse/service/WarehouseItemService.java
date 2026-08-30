package com.erp.server.master.warehouse.service;

import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.repository.AppUserRepository;
import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.item.domain.Item;
import com.erp.server.master.item.repository.ItemRepository;
import com.erp.server.master.warehouse.domain.Warehouse;
import com.erp.server.master.warehouse.domain.WarehouseItem;
import com.erp.server.master.warehouse.dto.WarehouseItemListResponse;
import com.erp.server.master.warehouse.dto.WarehouseItemUpdateRequest;
import com.erp.server.master.warehouse.repository.WarehouseItemQueryRepository;
import com.erp.server.master.warehouse.repository.WarehouseItemRepository;
import com.erp.server.master.warehouse.repository.WarehouseRepository;

import lombok.RequiredArgsConstructor;

// ********** 창고·품목별 안전재고 조회와 최초 등록·기존 변경 및 동시성 규칙을 처리하기 위한 Service 클래스 **********
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseItemService {

	private final AppUserRepository appUserRepository;
	private final WarehouseRepository warehouseRepository;
	private final ItemRepository itemRepository;
	private final WarehouseItemRepository warehouseItemRepository;
	private final WarehouseItemQueryRepository warehouseItemQueryRepository;

	// ========== 창고·품목·미달 여부 조건을 적용하여 안전재고 목록을 페이지 조회하는 메서드 ==========
	// 조회 Repository에서 사용 중인 창고와 품목의 미등록 조합까지 포함하고 가용재고·부족 수량을 함께 계산한다.
	public Page<WarehouseItemListResponse> getWarehouseItems(Long warehouseId, Long itemId,
			Boolean belowSafetyStock, int page) {

		return warehouseItemQueryRepository.findAllByFilters(warehouseId, itemId, belowSafetyStock,
				PageRequest.of(page, 20));
	}

	// ========== version 유무에 따라 창고·품목별 안전재고를 최초 등록하거나 기존 값을 변경하는 메서드 ==========
	@Transactional
	public WarehouseItemListResponse saveSafetyStock(Long warehouseId, Long itemId,
			WarehouseItemUpdateRequest request, Long currentUserId) {

		// 창고와 품목을 같은 순서로 잠가 상태 변경과 안전재고 저장이 동시에 진행되지 않게 한다.
		Warehouse warehouse = findWarehouseForUpdate(warehouseId);
		Item item = findItemForUpdate(itemId);

		validateWarehouseIsActive(warehouse);
		validateItemIsActive(item);

		WarehouseItem warehouseItem = warehouseItemRepository
				.findByWarehouseWarehouseIdAndItemItemId(warehouseId, itemId)
				.orElse(null);
		AppUser currentUser = findUser(currentUserId);

		if (warehouseItem == null) {
			validateNewSafetyStockVersion(request.version());
			createSafetyStock(warehouse, item, request, currentUser);

		} else {
			validateVersion(warehouseItem, request.version());
			updateSafetyStock(warehouseItem, request, currentUser);
		}

		return findWarehouseItemResponse(warehouseId, itemId);
	}

	// ========== version이 없는 창고·품목 조합의 WAREHOUSE_ITEM을 최초 생성하는 메서드 ==========
	private void createSafetyStock(Warehouse warehouse, Item item, WarehouseItemUpdateRequest request,
			AppUser currentUser) {

		WarehouseItem warehouseItem = WarehouseItem.create(warehouse, item, request.safetyStockQuantity(), currentUser);

		try {
			warehouseItemRepository.saveAndFlush(warehouseItem);

		} catch (DataIntegrityViolationException exception) {
			// 같은 창고·품목 조합이 먼저 등록된 경우 UNIQUE 제약조건 충돌을 최신 정보 재조회 안내로 변환한다.
			throw createVersionConflictException();
		}
	}

	// ========== 최신 version이 확인된 기존 WAREHOUSE_ITEM의 안전재고 수량을 변경하는 메서드 ==========
	private void updateSafetyStock(WarehouseItem warehouseItem, WarehouseItemUpdateRequest request,
			AppUser currentUser) {

		warehouseItem.updateSafetyStockQuantity(request.safetyStockQuantity(), currentUser);

		try {
			// 실제 UPDATE SQL을 즉시 실행하여 마지막 저장 순간에 발생할 수 있는 낙관적 잠금 충돌을 확인한다.
			warehouseItemRepository.flush();

		} catch (ObjectOptimisticLockingFailureException exception) {
			throw createVersionConflictException();
		}
	}

	// ========== 저장 직후 해당 창고·품목의 가용재고와 최신 안전재고 정보를 다시 조회하는 메서드 ==========
	private WarehouseItemListResponse findWarehouseItemResponse(Long warehouseId, Long itemId) {

		return warehouseItemQueryRepository.findAllByFilters(warehouseId, itemId, null, PageRequest.of(0, 1))
				.getContent().stream().findFirst()
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
						"안전재고 정보를 찾을 수 없습니다."));
	}

	// ========== 안전재고 저장 중 상태 변경을 막기 위해 warehouseId로 창고를 비관적 잠금 조회하는 메서드 ==========
	private Warehouse findWarehouseForUpdate(Long warehouseId) {

		return warehouseRepository.findByIdForUpdate(warehouseId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "창고를 찾을 수 없습니다."));
	}

	// ========== 안전재고 저장 중 상태 변경을 막기 위해 itemId로 품목을 비관적 잠금 조회하는 메서드 ==========
	private Item findItemForUpdate(Long itemId) {

		return itemRepository.findByIdForUpdate(itemId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "품목을 찾을 수 없습니다."));
	}

	// ========== userId로 현재 사용자를 조회하고 없으면 404 예외를 발생시키는 메서드 ==========
	private AppUser findUser(Long userId) {

		return appUserRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));
	}

	// ========== 안전재고 설정 대상 창고가 최신 ACTIVE 상태인지 검증하는 메서드 ==========
	private void validateWarehouseIsActive(Warehouse warehouse) {

		if (warehouse.getStatus() != MasterStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.CONFLICT, "사용 중인 창고에만 안전재고를 설정할 수 있습니다.");
		}
	}

	// ========== 안전재고 설정 대상 품목이 최신 ACTIVE 상태인지 검증하는 메서드 ==========
	private void validateItemIsActive(Item item) {

		if (item.getStatus() != MasterStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.CONFLICT, "사용 중인 품목에만 안전재고를 설정할 수 있습니다.");
		}
	}

	// ========== 미등록 조합의 최초 저장 요청에 기존 행의 version이 잘못 전달되지 않았는지 검증하는 메서드 ==========
	private void validateNewSafetyStockVersion(Long requestVersion) {

		if (requestVersion != null) {
			throw createVersionConflictException();
		}
	}

	// ========== 기존 안전재고의 요청 version과 현재 WAREHOUSE_ITEM version이 같은지 검증하는 메서드 ==========
	private void validateVersion(WarehouseItem warehouseItem, Long requestVersion) {

		if (!Objects.equals(warehouseItem.getVersion(), requestVersion)) {
			throw createVersionConflictException();
		}
	}

	// ========== 최초 등록·기존 변경의 동시성 충돌에 사용할 공통 409 업무 예외를 생성하는 메서드 ==========
	private BusinessException createVersionConflictException() {

		return new BusinessException(ErrorCode.CONFLICT,
				"다른 사용자가 먼저 안전재고를 등록하거나 변경했습니다. 최신 안전재고 정보를 다시 조회해 주세요.");
	}
}
