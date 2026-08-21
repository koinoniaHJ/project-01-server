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

// ********** Oracle Database의 APP_USER 테이블과 사용자 계정 정보를 Java 객체로 매핑하고 계정 정보 변경 규칙을 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "APP_USER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser {

	// Oracle의 SEQ_APP_USER에서 다음 값을 받아 PK로 사용한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "appUserSequenceGenerator")
	@SequenceGenerator(name = "appUserSequenceGenerator", sequenceName = "SEQ_APP_USER", allocationSize = 1)
	@Column(name = "user_id", nullable = false)
	private Long userId;

	// 로그인 아이디이며 DB의 UNIQUE 제약으로 중복을 최종 차단한다.
	@Column(name = "username", nullable = false, length = 50)
	private String username;

	// 평문 비밀번호가 아닌 BCrypt 해시값을 저장한다.
	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "user_name", nullable = false, length = 100)
	private String userName;

	// Enum 이름을 ADMIN, OFFICE, WAREHOUSE 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private UserRole role;

	// Enum 이름을 ACTIVE, INACTIVE 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	// 사용자를 등록한 APP_USER를 같은 테이블의 Entity 관계로 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private AppUser createdBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 사용자를 마지막으로 수정한 APP_USER를 같은 테이블의 Entity 관계로 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "updated_by")
	private AppUser updatedBy;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// UPDATE 시 조회 당시의 version과 DB의 version을 비교하여 동시 수정 충돌을 확인한다.
	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	// ========== 신규 Entity가 저장되기 전에 등록·수정 일시를 설정하는 메서드 ==========
	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();

		createdAt = now;
		updatedAt = now;
	}

	// ========== 기존 Entity가 수정되기 전에 최근 수정 일시를 갱신하는 메서드 ==========
	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	// Lombok이 username과 userName의 Getter 이름을 충돌로 판단할 수 있어 직접 구분한다.
	// ========== 로그인 아이디를 반환하는 메서드 ==========
	public String getUsername() {
		return username;
	}

	// ========== 사용자 표시 이름을 반환하는 메서드 ==========
	public String getUserName() {
		return userName;
	}

	// ========== 신규 사용자 Entity를 생성하는 정적 팩토리 메서드 ==========
	public static AppUser create(String username, String passwordHash, String userName, UserRole role,
			AppUser createdBy) {

		AppUser appUser = new AppUser();

		appUser.username = username;
		appUser.passwordHash = passwordHash;
		appUser.userName = userName;
		appUser.role = role;
		appUser.status = UserStatus.ACTIVE;
		appUser.createdBy = createdBy;
		appUser.updatedBy = createdBy;

		return appUser;
	}

	// ========== 사용자명과 역할을 변경하는 메서드 ==========
	public void update(String userName, UserRole role, AppUser updatedBy) {

		this.userName = userName;
		this.role = role;
		this.updatedBy = updatedBy;
	}

	// ========== 사용자 상태를 변경하는 메서드 ==========
	public void changeStatus(UserStatus status, AppUser updatedBy) {

		this.status = status;
		this.updatedBy = updatedBy;
	}

	// ========== 사용자 비밀번호 해시를 변경하는 메서드 ==========
	public void resetPassword(String passwordHash, AppUser updatedBy) {

		this.passwordHash = passwordHash;
		this.updatedBy = updatedBy;
	}
}