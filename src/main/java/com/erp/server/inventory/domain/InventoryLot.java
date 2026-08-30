package com.erp.server.inventory.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.master.item.domain.Item;
import com.erp.server.master.supplier.domain.Supplier;
import com.erp.server.master.warehouse.domain.Warehouse;

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

// ********** Oracle Database의 INVENTORY_LOT 테이블과 창고·품목·LOT별 재고 수량 및 출고 상태를 Java 객체로 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "INVENTORY_LOT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryLot {

	// Oracle의 SEQ_INVENTORY_LOT에서 다음 값을 받아 PK로 사용하는 재고 LOT 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventoryLotSequenceGenerator")
	@SequenceGenerator(name = "inventoryLotSequenceGenerator", sequenceName = "SEQ_INVENTORY_LOT", allocationSize = 1)
	@Column(name = "inventory_lot_id", nullable = false)
	private Long inventoryLotId;

	// 재고 LOT를 실제로 보관하는 WAREHOUSE 창고를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;

	// 재고 LOT에 보관된 ITEM 품목을 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	// 해당 LOT가 최초 입고된 SUPPLIER 공급업체를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id", nullable = false)
	private Supplier supplier;

	// 공급업체 LOT 번호 또는 시스템이 생성한 LOT000001 형식의 내부 LOT 번호를 저장한다.
	@Column(name = "lot_number", nullable = false, length = 100)
	private String lotNumber;

	// 공급업체가 제공한 LOT 번호를 저장하며 내부 LOT인 경우 null로 유지한다.
	@Column(name = "supplier_lot_number", length = 100)
	private String supplierLotNumber;

	// 공급업체 LOT가 없어 시스템에서 내부 LOT 번호를 생성했는지 Y 또는 N으로 저장한다.
	@Column(name = "internal_lot_yn", nullable = false, length = 1)
	private String internalLotYn;

	// LOT를 출고에 사용할 수 있는 마지막 날짜를 저장하고 현재 날짜와 비교하여 사용기한 경과 여부를 판단한다.
	@Column(name = "expiry_date", nullable = false)
	private LocalDate expiryDate;

	// LOT의 출고 가능 또는 출고 제한 상태를 AVAILABLE 또는 BLOCKED 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private InventoryLotStatus status = InventoryLotStatus.AVAILABLE;

	// 입고·반품·출고·조정·폐기 결과가 반영된 LOT의 실제 현재 재고 수량을 저장한다.
	@Column(name = "current_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal currentQuantity = BigDecimal.ZERO;

	// 포장 완료되었지만 아직 출고 완료되지 않은 출고에 배정된 LOT 수량을 저장한다.
	@Column(name = "reserved_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal reservedQuantity = BigDecimal.ZERO;

	// 재고 LOT를 최초 생성한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private AppUser createdBy;

	// 재고 LOT가 최초 생성된 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 현재·예약 수량 또는 LOT 상태가 마지막으로 변경된 일시를 저장한다.
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// 조회 당시 값과 DB 값을 비교하여 재고·예약 수량 및 LOT 상태의 동시 처리 충돌을 확인한다.
	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	// ========== 신규 Entity가 저장되기 전에 생성·수정 일시와 수량 기본값을 설정하는 메서드 ==========
	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();

		createdAt = now;
		updatedAt = now;
		currentQuantity = currentQuantity == null ? BigDecimal.ZERO : currentQuantity;
		reservedQuantity = reservedQuantity == null ? BigDecimal.ZERO : reservedQuantity;
	}

	// ========== 기존 Entity가 수정되기 전에 최근 수정 일시를 갱신하는 메서드 ==========
	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	// ========== 공급업체 LOT 번호를 그대로 적용하여 AVAILABLE 상태의 신규 재고 LOT를 생성하는 정적 팩토리 메서드 ==========
	public static InventoryLot createSupplierLot(Warehouse warehouse, Item item, Supplier supplier,
			String supplierLotNumber, LocalDate expiryDate, AppUser createdBy) {

		InventoryLot inventoryLot = createBaseLot(warehouse, item, supplier, supplierLotNumber, expiryDate, createdBy);
		inventoryLot.supplierLotNumber = supplierLotNumber;
		inventoryLot.internalLotYn = "N";

		return inventoryLot;
	}

	// ========== 공급업체 LOT가 없을 때 시스템 생성 번호를 적용하여 AVAILABLE 상태의 신규 내부 LOT를 생성하는 정적 팩토리 메서드 ==========
	public static InventoryLot createInternalLot(Warehouse warehouse, Item item, Supplier supplier,
			String internalLotNumber, LocalDate expiryDate, AppUser createdBy) {

		InventoryLot inventoryLot = createBaseLot(warehouse, item, supplier, internalLotNumber, expiryDate, createdBy);
		inventoryLot.supplierLotNumber = null;
		inventoryLot.internalLotYn = "Y";

		return inventoryLot;
	}

	// ========== 신규 재고 LOT가 공통으로 사용하는 창고·품목·공급업체·번호·사용기한·생성자를 설정하는 메서드 ==========
	private static InventoryLot createBaseLot(Warehouse warehouse, Item item, Supplier supplier, String lotNumber,
			LocalDate expiryDate, AppUser createdBy) {

		InventoryLot inventoryLot = new InventoryLot();

		inventoryLot.warehouse = warehouse;
		inventoryLot.item = item;
		inventoryLot.supplier = supplier;
		inventoryLot.lotNumber = lotNumber;
		inventoryLot.expiryDate = expiryDate;
		inventoryLot.status = InventoryLotStatus.AVAILABLE;
		inventoryLot.currentQuantity = BigDecimal.ZERO;
		inventoryLot.reservedQuantity = BigDecimal.ZERO;
		inventoryLot.createdBy = createdBy;

		return inventoryLot;
	}

	// ========== 현재 재고에서 포장 완료 예약 수량을 차감하여 LOT의 가용 수량을 계산하는 메서드 ==========
	// LOT 상태·사용기한·실사/조정 제한은 Service에서 별도로 검증한 뒤 이 값을 사용한다.
	public BigDecimal calculateAvailableQuantity() {
		return currentQuantity.subtract(reservedQuantity);
	}

	// ========== 입고·판매 반품 입고·증가 조정 수량을 현재 재고에 더하는 메서드 ==========
	public void increaseCurrentQuantity(BigDecimal quantity) {
		currentQuantity = currentQuantity.add(quantity);
	}

	// ========== 매입 반품·감소 조정·폐기 수량을 현재 재고에서 차감하는 메서드 ==========
	public void decreaseCurrentQuantity(BigDecimal quantity) {
		currentQuantity = currentQuantity.subtract(quantity);
	}

	// ========== 포장 완료된 출고의 LOT 배정 수량을 예약 수량에 더하는 메서드 ==========
	public void increaseReservedQuantity(BigDecimal quantity) {
		reservedQuantity = reservedQuantity.add(quantity);
	}

	// ========== 포장 취소된 출고의 LOT 배정 수량을 예약 수량에서 차감하는 메서드 ==========
	public void decreaseReservedQuantity(BigDecimal quantity) {
		reservedQuantity = reservedQuantity.subtract(quantity);
	}

	// ========== 출고 완료 수량을 현재 재고와 예약 수량에서 함께 차감하는 메서드 ==========
	public void completeShipmentQuantity(BigDecimal quantity) {
		currentQuantity = currentQuantity.subtract(quantity);
		reservedQuantity = reservedQuantity.subtract(quantity);
	}

	// ========== 내부 생성 LOT인지 확인하는 메서드 ==========
	public boolean isInternalLot() {
		return "Y".equals(internalLotYn);
	}

	// ========== LOT 상태가 출고 가능한 AVAILABLE인지 확인하는 메서드 ==========
	public boolean isAvailableStatus() {
		return status == InventoryLotStatus.AVAILABLE;
	}

	// ========== 현재 날짜를 기준으로 LOT 사용기한이 경과했는지 확인하는 메서드 ==========
	// 사용기한 당일은 출고 가능하며 현재 날짜가 사용기한보다 뒤인 경우에만 경과로 판단한다.
	public boolean isExpired(LocalDate currentDate) {
		return expiryDate.isBefore(currentDate);
	}
}
