package com.erp.server.master.item.service;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.domain.UserRole;
import com.erp.server.common.user.repository.AppUserRepository;
import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.item.domain.Item;
import com.erp.server.master.item.domain.SupplierItem;
import com.erp.server.master.item.dto.ItemCreateRequest;
import com.erp.server.master.item.dto.ItemDetailResponse;
import com.erp.server.master.item.dto.ItemListResponse;
import com.erp.server.master.item.dto.ItemStatusRequest;
import com.erp.server.master.item.dto.ItemUpdateRequest;
import com.erp.server.master.item.dto.SupplierItemRequest;
import com.erp.server.master.item.dto.SupplierItemResponse;
import com.erp.server.master.item.repository.ItemRepository;
import com.erp.server.master.item.repository.SupplierItemRepository;
import com.erp.server.master.supplier.domain.Supplier;
import com.erp.server.master.supplier.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

// ********** 품목 목록 조회와 품목 관련 업무 규칙을 처리하기 위한 Service 클래스 **********
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

	private final AppUserRepository appUserRepository;
	private final ItemRepository itemRepository;
	private final SupplierItemRepository supplierItemRepository;
	private final SupplierRepository supplierRepository;

	// ========== 키워드·사용 상태·취급 공급업체 조건을 적용하여 품목 목록을 페이지 조회하는 메서드 ==========
	public Page<ItemListResponse> getItems(String keyword, MasterStatus status, Long supplierId,
			UserRole currentUserRole, Pageable pageable) {
		String normalizedKeyword = normalizeKeyword(keyword);

		// 현재 로그인 사용자의 역할을 응답 변환에 전달하여 WAREHOUSE 사용자에게 기본 판매가격을 노출하지 않는다.
		return itemRepository.findAllByFilters(normalizedKeyword, status, supplierId, pageable)
				.map(item -> ItemListResponse.from(item, currentUserRole));
	}

	// ========== 신규 품목을 등록하는 메서드 ==========
	@Transactional
	public ItemDetailResponse createItem(ItemCreateRequest request, Long currentUserId) {
		AppUser currentUser = findUser(currentUserId);
		String itemCode = itemRepository.generateItemCode();

		// 사용 상태는 ACTIVE로 Entity에서 설정하고 createdBy와 updatedBy에는 현재 로그인 사용자를 저장한다.
		Item item = Item.create(itemCode, // SEQ_ITEM_CODE로 자동 생성
				request.itemName(), request.unit(), request.otherUnitName(), request.defaultSalesPrice(), request.memo(),
				currentUser);

		Item savedItem = itemRepository.saveAndFlush(item);

		// 품목 등록 단계에서는 취급 공급업체 관계를 별도 API로 등록하므로 빈 목록을 반환한다.
		return ItemDetailResponse.from(savedItem, List.of(), currentUser.getRole());
	}

	// ========== 품목 기본정보와 기본 판매가격을 수정하는 메서드 ==========
	@Transactional
	public ItemDetailResponse updateItem(Long itemId, ItemUpdateRequest request, Long currentUserId) {
		Item item = findItem(itemId);

		// 사용자가 수정 화면을 열어둔 사이 다른 사용자가 이미 저장했는지 확인한다.
		validateVersion(item, request.version());

		AppUser currentUser = findUser(currentUserId);

		// 영속성 Context의 Item Entity를 변경하고 트랜잭션 안에서 UPDATE SQL을 실행한다.
		item.update(request.itemName(), request.unit(), request.otherUnitName(), request.defaultSalesPrice(),
				request.memo(), currentUser);

		// 실제 DB UPDATE를 즉시 실행하여 마지막 저장 순간에 발생할 수 있는 동시 수정 충돌을 확인한다.
		flushItemChanges();

		return ItemDetailResponse.from(item, supplierItemRepository.findAllByItemId(itemId), currentUser.getRole());
	}

	// ========== 품목에 ACTIVE 공급업체의 취급 관계를 등록하는 메서드 ==========
	@Transactional
	public SupplierItemResponse addSupplier(Long itemId, SupplierItemRequest request, Long currentUserId) {
		// 취급 관계 등록은 품목의 사용 상태와 관계없이 허용하지만 저장된 품목인지는 확인한다.
		Item item = findItem(itemId);

		// 공급업체 상태 변경과 동시에 관계가 등록되지 않도록 잠근 후 최신 사용 상태를 검증한다.
		Supplier supplier = findSupplierForUpdate(request.supplierId());
		validateSupplierIsActive(supplier);

		if (supplierItemRepository.existsByItemItemIdAndSupplierSupplierId(itemId, supplier.getSupplierId())) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 등록된 취급 공급업체입니다.");
		}

		AppUser currentUser = findUser(currentUserId);
		SupplierItem supplierItem = SupplierItem.create(supplier, item, currentUser);

		return SupplierItemResponse.from(supplierItemRepository.saveAndFlush(supplierItem));
	}

	// ========== 품목에 등록된 취급 공급업체 관계를 해제하는 메서드 ==========
	@Transactional
	public void removeSupplier(Long itemId, Long supplierId) {
		// 존재하지 않는 품목 경로의 관계 해제 요청을 품목 404 오류로 구분한다.
		findItem(itemId);

		SupplierItem supplierItem = supplierItemRepository
				.findByItemItemIdAndSupplierSupplierId(itemId, supplierId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
						"등록된 취급 공급업체 관계를 찾을 수 없습니다."));

		// SUPPLIER_ITEM 관계만 삭제하며 기존 발주·입고 이력과 공급업체·품목 정보는 유지한다.
		supplierItemRepository.delete(supplierItem);
		supplierItemRepository.flush();
	}

	// ========== 진행 업무 참조 조건을 검증하고 품목 사용 상태를 변경하는 메서드 ==========
	@Transactional
	public ItemDetailResponse changeStatus(Long itemId, ItemStatusRequest request, Long currentUserId) {
		Item item = findItemForUpdate(itemId);

		validateVersion(item, request.version());
		validateItemCanBeInactivated(item, request.status());

		AppUser currentUser = findUser(currentUserId);

		item.changeStatus(request.status(), currentUser);

		flushItemChanges();

		return ItemDetailResponse.from(item, supplierItemRepository.findAllByItemId(itemId), currentUser.getRole());
	}

	// ========== itemId로 품목 상세정보와 취급 공급업체 목록을 조회하는 메서드 ==========
	public ItemDetailResponse getItem(Long itemId, UserRole currentUserRole) {
		Item item = findItem(itemId);

		// 품목 상세 화면에 표시할 취급 공급업체를 공급업체 코드 오름차순으로 조회한다.
		return ItemDetailResponse.from(item, supplierItemRepository.findAllByItemId(itemId), currentUserRole);
	}

	// ========== itemId로 품목을 조회하고 존재하지 않으면 404 예외를 발생시키는 메서드 ==========
	private Item findItem(Long itemId) {
		return itemRepository.findById(itemId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "품목을 찾을 수 없습니다."));
	}

	// ========== userId로 현재 사용자를 조회하고 없으면 404 예외를 발생시키는 메서드 ==========
	private AppUser findUser(Long userId) {
		return appUserRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));
	}

	// ========== 취급 관계 등록 대상 공급업체가 ACTIVE 상태인지 검증하는 메서드 ==========
	private void validateSupplierIsActive(Supplier supplier) {
		if (supplier.getStatus() != MasterStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.CONFLICT, "사용 중인 공급업체만 취급 공급업체로 등록할 수 있습니다.");
		}
	}

	// ========== 요청 version과 현재 품목 Entity version이 같은지 검증하는 메서드 ==========
	private void validateVersion(Item item, Long requestVersion) {
		if (!Objects.equals(item.getVersion(), requestVersion)) {
			throw new BusinessException(ErrorCode.CONFLICT, "다른 사용자가 먼저 수정했습니다. 최신 품목 정보를 다시 조회해 주세요.");
		}
	}

	// ========== ACTIVE 품목을 INACTIVE로 변경할 때 진행 업무 참조가 없는지 검증하는 메서드 ==========
	private void validateItemCanBeInactivated(Item item, MasterStatus nextStatus) {
		boolean inactivationRequested = item.getStatus() == MasterStatus.ACTIVE
				&& nextStatus == MasterStatus.INACTIVE;

		if (!inactivationRequested) return;

		long ongoingReferenceCount = itemRepository.countOngoingBusinessReferences(item.getItemId());

		if (ongoingReferenceCount > 0) {
			throw new BusinessException(ErrorCode.CONFLICT,
					"진행 중인 발주·입고·반품·주문·재고 실사 또는 재고 조정이 있어 품목을 사용 중지할 수 없습니다.");
		}
	}

	// ========== 품목 UPDATE를 즉시 실행하여 최종 낙관적 잠금 충돌을 확인하는 메서드 ==========
	private void flushItemChanges() {
		try {
			itemRepository.flush();
		} catch (ObjectOptimisticLockingFailureException exception) {
			throw new BusinessException(ErrorCode.CONFLICT, "다른 사용자가 먼저 수정했습니다. 최신 품목 정보를 다시 조회해 주세요.");
		}
	}

	// ========== 상태 변경 중 동시 처리를 막기 위해 itemId로 품목을 비관적 잠금 조회하는 메서드 ==========
	private Item findItemForUpdate(Long itemId) {
		return itemRepository.findByIdForUpdate(itemId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "품목을 찾을 수 없습니다."));
	}

	// ========== 관계 등록 중 상태 변경과의 동시 처리를 막기 위해 supplierId로 공급업체를 비관적 잠금 조회하는 메서드 ==========
	private Supplier findSupplierForUpdate(Long supplierId) {
		return supplierRepository.findByIdForUpdate(supplierId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "공급업체를 찾을 수 없습니다."));
	}

	// ========== 검색어 앞뒤 공백을 제거하고 빈 문자열을 조회 조건에서 제외하는 메서드 ==========
	private String normalizeKeyword(String keyword) {
		if (keyword == null) return null;

		String normalizedKeyword = keyword.trim();
		return normalizedKeyword.isEmpty() ? null : normalizedKeyword;
	}
}
