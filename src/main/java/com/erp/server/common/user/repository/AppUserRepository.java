package com.erp.server.common.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.server.common.user.domain.AppUser;

// APP_USER의 기본 조회·저장 기능과 로그인 아이디를 이용한 사용자 조회를 처리하기 위한 Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
}