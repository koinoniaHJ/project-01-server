package com.erp.server.common.user.controller;

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
import com.erp.server.common.user.domain.UserRole;
import com.erp.server.common.user.domain.UserStatus;
import com.erp.server.common.user.dto.UserCreateRequest;
import com.erp.server.common.user.dto.UserPasswordResetRequest;
import com.erp.server.common.user.dto.UserResponse;
import com.erp.server.common.user.dto.UserStatusRequest;
import com.erp.server.common.user.dto.UserUpdateRequest;
import com.erp.server.common.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// ********** 사용자 관리 REST 요청을 받아 입력값을 검증하고 현재 ADMIN의 userId와 함께 UserService에 전달하기 위한 Controller 클래스 **********
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	// ========== 상태와 역할 조건을 적용하여 사용자 목록을 조회하는 메서드 ==========
	@GetMapping
	public ApiResponse<List<UserResponse>> getUsers(@RequestParam(name = "status", required = false) UserStatus status,
			@RequestParam(name = "role", required = false) UserRole role,
			@PageableDefault(size = 20, sort = "userId", direction = Sort.Direction.DESC) Pageable pageable) {

		Page<UserResponse> users = userService.getUsers(status, role, pageable);

		return ApiResponse.success(users.getContent(), PageMeta.from(users));
	}

	// ========== 신규 사용자를 등록하는 메서드 ==========
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request,
			Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(userService.createUser(request, currentUser.getUserId()));
	}

	// ========== 사용자명과 역할을 수정하는 메서드 ==========
	@PatchMapping("/{userId}")
	public ApiResponse<UserResponse> updateUser(@PathVariable(name = "userId") Long userId,
			@Valid @RequestBody UserUpdateRequest request, Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(userService.updateUser(userId, request, currentUser.getUserId()));
	}

	// ========== 사용자 ACTIVE·INACTIVE 상태를 변경하는 메서드 ==========
	@PostMapping("/{userId}/status")
	public ApiResponse<UserResponse> changeStatus(@PathVariable(name = "userId") Long userId,
			@Valid @RequestBody UserStatusRequest request, Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(userService.changeStatus(userId, request, currentUser.getUserId()));
	}

	// ========== 사용자 비밀번호를 초기화하는 메서드 ==========
	@PatchMapping("/{userId}/password")
	public ApiResponse<UserResponse> resetPassword(@PathVariable(name = "userId") Long userId,
			@Valid @RequestBody UserPasswordResetRequest request, Authentication authentication) {

		AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(userService.resetPassword(userId, request, currentUser.getUserId()));
	}
}