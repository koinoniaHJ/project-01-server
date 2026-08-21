package com.erp.server.common.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.domain.UserRole;
import com.erp.server.common.user.domain.UserStatus;
import com.erp.server.common.user.repository.AppUserRepository;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;

// ********** 로그인 후 상태·역할 변경이 기존 Session의 접근 권한과 현재 사용자 응답에 즉시 반영되는지 확인하기 위한 인증 Session 통합 테스트 클래스 **********
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthSessionIntegrationTest {

	private static final String PASSWORD = "Password1234!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EntityManager entityManager;

	// ========== INACTIVE 사용자의 기존 Session 요청 거부와 Session 무효화를 확인하는 테스트 메서드
	// ==========
	@Test
	@DisplayName("로그인 사용자가 INACTIVE로 변경되면 기존 세션 요청을 거부하고 세션을 무효화한다")
	void inactiveUserExistingSessionIsRejectedAndInvalidated() throws Exception {

		AppUser admin = saveAdmin("admin1");
		MockHttpSession session = login(admin.getUsername());
		String sessionId = session.getId();

		changeStatus(admin.getUserId(), UserStatus.INACTIVE);

		mockMvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

		mockMvc.perform(get("/api/v1/auth/me").cookie(new Cookie("JSESSIONID", sessionId)))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	// ========== 강등된 ADMIN의 기존 Session에서 사용자 관리 API 접근이 거부되는지 확인하는 테스트 메서드
	// ==========
	@Test
	@DisplayName("ADMIN에서 OFFICE로 변경되면 기존 세션으로 사용자 관리 API에 접근할 수 없다")
	void demotedAdminCannotAccessUserApiWithExistingSession() throws Exception {

		AppUser admin = saveAdmin("admin1");
		MockHttpSession session = login(admin.getUsername());

		changeRole(admin.getUserId(), UserRole.OFFICE);

		mockMvc.perform(get("/api/v1/users").session(session)).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	// ========== 역할 변경 후 현재 사용자 조회가 최신 역할을 반환하는지 확인하는 테스트 메서드 ==========
	@Test
	@DisplayName("역할 변경 후 현재 사용자 조회는 기존 세션에 최신 역할을 반환한다")
	void meReturnsLatestRoleAfterRoleChange() throws Exception {

		AppUser admin = saveAdmin("admin1");
		MockHttpSession session = login(admin.getUsername());

		changeRole(admin.getUserId(), UserRole.OFFICE);

		mockMvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.username").value(admin.getUsername()))
				.andExpect(jsonPath("$.data.role").value("OFFICE"));
	}

	// ========== 테스트용 ACTIVE ADMIN 사용자를 저장하는 메서드 ==========
	private AppUser saveAdmin(String username) {

		AppUser admin = AppUser.create(username, passwordEncoder.encode(PASSWORD), "관리자", UserRole.ADMIN, null);

		return appUserRepository.saveAndFlush(admin);
	}

	// ========== REST 로그인 후 생성된 Mock Session을 반환하는 메서드 ==========
	private MockHttpSession login(String username) throws Exception {

		MvcResult result = mockMvc
				.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
						{
						  "username": "%s",
						  "password": "%s"
						}
						""".formatted(username, PASSWORD))).andExpect(status().isOk()).andReturn();

		return (MockHttpSession) result.getRequest().getSession(false);
	}

	// ========== DB의 사용자 상태를 변경하고 영속성 Context를 초기화하는 메서드 ==========
	private void changeStatus(Long userId, UserStatus status) {

		entityManager.clear();
		AppUser user = findUser(userId);

		user.changeStatus(status, user);
		appUserRepository.flush();
		entityManager.clear();
	}

	// ========== DB의 사용자 역할을 변경하고 영속성 Context를 초기화하는 메서드 ==========
	private void changeRole(Long userId, UserRole role) {

		entityManager.clear();
		AppUser user = findUser(userId);

		user.update(user.getUserName(), role, user);
		appUserRepository.flush();
		entityManager.clear();
	}

	// ========== 테스트 사용자를 userId로 조회하는 메서드 ==========
	private AppUser findUser(Long userId) {
		return appUserRepository.findById(userId).orElseThrow();
	}
}