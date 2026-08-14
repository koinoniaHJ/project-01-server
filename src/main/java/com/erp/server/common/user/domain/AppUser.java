package com.erp.server.common.user.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Oracle Database의 APP_USER 테이블과 사용자 계정 정보를 Java 객체로 매핑하기 위한 Entity 클래스
@Entity														// 해당 클래스가 JPA Entity임을 지정
@Table(name = "APP_USER")									// Entity와 매핑할 실제 DB 테이블을 지정
@Getter														// getter 메서드를 Lombok이 자동 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)			// JPA가 Entity를 생성할 때 필요한 기본 생성자를 생성한다.
public class AppUser {

    @Id				// Primary Key와 매핑되는 필드를 지정한다.
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "appUserSequenceGenerator") // PK 값을 Sequence 등의 방식으로 생성하도록 지정
    @SequenceGenerator(name = "appUserSequenceGenerator", sequenceName = "SEQ_APP_USER", allocationSize = 1) // JPA에서 사용할 Sequence Generator와 실제 Oracle Sequence를 연결
    @Column(name = "user_id", nullable = false)	// Java 필드와 DB 컬럼을 매핑한다.
    private Long userId;

    // 로그인에 사용하는 아이디이며 DB에서 UNIQUE로 관리한다.
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    // 평문 비밀번호가 아닌 비밀번호 해시를 저장한다.
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // 화면 등에 표시할 사용자명
    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    // Enum 값을 ADMIN, OFFICE, WAREHOUSE 문자열 그대로 DB에 저장한다.
    @Enumerated(EnumType.STRING)	// Java Enum 값을 Enum 이름 그대로 문자열로 DB에 저장한다.
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    // Enum 값을 ACTIVE, INACTIVE 문자열 그대로 DB에 저장한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    // 최근 정상 로그인 일시
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // 해당 사용자를 등록한 APP_USER를 다시 참조한다.
    @ManyToOne(fetch = FetchType.LAZY)	// @ManyToONe: 여러 Entity가 하나의 Entity를 참조하는 관계를 매핑한다.
    @JoinColumn(name = "created_by")	// 연관관계에서 FK로 사용할 DB 컬럼을 지정한다.
    private AppUser createdBy;

    // 최초 등록 일시는 이후 UPDATE 대상에서 제외한다.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 해당 사용자를 가장 최근에 수정한 APP_USER를 다시 참조한다.
    @ManyToOne(fetch = FetchType.LAZY) // FetchType.LAZY: 연관된 Entity를 처음부터 함께 조회하지 않고 실제로 해당 값에 접근할 때 조회하도록 지정하는 방식
    @JoinColumn(name = "updated_by")
    private AppUser updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 동일 사용자 정보를 동시에 수정할 경우 충돌을 확인하기 위한 낙관적 잠금 버전
    @Version		// 낙관적 잠금에 사용할 필드를 지정
    @Column(name = "version", nullable = false)
    private Long version;

    // 새로운 사용자 Entity가 처음 저장되기 직전에 등록·수정 일시를 설정한다.
    @PrePersist		// Entity가 처음 저장되기 직전에 실행할 메서드를 지정
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    // 기존 사용자 Entity가 UPDATE되기 직전에 최근 수정 일시를 갱신한다.
    @PreUpdate		// Entity가 수정되기 직전에 실행할 메서드를 지정
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // username과 userName은 Lombok Getter 이름이 충돌할 수 있으므로 직접 작성한다.

    // 로그인 아이디 반환
    public String getUsername() {
        return username;
    }

    // 사용자 표시 이름 반환
    public String getUserName() {
        return userName;
    }
}