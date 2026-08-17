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

    @Test
    @DisplayName("로그인 사용자가 INACTIVE로 변경되면 기존 세션 요청을 거부하고 세션을 무효화한다")
    void inactiveUserExistingSessionIsRejectedAndInvalidated() throws Exception {

        AppUser admin = saveAdmin("admin1");
        MockHttpSession session = login(admin.getUsername());
        String sessionId = session.getId();

        changeStatus(admin.getUserId(), UserStatus.INACTIVE);

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(new Cookie("JSESSIONID", sessionId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("ADMIN에서 OFFICE로 변경되면 기존 세션으로 사용자 관리 API에 접근할 수 없다")
    void demotedAdminCannotAccessUserApiWithExistingSession() throws Exception {

        AppUser admin = saveAdmin("admin1");
        MockHttpSession session = login(admin.getUsername());

        changeRole(admin.getUserId(), UserRole.OFFICE);

        mockMvc.perform(get("/api/v1/users").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("역할 변경 후 현재 사용자 조회는 기존 세션에 최신 역할을 반환한다")
    void meReturnsLatestRoleAfterRoleChange() throws Exception {

        AppUser admin = saveAdmin("admin1");
        MockHttpSession session = login(admin.getUsername());

        changeRole(admin.getUserId(), UserRole.OFFICE);

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(admin.getUsername()))
                .andExpect(jsonPath("$.data.role").value("OFFICE"));
    }

    private AppUser saveAdmin(String username) {

        AppUser admin = AppUser.create(
                username,
                passwordEncoder.encode(PASSWORD),
                "관리자",
                UserRole.ADMIN,
                null
        );

        return appUserRepository.saveAndFlush(admin);
    }

    private MockHttpSession login(String username) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void changeStatus(
            Long userId,
            UserStatus status) {

        entityManager.clear();
        AppUser user = findUser(userId);

        user.changeStatus(status, user);
        appUserRepository.flush();
        entityManager.clear();
    }

    private void changeRole(
            Long userId,
            UserRole role) {

        entityManager.clear();
        AppUser user = findUser(userId);

        user.update(user.getUserName(), role, user);
        appUserRepository.flush();
        entityManager.clear();
    }

    private AppUser findUser(Long userId) {

        return appUserRepository.findById(userId)
                .orElseThrow();
    }
}
