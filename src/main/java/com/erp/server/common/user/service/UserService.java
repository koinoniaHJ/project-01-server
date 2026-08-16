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

// 사용자 목록 조회·등록·수정·상태 변경·비밀번호 초기화 업무를 처리하기 위한 Service 클래스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    // 사용자 상태와 역할 조건을 적용하여 사용자 목록을 페이지 조회한다.
    public Page<UserResponse> getUsers(
            UserStatus status,
            UserRole role,
            Pageable pageable) {

        return appUserRepository
                .findAllByFilters(status, role, pageable)
                .map(UserResponse::from);
    }

    // 신규 사용자를 등록한다.
    @Transactional
    public UserResponse createUser(
            UserCreateRequest request,
            Long currentUserId) {

        // 로그인 아이디는 중복 등록할 수 없다.
        if (appUserRepository.existsByUsername(request.username())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "이미 사용 중인 로그인 아이디입니다.");
        }

        // 현재 로그인한 ADMIN 사용자를 등록 처리자로 사용한다.
        AppUser currentUser = findUser(currentUserId);

        // 평문 비밀번호는 DB에 저장하지 않고 BCrypt 해시값으로 변환한다.
        String passwordHash = passwordEncoder.encode(request.password());

        AppUser appUser = AppUser.create(
                request.username(),
                passwordHash,
                request.userName(),
                request.role(),
                currentUser
        );

        try {
            // DB UNIQUE 제약까지 즉시 확인하고 생성된 userId와 version을 응답에 반영한다.
            AppUser savedUser = appUserRepository.saveAndFlush(appUser);

            return UserResponse.from(savedUser);

        } catch (DataIntegrityViolationException exception) {

            // existsByUsername() 확인 이후 동시에 같은 아이디가 등록되는 경우 DB UNIQUE 제약에서 최종 차단한다.
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "이미 사용 중인 로그인 아이디입니다.");
        }
    }

    // 사용자명과 역할을 수정한다.
    @Transactional
    public UserResponse updateUser(
            Long userId,
            UserUpdateRequest request,
            Long currentUserId) {

        AppUser appUser = findUser(userId);

        // 화면에서 전달한 version이 현재 DB version과 같은지 확인한다.
        validateVersion(appUser, request.version());

        // 현재 로그인한 ADMIN 사용자를 최근 수정 사용자로 사용한다.
        AppUser currentUser = findUser(currentUserId);

        appUser.update(
                request.userName(),
                request.role(),
                currentUser
        );

        flushUserChanges();

        return UserResponse.from(appUser);
    }

    // 사용자 ACTIVE / INACTIVE 상태를 변경한다.
    @Transactional
    public UserResponse changeStatus(
            Long userId,
            UserStatusRequest request,
            Long currentUserId) {

        AppUser appUser = findUser(userId);

        // 화면에서 전달한 version이 현재 DB version과 같은지 확인한다.
        validateVersion(appUser, request.version());

        // 현재 로그인한 ADMIN 사용자를 최근 수정 사용자로 사용한다.
        AppUser currentUser = findUser(currentUserId);

        appUser.changeStatus(
                request.status(),
                currentUser
        );

        flushUserChanges();

        return UserResponse.from(appUser);
    }

    // 사용자 비밀번호를 초기화한다.
    @Transactional
    public UserResponse resetPassword(
            Long userId,
            UserPasswordResetRequest request,
            Long currentUserId) {

        AppUser appUser = findUser(userId);

        // 화면에서 전달한 version이 현재 DB version과 같은지 확인한다.
        validateVersion(appUser, request.version());

        // 현재 로그인한 ADMIN 사용자를 최근 수정 사용자로 사용한다.
        AppUser currentUser = findUser(currentUserId);

        // 새 평문 비밀번호는 DB에 저장하지 않고 BCrypt 해시값으로 변환한다.
        String passwordHash = passwordEncoder.encode(request.newPassword());

        appUser.resetPassword(
                passwordHash,
                currentUser
        );

        flushUserChanges();

        return UserResponse.from(appUser);
    }

    // userId로 사용자를 조회하고 없으면 404 오류로 처리한다.
    private AppUser findUser(Long userId) {

        return appUserRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "사용자를 찾을 수 없습니다."));
    }

    // 요청 version과 현재 Entity version이 같은지 확인한다.
    private void validateVersion(
            AppUser appUser,
            Long requestVersion) {

        if (!Objects.equals(appUser.getVersion(), requestVersion)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "다른 사용자가 먼저 수정했습니다. 최신 사용자 정보를 다시 조회해 주세요.");
        }
    }

    // UPDATE SQL을 즉시 실행하여 JPA @Version 동시 수정 충돌을 현재 요청 안에서 확인한다.
    private void flushUserChanges() {

        try {
            appUserRepository.flush();

        } catch (ObjectOptimisticLockingFailureException exception) {

            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "다른 사용자가 먼저 수정했습니다. 최신 사용자 정보를 다시 조회해 주세요.");
        }
    }
}