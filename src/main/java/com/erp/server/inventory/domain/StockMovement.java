package com.erp.server.inventory.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.master.item.domain.Item;
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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ********** Oracle Database의 STOCK_MOVEMENT 테이블과 실제 재고 증감 원인·수량·전후 수량 및 원본 업무를 Java 객체로 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "STOCK_MOVEMENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovement {

	// Oracle의 SEQ_STOCK_MOVEMENT에서 다음 값을 받아 PK로 사용하는 재고 변동 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stockMovementSequenceGenerator")
	@SequenceGenerator(name = "stockMovementSequenceGenerator", sequenceName = "SEQ_STOCK_MOVEMENT", allocationSize = 1)
	@Column(name = "stock_movement_id", nullable = false)
	private Long stockMovementId;

	// 재고 수량이 실제로 변경된 INVENTORY_LOT를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inventory_lot_id", nullable = false)
	private InventoryLot inventoryLot;

	// 변동 당시 재고 LOT가 보관된 WAREHOUSE 창고를 참조하여 이력 조회 기준을 보존한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;

	// 변동 당시 재고 LOT의 ITEM 품목을 참조하여 이력 조회 기준을 보존한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	// 재고 변동 원인을 RECEIPT, SHIPMENT, PURCHASE_RETURN, RETURN_IN, ADJUSTMENT_IN, ADJUSTMENT_OUT 또는 DISPOSAL로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 30)
	private StockMovementType type;

	// 증가 이력은 양수, 감소 이력은 음수로 실제 현재 재고에 적용된 수량을 저장한다.
	@Column(name = "change_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal changeQuantity;

	// 재고 변동을 적용하기 직전의 LOT 현재 재고 수량을 저장한다.
	@Column(name = "before_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal beforeQuantity;

	// 재고 변동을 적용한 직후의 LOT 현재 재고 수량을 저장한다.
	@Column(name = "after_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal afterQuantity;

	// RECEIPT 변동을 발생시킨 RECEIPT_LOT 식별자를 저장하며 다른 변동 유형에서는 null이다.
	// 입고 Entity 구현 전에도 재고 코어가 독립적으로 컴파일되도록 식별자만 보관하고 DB FK로 무결성을 보장한다.
	@Column(name = "receipt_lot_id")
	private Long receiptLotId;

	// SHIPMENT 변동을 발생시킨 SHIPMENT_LOT 식별자를 저장하며 다른 변동 유형에서는 null이다.
	// 출고 Entity 구현 전에도 재고 코어가 독립적으로 컴파일되도록 식별자만 보관하고 DB FK로 무결성을 보장한다.
	@Column(name = "shipment_lot_id")
	private Long shipmentLotId;

	// PURCHASE_RETURN 변동을 발생시킨 PURCHASE_RETURN_ITEM 식별자를 저장하며 다른 변동 유형에서는 null이다.
	// 매입 반품 Entity 구현 전에도 재고 코어가 독립적으로 컴파일되도록 식별자만 보관하고 DB FK로 무결성을 보장한다.
	@Column(name = "purchase_return_item_id")
	private Long purchaseReturnItemId;

	// RETURN_IN 변동을 발생시킨 CUSTOMER_RETURN_ITEM 식별자를 저장하며 다른 변동 유형에서는 null이다.
	// 거래처 반품 Entity 구현 전에도 재고 코어가 독립적으로 컴파일되도록 식별자만 보관하고 DB FK로 무결성을 보장한다.
	@Column(name = "customer_return_item_id")
	private Long customerReturnItemId;

	// ADJUSTMENT_IN 또는 ADJUSTMENT_OUT 변동을 발생시킨 STOCK_ADJUSTMENT 식별자를 저장하며 다른 유형에서는 null이다.
	// 재고 조정 Entity 구현 전에도 재고 코어가 독립적으로 컴파일되도록 식별자만 보관하고 DB FK로 무결성을 보장한다.
	@Column(name = "stock_adjustment_id")
	private Long stockAdjustmentId;

	// 재고 조정 또는 폐기 등의 변동 사유를 저장하며 폐기 이력에서는 반드시 값이 존재한다.
	@Column(name = "reason", length = 1000)
	private String reason;

	// 재고 변동 업무를 실행한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "processed_by", nullable = false)
	private AppUser processedBy;

	// 실제 현재 재고 수량과 이력이 같은 트랜잭션에서 변경된 일시를 저장한다.
	@Column(name = "processed_at", nullable = false, updatable = false)
	private LocalDateTime processedAt;

	// ========== 신규 재고 변동 이력이 저장되기 전에 처리 일시를 설정하는 메서드 ==========
	@PrePersist
	protected void onCreate() {
		processedAt = LocalDateTime.now();
	}

	// ========== 입고 완료로 증가한 재고와 원본 RECEIPT_LOT를 연결하는 이력을 생성하는 정적 팩토리 메서드 ==========
	public static StockMovement createReceipt(InventoryLot lot, BigDecimal quantity, BigDecimal beforeQuantity,
			Long receiptLotId, AppUser processedBy) {
		StockMovement movement = createBase(lot, StockMovementType.RECEIPT, quantity, beforeQuantity, null, processedBy);
		movement.receiptLotId = receiptLotId;
		return movement;
	}

	// ========== 출고 완료로 감소한 재고와 원본 SHIPMENT_LOT를 연결하는 이력을 생성하는 정적 팩토리 메서드 ==========
	public static StockMovement createShipment(InventoryLot lot, BigDecimal quantity, BigDecimal beforeQuantity,
			Long shipmentLotId, AppUser processedBy) {
		StockMovement movement = createBase(lot, StockMovementType.SHIPMENT, quantity, beforeQuantity, null, processedBy);
		movement.shipmentLotId = shipmentLotId;
		return movement;
	}

	// ========== 매입 반품 완료로 감소한 재고와 원본 PURCHASE_RETURN_ITEM을 연결하는 이력을 생성하는 정적 팩토리 메서드 ==========
	public static StockMovement createPurchaseReturn(InventoryLot lot, BigDecimal quantity,
			BigDecimal beforeQuantity, Long purchaseReturnItemId, AppUser processedBy) {
		StockMovement movement = createBase(lot, StockMovementType.PURCHASE_RETURN, quantity, beforeQuantity, null,
				processedBy);
		movement.purchaseReturnItemId = purchaseReturnItemId;
		return movement;
	}

	// ========== 거래처 반품 완료로 증가한 재고와 원본 CUSTOMER_RETURN_ITEM을 연결하는 이력을 생성하는 정적 팩토리 메서드 ==========
	public static StockMovement createReturnIn(InventoryLot lot, BigDecimal quantity, BigDecimal beforeQuantity,
			Long customerReturnItemId, AppUser processedBy) {
		StockMovement movement = createBase(lot, StockMovementType.RETURN_IN, quantity, beforeQuantity, null,
				processedBy);
		movement.customerReturnItemId = customerReturnItemId;
		return movement;
	}

	// ========== 재고 조정 승인으로 증가하거나 감소한 재고와 원본 STOCK_ADJUSTMENT를 연결하는 이력을 생성하는 정적 팩토리 메서드 ==========
	public static StockMovement createAdjustment(InventoryLot lot, StockMovementType type, BigDecimal quantity,
			BigDecimal beforeQuantity, Long stockAdjustmentId, String reason, AppUser processedBy) {
		StockMovement movement = createBase(lot, type, quantity, beforeQuantity, reason, processedBy);
		movement.stockAdjustmentId = stockAdjustmentId;
		return movement;
	}

	// ========== 사용기한 경과 또는 출고 제한 LOT의 폐기 수량과 필수 사유를 기록하는 이력을 생성하는 정적 팩토리 메서드 ==========
	public static StockMovement createDisposal(InventoryLot lot, BigDecimal quantity, BigDecimal beforeQuantity,
			String reason, AppUser processedBy) {
		return createBase(lot, StockMovementType.DISPOSAL, quantity, beforeQuantity, reason, processedBy);
	}

	// ========== 변동 유형의 방향에 따라 부호·변동 후 수량과 공통 이력 정보를 계산하는 메서드 ==========
	private static StockMovement createBase(InventoryLot lot, StockMovementType type, BigDecimal quantity,
			BigDecimal beforeQuantity, String reason, AppUser processedBy) {

		StockMovement movement = new StockMovement();
		BigDecimal changeQuantity = type.getDirection().apply(quantity);

		movement.inventoryLot = lot;
		movement.warehouse = lot.getWarehouse();
		movement.item = lot.getItem();
		movement.type = type;
		movement.changeQuantity = changeQuantity;
		movement.beforeQuantity = beforeQuantity;
		movement.afterQuantity = beforeQuantity.add(changeQuantity);
		movement.reason = reason;
		movement.processedBy = processedBy;

		return movement;
	}
}
