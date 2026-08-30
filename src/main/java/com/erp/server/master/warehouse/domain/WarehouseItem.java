package com.erp.server.master.warehouse.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.master.item.domain.Item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

// ********** Oracle Database의 WAREHOUSE_ITEM 테이블과 창고·품목별 안전재고 기준을 Java 객체로 매핑하기 위한 Entity 클래스 **********
@Entity
@Table(name = "WAREHOUSE_ITEM")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarehouseItem {

	// Oracle의 SEQ_WAREHOUSE_ITEM에서 다음 값을 받아 PK로 사용하는 창고 품목 기준 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "warehouseItemSequenceGenerator")
	@SequenceGenerator(name = "warehouseItemSequenceGenerator", sequenceName = "SEQ_WAREHOUSE_ITEM", allocationSize = 1)
	@Column(name = "warehouse_item_id", nullable = false)
	private Long warehouseItemId;

	// 안전재고를 설정할 WAREHOUSE 창고를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;

	// 창고별 안전재고를 설정할 ITEM 품목을 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	// 해당 창고에서 유지할 품목 기준 재고 단위의 안전재고 수량을 저장한다.
	@Column(name = "safety_stock_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal safetyStockQuantity = BigDecimal.ZERO;

	// 안전재고 기준을 최초 등록한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private AppUser createdBy;

	// 안전재고 기준의 최초 등록 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 안전재고 기준을 마지막으로 변경한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "updated_by", nullable = false)
	private AppUser updatedBy;

	// 안전재고 기준의 최근 변경 일시를 저장한다.
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// 조회 당시 값과 DB 값을 비교하여 같은 창고·품목 안전재고의 동시 변경 충돌을 확인한다.
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

	// ========== 기존 Entity가 변경되기 전에 최근 수정 일시를 갱신하는 메서드 ==========
	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	// ========== 저장되지 않은 창고·품목 조합의 안전재고 기준을 최초 생성하는 정적 팩토리 메서드 ==========
	public static WarehouseItem create(Warehouse warehouse, Item item, BigDecimal safetyStockQuantity,
			AppUser createdBy) {

		WarehouseItem warehouseItem = new WarehouseItem();

		warehouseItem.warehouse = warehouse;
		warehouseItem.item = item;
		warehouseItem.safetyStockQuantity = safetyStockQuantity;
		warehouseItem.createdBy = createdBy;
		warehouseItem.updatedBy = createdBy;

		return warehouseItem;
	}

	// ========== 기존 창고·품목 조합의 안전재고 수량과 최근 수정 사용자를 변경하는 메서드 ==========
	public void updateSafetyStockQuantity(BigDecimal safetyStockQuantity, AppUser updatedBy) {

		this.safetyStockQuantity = safetyStockQuantity;
		this.updatedBy = updatedBy;
	}
}
