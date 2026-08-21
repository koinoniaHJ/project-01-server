package com.erp.server.common.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.domain.UserRole;
import com.erp.server.common.user.domain.UserStatus;
import com.erp.server.common.user.dto.UserResponse;
import com.erp.server.common.user.dto.UserStatusRequest;
import com.erp.server.common.user.dto.UserUpdateRequest;
import com.erp.server.common.user.repository.AppUserRepository;

// ********** 마지막 활성 ADMIN 보호 규칙의 차단 조건과 정상 변경 조건을 확인하기 위한 사용자 Service 통합 테스트 클래스 **********
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

	@Autowired
	private UserService userService;

	@Autowired
	private AppUserRepository appUserRepository;

	// ========== 마지막 활성 ADMIN의 역할 강등이 차단되는지 확인하는 테스트 메서드 ==========
	@Test
	@DisplayName("마지막 활성 ADMIN의 역할은 변경할 수 없다")
	void lastActiveAdminCannotBeDemoted() {

		AppUser admin = saveAdmin("admin1", null);

		UserUpdateRequest request = new UserUpdateRequest("관리자1", UserRole.OFFICE, admin.getVersion());

		BusinessException exception = assertThrows(BusinessException.class,
				() -> userService.updateUser(admin.getUserId(), request, admin.getUserId()));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);

		assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
	}

	// ========== 마지막 활성 ADMIN의 비활성화가 차단되는지 확인하는 테스트 메서드 ==========
	@Test
	@DisplayName("마지막 활성 ADMIN은 사용 중지할 수 없다")
	void lastActiveAdminCannotBeDeactivated() {

		AppUser admin = saveAdmin("admin1", null);

		UserStatusRequest request = new UserStatusRequest(UserStatus.INACTIVE, admin.getVersion());

		BusinessException exception = assertThrows(BusinessException.class,
				() -> userService.changeStatus(admin.getUserId(), request, admin.getUserId()));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);

		assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	// ========== 다른 활성 ADMIN이 있으면 한 명의 역할 강등이 허용되는지 확인하는 테스트 메서드 ==========
	@Test
	@DisplayName("활성 ADMIN이 두 명이면 한 명의 역할을 변경할 수 있다")
	void adminCanBeDemotedWhenAnotherActiveAdminExists() {

		AppUser targetAdmin = saveAdmin("admin1", null);
		AppUser anotherAdmin = saveAdmin("admin2", targetAdmin);

		UserUpdateRequest request = new UserUpdateRequest("관리자1", UserRole.OFFICE, targetAdmin.getVersion());

		UserResponse response = userService.updateUser(targetAdmin.getUserId(), request, anotherAdmin.getUserId());

		assertThat(response.role()).isEqualTo(UserRole.OFFICE);
		assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
	}

	// ========== 다른 활성 ADMIN이 있으면 한 명의 비활성화가 허용되는지 확인하는 테스트 메서드 ==========
	@Test
	@DisplayName("활성 ADMIN이 두 명이면 한 명을 사용 중지할 수 있다")
	void adminCanBeDeactivatedWhenAnotherActiveAdminExists() {

		AppUser targetAdmin = saveAdmin("admin1", null);
		AppUser anotherAdmin = saveAdmin("admin2", targetAdmin);

		UserStatusRequest request = new UserStatusRequest(UserStatus.INACTIVE, targetAdmin.getVersion());

		UserResponse response = userService.changeStatus(targetAdmin.getUserId(), request, anotherAdmin.getUserId());

		assertThat(response.role()).isEqualTo(UserRole.ADMIN);
		assertThat(response.status()).isEqualTo(UserStatus.INACTIVE);
	}

	// ========== 테스트용 ACTIVE ADMIN 사용자를 H2 DB에 저장하는 메서드 ==========
	private AppUser saveAdmin(String username, AppUser createdBy) {

		AppUser admin = AppUser.create(username, "test-password-hash", username, UserRole.ADMIN, createdBy);

		return appUserRepository.saveAndFlush(admin);
	}
}