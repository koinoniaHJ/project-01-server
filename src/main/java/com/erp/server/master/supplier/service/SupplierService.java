package com.erp.server.master.supplier.service;

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
import com.erp.server.master.supplier.domain.Supplier;
import com.erp.server.master.supplier.dto.SupplierCreateRequest;
import com.erp.server.master.supplier.dto.SupplierDetailResponse;
import com.erp.server.master.supplier.dto.SupplierListResponse;
import com.erp.server.master.supplier.dto.SupplierStatusRequest;
import com.erp.server.master.supplier.dto.SupplierUpdateRequest;
import com.erp.server.master.supplier.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

// ********** 공급업체 목록 조회와 공급업체 관련 업무 규칙을 처리하기 위한 Service 클래스 **********
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierService {

	private final AppUserRepository appUserRepository;
	private final SupplierRepository supplierRepository;

	// ========== 키워드·사용 상태 조건을 적용하여 공급업체 목록을 페이지 조회하는 메서드 ==========
	public Page<SupplierListResponse> getSuppliers(String keyword, MasterStatus status, Pageable pageable) {

		String normalizedKeyword = normalizeKeyword(keyword);
		String phoneKeyword = extractPhoneKeyword(normalizedKeyword);

		return supplierRepository.findAllByFilters(normalizedKeyword, phoneKeyword, status, pageable)
				.map(SupplierListResponse::from);
	}

	// ========== 신규 공급업체를 등록하는 메서드 ==========
	@Transactional
	public SupplierDetailResponse createSupplier(SupplierCreateRequest request, Long currentUserId) {

		AppUser currentUser = findUser(currentUserId);
		String supplierCode = supplierRepository.generateSupplierCode();

		// 사용 상태는 ACTIVE로 Entity에서 설정하고 createdBy와 updatedBy에는 현재 로그인 사용자를 저장한다.
		Supplier supplier = Supplier.create(supplierCode, // SEQ_SUPPLIER_CODE로 자동 생성
				request.supplierName(), request.phone(), request.email(), request.postalCode(), request.address(),
				request.addressDetail(), request.memo(), currentUser);

		Supplier savedSupplier = supplierRepository.saveAndFlush(supplier);

		return SupplierDetailResponse.from(savedSupplier, currentUser.getRole());
	}

	// ========== 공급업체 기본정보와 주소를 수정하는 메서드 ==========
	@Transactional
	public SupplierDetailResponse updateSupplier(Long supplierId, SupplierUpdateRequest request, Long currentUserId) {

		Supplier supplier = findSupplier(supplierId);

		// 사용자가 수정 화면을 열어둔 사이 다른 사용자가 이미 저장했는지 확인한다.
		validateVersion(supplier, request.version());

		AppUser currentUser = findUser(currentUserId);

		// DB가 아니라 영속성 Context의 Supplier Entity를 변경하고 트랜잭션 안에서 UPDATE SQL을 실행한다.
		supplier.update(request.supplierName(), request.phone(), request.email(), request.postalCode(),
				request.address(), request.addressDetail(), request.memo(), currentUser);

		// 실제 DB UPDATE를 즉시 실행하여 마지막 저장 순간에 발생할 수 있는 동시 수정 충돌을 확인한다.
		flushSupplierChanges();

		return SupplierDetailResponse.from(supplier, currentUser.getRole());
	}

	// ========== 진행 업무 참조 조건을 검증하고 공급업체 사용 상태를 변경하는 메서드 ==========
	@Transactional
	public SupplierDetailResponse changeStatus(Long supplierId, SupplierStatusRequest request, Long currentUserId) {

		Supplier supplier = findSupplierForUpdate(supplierId);

		validateVersion(supplier, request.version());
		validateSupplierCanBeInactivated(supplier, request.status());

		AppUser currentUser = findUser(currentUserId);

		supplier.changeStatus(request.status(), currentUser);

		flushSupplierChanges();

		return SupplierDetailResponse.from(supplier, currentUser.getRole());
	}

	// ========== supplierId로 공급업체 상세정보를 조회하는 메서드 ==========
	public SupplierDetailResponse getSupplier(Long supplierId, UserRole currentUserRole) {

		Supplier supplier = findSupplier(supplierId);

		return SupplierDetailResponse.from(supplier, currentUserRole);
	}

	// ========== supplierId로 공급업체를 조회하고 존재하지 않으면 예외를 발생시키는 메서드 ==========
	private Supplier findSupplier(Long supplierId) {

		return supplierRepository.findById(supplierId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "공급업체를 찾을 수 없습니다."));
	}

	// ========== userId로 현재 사용자를 조회하고 없으면 404 오류를 발생시키는 메서드 ==========
	private AppUser findUser(Long userId) {

		return appUserRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));
	}

	// ========== 요청 version과 현재 공급업체 Entity version이 같은지 검증하는 메서드 ==========
	private void validateVersion(Supplier supplier, Long requestVersion) {

		if (!Objects.equals(supplier.getVersion(), requestVersion)) {
			throw new BusinessException(ErrorCode.CONFLICT, "다른 사용자가 먼저 수정했습니다. 최신 공급업체 정보를 다시 조회해 주세요.");
		}
	}

	// ========== ACTIVE 공급업체를 INACTIVE로 변경할 때 진행 업무 참조가 없는지 검증하는 메서드 ==========
	private void validateSupplierCanBeInactivated(Supplier supplier, MasterStatus nextStatus) {

		boolean inactivationRequested = supplier.getStatus() == MasterStatus.ACTIVE
				&& nextStatus == MasterStatus.INACTIVE;

		if (!inactivationRequested) {
			return;
		}

		long ongoingReferenceCount = supplierRepository.countOngoingBusinessReferences(supplier.getSupplierId());

		if (ongoingReferenceCount > 0) {
			throw new BusinessException(ErrorCode.CONFLICT,
					"진행 중인 발주·입고·매입 반품 또는 미결 매입 전표가 있어 공급업체를 사용 중지할 수 없습니다.");
		}
	}

	// ========== 공급업체 UPDATE를 즉시 실행하여 최종 낙관적 잠금 충돌을 확인하는 메서드 ==========
	private void flushSupplierChanges() {

		try {
			supplierRepository.flush();

		} catch (ObjectOptimisticLockingFailureException exception) {
			throw new BusinessException(ErrorCode.CONFLICT, "다른 사용자가 먼저 수정했습니다. 최신 공급업체 정보를 다시 조회해 주세요.");
		}
	}

	// ========== 상태 변경 중 동시 처리를 막기 위해 supplierId로 공급업체를 비관적 잠금 조회하는 메서드 ==========
	private Supplier findSupplierForUpdate(Long supplierId) {

		return supplierRepository.findByIdForUpdate(supplierId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "공급업체를 찾을 수 없습니다."));
	}

	// ========== 키워드의 앞뒤 공백을 제거하고 검색값이 없으면 null로 변환하는 메서드 ==========
	private String normalizeKeyword(String keyword) {

		if (keyword == null) {
			return null;
		}

		String normalizedKeyword = keyword.trim();

		return normalizedKeyword.isEmpty() ? null : normalizedKeyword;
	}

	// ========== 대표 연락처 검색을 위해 전화번호 형식의 키워드에서 숫자만 추출하는 메서드 ==========
	private String extractPhoneKeyword(String keyword) {

		if (keyword == null) {
			return null;
		}

		// 공급업체 코드·공급업체명·발주 이메일에 포함된 숫자가 연락처 검색에 사용되지 않도록 문자가 있으면 제외한다.
		boolean containsLetter = keyword.codePoints().anyMatch(Character::isLetter);

		if (containsLetter) {
			return null;
		}

		String phoneKeyword = keyword.replaceAll("[^0-9]", "");

		return phoneKeyword.isEmpty() ? null : phoneKeyword;
	}
}
