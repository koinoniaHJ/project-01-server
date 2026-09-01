package com.erp.server.sales.shipment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.erp.server.inventory.domain.InventoryLot;
import com.erp.server.sales.order.domain.SalesOrderItem;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ********** Oracle Database의 SHIPMENT_LOT 테이블과 출고 품목별 LOT 포장·예약 상태를 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "SHIPMENT_LOT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShipmentLot {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shipmentLotSequenceGenerator")
	@SequenceGenerator(name = "shipmentLotSequenceGenerator", sequenceName = "SEQ_SHIPMENT_LOT", allocationSize = 1)
	@Column(name = "shipment_lot_id", nullable = false)
	private Long shipmentLotId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipment_id", nullable = false)
	private Shipment shipment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_order_item_id", nullable = false)
	private SalesOrderItem salesOrderItem;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inventory_lot_id", nullable = false)
	private InventoryLot inventoryLot;

	@Column(name = "packed_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal packedQuantity;

	// 포장 수량이 재고 LOT의 예약 수량에 반영되었는지 Y 또는 N으로 저장한다.
	@Column(name = "reserved_yn", nullable = false, length = 1, columnDefinition = "CHAR(1)")
	private String reservedYn = "N";

	@Column(name = "reserved_at")
	private LocalDateTime reservedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	// ========== PENDING 출고의 현재 포장안에 주문 품목·재고 LOT·포장 수량을 연결하는 정적 팩토리 메서드 ==========
	// 포장안 저장 단계에는 재고를 예약하지 않으므로 reservedYn은 N으로 시작한다.
	public static ShipmentLot create(Shipment shipment, SalesOrderItem salesOrderItem,
			InventoryLot inventoryLot, BigDecimal packedQuantity) {
		ShipmentLot shipmentLot = new ShipmentLot();
		shipmentLot.shipment = shipment;
		shipmentLot.salesOrderItem = salesOrderItem;
		shipmentLot.inventoryLot = inventoryLot;
		shipmentLot.packedQuantity = packedQuantity;
		shipmentLot.reservedYn = "N";
		return shipmentLot;
	}

	// ========== 포장 확정으로 포장 수량이 재고 LOT 예약에 반영된 상태와 일시를 기록하는 메서드 ==========
	public void reserve() {
		reservedYn = "Y";
		reservedAt = LocalDateTime.now();
	}

	// ========== 주문 취소·포장 취소·실제 출고 완료로 재고 예약이 소멸한 상태를 표시하는 메서드 ==========
	public void releaseReservation() {
		reservedYn = "N";
		reservedAt = null;
	}

	// ========== 재고 예약이 실제 반영된 출고 LOT인지 확인하는 메서드 ==========
	public boolean isReserved() {
		return "Y".equals(reservedYn);
	}
}
