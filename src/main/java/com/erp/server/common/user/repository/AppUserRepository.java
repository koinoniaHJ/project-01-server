package com.erp.server.common.user.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.domain.UserRole;
import com.erp.server.common.user.domain.UserStatus;

import jakarta.persistence.LockModeType;

// APP_USER의 기본 조회·저장 기능과 로그인 아이디 및 사용자 관리 조회를 처리하기 위한 Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
    
    @Modifying
    @Query("""
            update AppUser u
            set u.lastLoginAt = :lastLoginAt
            where u.userId = :userId
            """)
    void updateLastLoginAt(
            @Param("userId") Long userId,
            @Param("lastLoginAt") LocalDateTime lastLoginAt);
    
    boolean existsByUsername(String username);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u
            from AppUser u
            where u.role = :role
              and u.status = :status
            order by u.userId
            """)
    List<AppUser> findByRoleAndStatusForUpdate(
            @Param("role") UserRole role,
            @Param("status") UserStatus status);

    // 사용자 상태와 역할을 선택 조건으로 적용하여 사용자 목록을 페이지 조회한다.
    @Query("""
            select u
            from AppUser u
            where (:status is null or u.status = :status)
              and (:role is null or u.role = :role)
            """)
    Page<AppUser> findAllByFilters(
            @Param("status") UserStatus status,
            @Param("role") UserRole role,
            Pageable pageable);
}