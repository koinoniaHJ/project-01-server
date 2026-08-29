package com.erp.server.master.warehouse.domain;

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

// ********** Oracle Database의 WAREHOUSE 테이블과 창고 정보를 Java 객체로 매핑하고 창고 정보 변경 규칙을 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "WAREHOUSE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Warehouse {

	// Oracle의 SEQ_WAREHOUSE에서 다음 값을 받아 PK로 사용하는 창고 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "warehouseSequenceGenerator")
	@SequenceGenerator(name = "warehouseSequenceGenerator", sequenceName = "SEQ_WAREHOUSE", allocationSize = 1)
	@Column(name = "warehouse_id", nullable = false)
	private Long warehouseId;

	// WH + 6자리 형식으로 자동 생성되는 창고 코드를 저장한다.
	@Column(name = "warehouse_code", nullable = false, length = 20)
	private String warehouseCode;

	// 입고·출고·재고 업무에서 창고를 식별할 때 사용하는 창고명을 저장한다.
	@Column(name = "warehouse_name", nullable = false, length = 150)
	private String warehouseName;

	// 창고 주소의 우편번호를 저장한다.
	@Column(name = "postal_code", length = 10)
	private String postalCode;

	// 창고의 기본 주소를 저장한다.
	@Column(name = "address", length = 500)
	private String address;

	// 창고의 층·구역 등 상세 주소를 저장한다.
	@Column(name = "address_detail", length = 300)
	private String addressDetail;

	// 창고 사용 상태를 ACTIVE 또는 INACTIVE 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private MasterStatus status = MasterStatus.ACTIVE;

	// 창고 관리와 입고·출고 업무에서 참고할 특이사항을 저장한다.
	@Column(name = "memo", length = 2000)
	private String memo;

	// 창고를 등록한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private AppUser createdBy;

	// 창고 등록 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 창고를 마지막으로 수정한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "updated_by", nullable = false)
	private AppUser updatedBy;

	// 창고의 최근 수정 일시를 저장한다.
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

	// ========== 신규 창고 Entity를 생성하는 정적 팩토리 메서드 ==========
	public static Warehouse create(String warehouseCode, String warehouseName, String postalCode, String address,
			String addressDetail, String memo, AppUser createdBy) {

		Warehouse warehouse = new Warehouse();

		warehouse.warehouseCode = warehouseCode;
		warehouse.warehouseName = warehouseName;
		warehouse.postalCode = postalCode;
		warehouse.address = address;
		warehouse.addressDetail = addressDetail;
		warehouse.status = MasterStatus.ACTIVE;
		warehouse.memo = memo;
		warehouse.createdBy = createdBy;
		warehouse.updatedBy = createdBy;

		return warehouse;
	}

	// ========== 창고 기본정보와 주소를 변경하는 메서드 ==========
	public void update(String warehouseName, String postalCode, String address, String addressDetail, String memo,
			AppUser updatedBy) {

		this.warehouseName = warehouseName;
		this.postalCode = postalCode;
		this.address = address;
		this.addressDetail = addressDetail;
		this.memo = memo;
		this.updatedBy = updatedBy;
	}

	// ========== 창고 사용 상태를 변경하는 메서드 ==========
	public void changeStatus(MasterStatus status, AppUser updatedBy) {

		this.status = status;
		this.updatedBy = updatedBy;
	}
}
