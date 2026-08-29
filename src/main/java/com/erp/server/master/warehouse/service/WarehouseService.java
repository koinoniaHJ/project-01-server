package com.erp.server.master.warehouse.service;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.repository.AppUserRepository;
import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.warehouse.domain.Warehouse;
import com.erp.server.master.warehouse.dto.WarehouseCreateRequest;
import com.erp.server.master.warehouse.dto.WarehouseDetailResponse;
import com.erp.server.master.warehouse.dto.WarehouseListResponse;
import com.erp.server.master.warehouse.dto.WarehouseStatusRequest;
import com.erp.server.master.warehouse.dto.WarehouseUpdateRequest;
import com.erp.server.master.warehouse.repository.WarehouseRepository;

import lombok.RequiredArgsConstructor;

// ********** 창고 목록 조건 조회와 창고 관련 업무 규칙을 처리하기 위한 Service 클래스 **********
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService {

	private final AppUserRepository appUserRepository;
	private final WarehouseRepository warehouseRepository;

	// ========== 키워드·사용 상태 조건을 적용하여 창고 목록을 페이지 조회하는 메서드 ==========
	public Page<WarehouseListResponse> getWarehouses(String keyword, MasterStatus status, Pageable pageable) {

		String normalizedKeyword = normalizeKeyword(keyword);

		return warehouseRepository.findAllByFilters(normalizedKeyword, status, pageable)
				.map(WarehouseListResponse::from);
	}

	// ========== 신규 창고를 등록하는 메서드 ==========
	@Transactional
	public WarehouseDetailResponse createWarehouse(WarehouseCreateRequest request, Long currentUserId) {

		AppUser currentUser = findUser(currentUserId);
		String warehouseCode = warehouseRepository.generateWarehouseCode();

		// 사용 상태는 ACTIVE로 Entity에서 설정하고 createdBy와 updatedBy에는 현재 로그인 사용자를 저장한다.
		Warehouse warehouse = Warehouse.create(warehouseCode, // SEQ_WAREHOUSE_CODE로 자동 생성
				request.warehouseName(), request.postalCode(), request.address(), request.addressDetail(), request.memo(),
				currentUser);

		Warehouse savedWarehouse = warehouseRepository.saveAndFlush(warehouse);

		return WarehouseDetailResponse.from(savedWarehouse);
	}

	// ========== 창고 기본정보와 주소를 수정하는 메서드 ==========
	@Transactional
	public WarehouseDetailResponse updateWarehouse(Long warehouseId, WarehouseUpdateRequest request,
			Long currentUserId) {

		Warehouse warehouse = findWarehouse(warehouseId);

		// 사용자가 수정 화면을 열어둔 사이 다른 사용자가 이미 저장했는지 확인한다.
		validateVersion(warehouse, request.version());

		AppUser currentUser = findUser(currentUserId);

		// 영속성 Context의 Warehouse Entity를 변경하고 트랜잭션 안에서 UPDATE SQL을 실행한다.
		warehouse.update(request.warehouseName(), request.postalCode(), request.address(), request.addressDetail(),
				request.memo(), currentUser);

		// 실제 DB UPDATE를 즉시 실행하여 마지막 저장 순간에 발생할 수 있는 동시 수정 충돌을 확인한다.
		flushWarehouseChanges();

		return WarehouseDetailResponse.from(warehouse);
	}

	// ========== 현재 재고·진행 업무 참조 조건을 검증하고 창고 사용 상태를 변경하는 메서드 ==========
	@Transactional
	public WarehouseDetailResponse changeStatus(Long warehouseId, WarehouseStatusRequest request,
			Long currentUserId) {

		Warehouse warehouse = findWarehouseForUpdate(warehouseId);

		validateVersion(warehouse, request.version());
		validateWarehouseCanBeInactivated(warehouse, request.status());

		AppUser currentUser = findUser(currentUserId);

		warehouse.changeStatus(request.status(), currentUser);

		flushWarehouseChanges();

		return WarehouseDetailResponse.from(warehouse);
	}

	// ========== warehouseId로 창고 상세정보를 조회하는 메서드 ==========
	public WarehouseDetailResponse getWarehouse(Long warehouseId) {

		Warehouse warehouse = findWarehouse(warehouseId);

		return WarehouseDetailResponse.from(warehouse);
	}

	// ========== warehouseId로 창고를 조회하고 존재하지 않으면 404 예외를 발생시키는 메서드 ==========
	private Warehouse findWarehouse(Long warehouseId) {

		return warehouseRepository.findById(warehouseId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "창고를 찾을 수 없습니다."));
	}

	// ========== userId로 현재 사용자를 조회하고 없으면 404 예외를 발생시키는 메서드 ==========
	private AppUser findUser(Long userId) {

		return appUserRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));
	}

	// ========== 요청 version과 현재 창고 Entity version이 같은지 검증하는 메서드 ==========
	private void validateVersion(Warehouse warehouse, Long requestVersion) {

		if (!Objects.equals(warehouse.getVersion(), requestVersion)) {
			throw new BusinessException(ErrorCode.CONFLICT, "다른 사용자가 먼저 수정했습니다. 최신 창고 정보를 다시 조회해 주세요.");
		}
	}

	// ========== ACTIVE 창고를 INACTIVE로 변경할 때 현재 재고와 진행 업무 참조가 없는지 검증하는 메서드 ==========
	private void validateWarehouseCanBeInactivated(Warehouse warehouse, MasterStatus nextStatus) {

		boolean inactivationRequested = warehouse.getStatus() == MasterStatus.ACTIVE
				&& nextStatus == MasterStatus.INACTIVE;

		if (!inactivationRequested) {
			return;
		}

		long referenceCount = warehouseRepository
				.countStockAndOngoingBusinessReferences(warehouse.getWarehouseId());

		if (referenceCount > 0) {
			throw new BusinessException(ErrorCode.CONFLICT,
					"현재 재고가 있거나 진행 중인 입고·매입 반품·출고·거래처 반품·재고 실사 또는 재고 조정이 있어 창고를 사용 중지할 수 없습니다.");
		}
	}

	// ========== 창고 UPDATE를 즉시 실행하여 최종 낙관적 잠금 충돌을 확인하는 메서드 ==========
	private void flushWarehouseChanges() {

		try {
			warehouseRepository.flush();

		} catch (ObjectOptimisticLockingFailureException exception) {
			throw new BusinessException(ErrorCode.CONFLICT, "다른 사용자가 먼저 수정했습니다. 최신 창고 정보를 다시 조회해 주세요.");
		}
	}

	// ========== 상태 변경 중 동시 처리를 막기 위해 warehouseId로 창고를 비관적 잠금 조회하는 메서드 ==========
	private Warehouse findWarehouseForUpdate(Long warehouseId) {

		return warehouseRepository.findByIdForUpdate(warehouseId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "창고를 찾을 수 없습니다."));
	}

	// ========== 검색어 앞뒤 공백을 제거하고 빈 문자열을 조회 조건에서 제외하는 메서드 ==========
	private String normalizeKeyword(String keyword) {

		if (keyword == null) {
			return null;
		}

		String normalizedKeyword = keyword.trim();

		return normalizedKeyword.isEmpty() ? null : normalizedKeyword;
	}
}
