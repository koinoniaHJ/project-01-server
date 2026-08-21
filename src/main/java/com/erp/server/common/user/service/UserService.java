package com.erp.server.common.user.service;

import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.domain.UserRole;
import com.erp.server.common.user.domain.UserStatus;
import com.erp.server.common.user.dto.UserCreateRequest;
import com.erp.server.common.user.dto.UserPasswordResetRequest;
import com.erp.server.common.user.dto.UserResponse;
import com.erp.server.common.user.dto.UserStatusRequest;
import com.erp.server.common.user.dto.UserUpdateRequest;
import com.erp.server.common.user.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

// ********** 사용자 목록 조회·등록·수정·상태 변경·비밀번호 초기화와 마지막 활성 ADMIN 보호 규칙을 처리하기 위한 Service 클래스 **********
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;

	// ========== 상태와 역할 조건을 적용하여 사용자 목록을 페이지 조회하는 메서드 ==========
	public Page<UserResponse> getUsers(UserStatus status, UserRole role, Pageable pageable) {

		// Page.map()은 Page의 각 AppUser를 UserResponse로 변환하면서 페이지 정보는 유지한다.
		return appUserRepository.findAllByFilters(status, role, pageable).map(UserResponse::from);
	}

	// ========== 신규 사용자를 등록하는 메서드 ==========
	@Transactional
	public UserResponse createUser(UserCreateRequest request, Long currentUserId) {

		// 빠른 중복 안내를 위해 먼저 확인하고 DB UNIQUE 제약으로도 최종 차단한다.
		if (appUserRepository.existsByUsername(request.username())) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 로그인 아이디입니다.");
		}

		// 현재 로그인한 ADMIN을 created_by와 updated_by에 연결한다.
		AppUser currentUser = findUser(currentUserId);

		String passwordHash = passwordEncoder.encode(request.password());

		AppUser appUser = AppUser.create(request.username(), passwordHash, request.userName(), request.role(),
				currentUser);

		try {
			// saveAndFlush()로 INSERT를 즉시 실행하여 UNIQUE 충돌과 생성된 PK·version을 현재 요청에서 확인한다.
			AppUser savedUser = appUserRepository.saveAndFlush(appUser);

			return UserResponse.from(savedUser);

		} catch (DataIntegrityViolationException exception) {
			// 사전 조회 뒤 동시에 같은 아이디가 등록된 경우 DB UNIQUE 제약 위반을 409로 변환한다.
			throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 로그인 아이디입니다.");
		}
	}

	// ========== 사용자명과 역할을 수정하는 메서드 ==========
	@Transactional
	public UserResponse updateUser(Long userId, UserUpdateRequest request, Long currentUserId) {

		AppUser appUser = findUser(userId);

		validateVersion(appUser, request.version());

		// 현재 ACTIVE ADMIN을 ADMIN이 아니게 만드는 요청이면 마지막 관리자 여부를 확인한다.
		validateLastActiveAdmin(appUser, request.role(), appUser.getStatus());

		AppUser currentUser = findUser(currentUserId);

		appUser.update(request.userName(), request.role(), currentUser);

		flushUserChanges();

		return UserResponse.from(appUser);
	}

	// ========== 사용자 상태를 변경하는 메서드 ==========
	@Transactional
	public UserResponse changeStatus(Long userId, UserStatusRequest request, Long currentUserId) {

		AppUser appUser = findUser(userId);

		validateVersion(appUser, request.version());

		// 현재 ACTIVE ADMIN을 INACTIVE로 만드는 요청이면 마지막 관리자 여부를 확인한다.
		validateLastActiveAdmin(appUser, appUser.getRole(), request.status());

		AppUser currentUser = findUser(currentUserId);

		appUser.changeStatus(request.status(), currentUser);

		flushUserChanges();

		return UserResponse.from(appUser);
	}

	// ========== 사용자 비밀번호를 초기화하는 메서드 ==========
	@Transactional
	public UserResponse resetPassword(Long userId, UserPasswordResetRequest request, Long currentUserId) {

		AppUser appUser = findUser(userId);

		validateVersion(appUser, request.version());

		AppUser currentUser = findUser(currentUserId);

		String passwordHash = passwordEncoder.encode(request.newPassword());

		appUser.resetPassword(passwordHash, currentUser);

		flushUserChanges();

		return UserResponse.from(appUser);
	}

	// ========== 변경 후에도 활성 ADMIN이 한 명 이상 남는지 검증하는 메서드 ==========
	private void validateLastActiveAdmin(AppUser appUser, UserRole nextRole, UserStatus nextStatus) {

		// 현재 ACTIVE ADMIN이 변경 후 그 조건에서 벗어나는 요청인지 먼저 판단한다.
		boolean removesActiveAdmin = appUser.getRole() == UserRole.ADMIN && appUser.getStatus() == UserStatus.ACTIVE
				&& (nextRole != UserRole.ADMIN || nextStatus != UserStatus.ACTIVE);

		if (!removesActiveAdmin) {
			return;
		}

		// 활성 ADMIN 행을 잠근 상태로 조회하여 동시 강등·비활성화 요청도 순서대로 검사한다.
		int activeAdminCount = appUserRepository.findByRoleAndStatusForUpdate(UserRole.ADMIN, UserStatus.ACTIVE).size();

		if (activeAdminCount <= 1) {
			throw new BusinessException(ErrorCode.CONFLICT, "마지막 활성 관리자의 역할이나 상태는 변경할 수 없습니다.");
		}
	}

	// ========== userId로 사용자를 조회하고 없으면 404 오류를 발생시키는 메서드 ==========
	private AppUser findUser(Long userId) {

		return appUserRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));
	}

	// ========== 요청 version과 현재 Entity version이 같은지 검증하는 메서드 ==========
	private void validateVersion(AppUser appUser, Long requestVersion) {

		if (!Objects.equals(appUser.getVersion(), requestVersion)) {
			throw new BusinessException(ErrorCode.CONFLICT, "다른 사용자가 먼저 수정했습니다. 최신 사용자 정보를 다시 조회해 주세요.");
		}
	}

	// ========== UPDATE를 즉시 실행하여 최종 낙관적 잠금 충돌을 확인하는 메서드 ==========
	private void flushUserChanges() {

		try {
			appUserRepository.flush();

		} catch (ObjectOptimisticLockingFailureException exception) {
			throw new BusinessException(ErrorCode.CONFLICT, "다른 사용자가 먼저 수정했습니다. 최신 사용자 정보를 다시 조회해 주세요.");
		}
	}
}
