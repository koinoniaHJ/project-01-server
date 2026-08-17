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

    @Test
    @DisplayName("로그인 성공 시 최종 로그인 일시를 갱신하고 version은 유지한다")
    void successfulLoginUpdatesLastLoginAtWithoutChangingVersion() {

        AppUser user = saveUser("office1");
        Long versionBeforeLogin = user.getVersion();
        LocalDateTime loginStartedAt = LocalDateTime.now();

        authService.authenticate(new LoginRequest(user.getUsername(), PASSWORD));

        entityManager.clear();
        AppUser updatedUser = findUser(user.getUserId());

        assertThat(updatedUser.getLastLoginAt())
                .isNotNull()
                .isAfterOrEqualTo(loginStartedAt);

        assertThat(updatedUser.getVersion())
                .isEqualTo(versionBeforeLogin);
    }

    @Test
    @DisplayName("로그인 실패 시 최종 로그인 일시와 version을 변경하지 않는다")
    void failedLoginDoesNotUpdateLastLoginAtOrVersion() {

        AppUser user = saveUser("office1");
        Long versionBeforeLogin = user.getVersion();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.authenticate(
                        new LoginRequest(user.getUsername(), "wrong-password"))
        );

        entityManager.clear();
        AppUser unchangedUser = findUser(user.getUserId());

        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        assertThat(unchangedUser.getLastLoginAt())
                .isNull();

        assertThat(unchangedUser.getVersion())
                .isEqualTo(versionBeforeLogin);
    }

    private AppUser saveUser(String username) {

        AppUser user = AppUser.create(
                username,
                passwordEncoder.encode(PASSWORD),
                "사무 직원",
                UserRole.OFFICE,
                null
        );

        return appUserRepository.saveAndFlush(user);
    }

    private AppUser findUser(Long userId) {

        return appUserRepository.findById(userId)
                .orElseThrow();
    }
}
