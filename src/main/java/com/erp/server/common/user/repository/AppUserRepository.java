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

// ********** APP_USER의 기본 CRUD, 로그인 사용자 조회, 최종 로그인 갱신, 사용자 목록 조회와 마지막 활성 ADMIN 보호 조회를 처리하기 위한 Repository interface **********
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    // ========== 로그인 아이디로 사용자를 조회하는 Query Method ==========
    Optional<AppUser> findByUsername(String username);

    // ========== 사용자 version을 변경하지 않고 최종 로그인 일시만 갱신하는 메서드 ==========
    @Modifying
    @Query("""
            update AppUser u
            set u.lastLoginAt = :lastLoginAt
            where u.userId = :userId
            """)
    void updateLastLoginAt(
            @Param("userId") Long userId,
            @Param("lastLoginAt") LocalDateTime lastLoginAt);

    // ========== 같은 로그인 아이디가 존재하는지 확인하는 Query Method ==========
    boolean existsByUsername(String username);

    // ========== 활성 ADMIN 목록을 잠근 상태로 조회하는 메서드 ==========
    // 동시에 들어온 강등·비활성화 요청이 같은 관리자 수를 보고 모두 통과하지 못하도록 PESSIMISTIC_WRITE를 사용한다.
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

    // ========== 상태와 역할을 선택 조건으로 적용하여 사용자 목록을 페이지 조회하는 메서드 ==========
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