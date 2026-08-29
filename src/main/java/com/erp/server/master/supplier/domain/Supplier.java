package com.erp.server.master.supplier.domain;

import java.time.LocalDateTime;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.master.common.domain.MasterStatus;

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

// ********** Oracle Database의 SUPPLIER 테이블과 공급업체 정보를 Java 객체로 매핑하고 공급업체 정보 변경 규칙을 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "SUPPLIER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Supplier {

	// Oracle의 SEQ_SUPPLIER에서 다음 값을 받아 PK로 사용하는 공급업체 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "supplierSequenceGenerator")
	@SequenceGenerator(name = "supplierSequenceGenerator", sequenceName = "SEQ_SUPPLIER", allocationSize = 1)
	@Column(name = "supplier_id", nullable = false)
	private Long supplierId;

	// SUP + 6자리 형식으로 자동 생성되는 공급업체 코드를 저장한다.
	@Column(name = "supplier_code", nullable = false, length = 20)
	private String supplierCode;

	// 공급업체명을 저장한다.
	@Column(name = "supplier_name", nullable = false, length = 150)
	private String supplierName;

	// 공급업체 대표 연락처를 저장한다.
	@Column(name = "phone", length = 30)
	private String phone;

	// 발주서 수신에 사용하는 공급업체 이메일을 저장한다.
	@Column(name = "email", nullable = false, length = 255)
	private String email;

	// 공급업체 사업장 우편번호를 저장한다.
	@Column(name = "postal_code", length = 10)
	private String postalCode;

	// 공급업체 사업장 기본 주소를 저장한다.
	@Column(name = "address", length = 500)
	private String address;

	// 공급업체 사업장 상세 주소를 저장한다.
	@Column(name = "address_detail", length = 300)
	private String addressDetail;

	// 발주와 입고 업무에서 참고할 공급업체 특이사항을 저장한다.
	@Column(name = "memo", length = 2000)
	private String memo;

	// 공급업체 사용 상태를 ACTIVE 또는 INACTIVE 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private MasterStatus status = MasterStatus.ACTIVE;

	// 공급업체를 등록한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private AppUser createdBy;

	// 공급업체 등록 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 공급업체를 마지막으로 수정한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "updated_by", nullable = false)
	private AppUser updatedBy;

	// 공급업체의 최근 수정 일시를 저장한다.
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// 조회 당시 값과 DB 값을 비교하여 동시 수정 충돌을 확인한다.
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

	// ========== 신규 공급업체 Entity를 생성하는 정적 팩토리 메서드 ==========
	public static Supplier create(String supplierCode, String supplierName, String phone, String email,
			String postalCode, String address, String addressDetail, String memo, AppUser createdBy) {

		Supplier supplier = new Supplier();

		supplier.supplierCode = supplierCode;
		supplier.supplierName = supplierName;
		supplier.phone = phone;
		supplier.email = email;
		supplier.postalCode = postalCode;
		supplier.address = address;
		supplier.addressDetail = addressDetail;
		supplier.memo = memo;
		supplier.status = MasterStatus.ACTIVE;
		supplier.createdBy = createdBy;
		supplier.updatedBy = createdBy;

		return supplier;
	}

	// ========== 공급업체 기본정보와 주소를 변경하는 메서드 ==========
	public void update(String supplierName, String phone, String email, String postalCode, String address,
			String addressDetail, String memo, AppUser updatedBy) {

		this.supplierName = supplierName;
		this.phone = phone;
		this.email = email;
		this.postalCode = postalCode;
		this.address = address;
		this.addressDetail = addressDetail;
		this.memo = memo;
		this.updatedBy = updatedBy;
	}

	// ========== 공급업체 사용 상태를 변경하는 메서드 ==========
	public void changeStatus(MasterStatus status, AppUser updatedBy) {

		this.status = status;
		this.updatedBy = updatedBy;
	}
}
