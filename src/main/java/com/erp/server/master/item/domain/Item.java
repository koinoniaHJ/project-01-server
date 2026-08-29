package com.erp.server.master.item.domain;

import java.math.BigDecimal;
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

// ********** Oracle Database의 ITEM 테이블과 품목 정보를 Java 객체로 매핑하고 품목 정보 변경 규칙을 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "ITEM")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

	// Oracle의 SEQ_ITEM에서 다음 값을 받아 PK로 사용하는 품목 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "itemSequenceGenerator")
	@SequenceGenerator(name = "itemSequenceGenerator", sequenceName = "SEQ_ITEM", allocationSize = 1)
	@Column(name = "item_id", nullable = false)
	private Long itemId;

	// ITM + 6자리 형식으로 자동 생성되는 품목 코드를 저장한다.
	@Column(name = "item_code", nullable = false, length = 20)
	private String itemCode;

	// 업무 화면과 발주·주문에서 사용하는 품목명을 저장한다.
	@Column(name = "item_name", nullable = false, length = 150)
	private String itemName;

	// 품목의 기준 재고 단위를 G, KG, EA, PACK, BOX 또는 OTHER 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "unit", nullable = false, length = 20)
	private ItemUnit unit;

	// 기준 재고 단위가 OTHER일 때 화면과 업무 문서에 표시할 기타 단위명을 저장한다.
	@Column(name = "other_unit_name", length = 50)
	private String otherUnitName;

	// 주문 작성 시 기본값으로 적용할 품목의 판매가격을 저장한다.
	@Column(name = "default_sales_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal defaultSalesPrice = BigDecimal.ZERO;

	// 품목 사용 상태를 ACTIVE 또는 INACTIVE 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private MasterStatus status = MasterStatus.ACTIVE;

	// 품목 관리와 발주·주문 업무에서 참고할 특이사항을 저장한다.
	@Column(name = "memo", length = 2000)
	private String memo;

	// 품목을 등록한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private AppUser createdBy;

	// 품목 등록 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 품목을 마지막으로 수정한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "updated_by", nullable = false)
	private AppUser updatedBy;

	// 품목의 최근 수정 일시를 저장한다.
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

	// ========== 신규 품목 Entity를 생성하는 정적 팩토리 메서드 ==========
	public static Item create(String itemCode, String itemName, ItemUnit unit, String otherUnitName,
			BigDecimal defaultSalesPrice, String memo, AppUser createdBy) {

		Item item = new Item();

		item.itemCode = itemCode;
		item.itemName = itemName;
		item.unit = unit;
		item.otherUnitName = otherUnitName;
		item.defaultSalesPrice = defaultSalesPrice;
		item.status = MasterStatus.ACTIVE;
		item.memo = memo;
		item.createdBy = createdBy;
		item.updatedBy = createdBy;

		return item;
	}

	// ========== 품목 기본정보와 기본 판매가격을 변경하는 메서드 ==========
	public void update(String itemName, ItemUnit unit, String otherUnitName, BigDecimal defaultSalesPrice,
			String memo, AppUser updatedBy) {

		this.itemName = itemName;
		this.unit = unit;
		this.otherUnitName = otherUnitName;
		this.defaultSalesPrice = defaultSalesPrice;
		this.memo = memo;
		this.updatedBy = updatedBy;
	}

	// ========== 품목 사용 상태를 변경하는 메서드 ==========
	public void changeStatus(MasterStatus status, AppUser updatedBy) {

		this.status = status;
		this.updatedBy = updatedBy;
	}
}
