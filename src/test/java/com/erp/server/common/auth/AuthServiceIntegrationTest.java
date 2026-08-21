package com.erp.server.common.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.erp.server.common.auth.dto.LoginRequest;
import com.erp.server.common.auth.service.AuthService;
import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.domain.UserRole;
import com.erp.server.common.user.repository.AppUserRepository;

import jakarta.persistence.EntityManager;

// ********** 로그인 성공·실패에 따른 last_login_at 변경 여부와 사용자 관리용 version 유지 여부를 확인하기 위한 인증 Service 통합 테스트 클래스 **********
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceIntegrationTest {

	private static final String PASSWORD = "Password1234!";

	@Autowired
	private AuthService authService;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EntityManager entityManager;

	// ========== 로그인 성공 시 최종 로그인 일시만 갱신되는지 확인하는 테스트 메서드 ==========
	@Test
	@DisplayName("로그인 성공 시 최종 로그인 일시를 갱신하고 version은 유지한다")
	void successfulLoginUpdatesLastLoginAtWithoutChangingVersion() {

		AppUser user = saveUser("office1");
		Long versionBeforeLogin = user.getVersion();
		LocalDateTime loginStartedAt = LocalDateTime.now();

		authService.authenticate(new LoginRequest(user.getUsername(), PASSWORD));

		// 영속성 Context의 기존 객체를 비워 다음 조회가 DB의 최신 값을 가져오게 한다.
		entityManager.clear();
		AppUser updatedUser = findUser(user.getUserId());

		assertThat(updatedUser.getLastLoginAt()).isNotNull().isAfterOrEqualTo(loginStartedAt);

		assertThat(updatedUser.getVersion()).isEqualTo(versionBeforeLogin);
	}

	// ========== 로그인 실패 시 최종 로그인 일시와 version이 유지되는지 확인하는 테스트 메서드 ==========
	@Test
	@DisplayName("로그인 실패 시 최종 로그인 일시와 version을 변경하지 않는다")
	void failedLoginDoesNotUpdateLastLoginAtOrVersion() {

		AppUser user = saveUser("office1");
		Long versionBeforeLogin = user.getVersion();

		BusinessException exception = assertThrows(BusinessException.class,
				() -> authService.authenticate(new LoginRequest(user.getUsername(), "wrong-password")));

		entityManager.clear();
		AppUser unchangedUser = findUser(user.getUserId());

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);

		assertThat(unchangedUser.getLastLoginAt()).isNull();

		assertThat(unchangedUser.getVersion()).isEqualTo(versionBeforeLogin);
	}

	// ========== 테스트용 ACTIVE OFFICE 사용자를 저장하는 메서드 ==========
	private AppUser saveUser(String username) {

		AppUser user = AppUser.create(username, passwordEncoder.encode(PASSWORD), "사무 직원", UserRole.OFFICE, null);

		return appUserRepository.saveAndFlush(user);
	}

	// ========== 테스트 사용자를 userId로 조회하는 메서드 ==========
	private AppUser findUser(Long userId) {
		return appUserRepository.findById(userId).orElseThrow();
	}
}