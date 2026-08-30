package com.erp.server.purchase.receipt.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.erp.server.inventory.domain.InventoryLot;

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

// ********** Oracle Database의 RECEIPT_LOT 테이블과 정상 입고 수량의 LOT 번호·사용기한·재고 LOT 연결을 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "RECEIPT_LOT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReceiptLot {

	// Oracle의 SEQ_RECEIPT_LOT에서 다음 값을 받아 PK로 사용하는 입고 LOT 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "receiptLotSequenceGenerator")
	@SequenceGenerator(name = "receiptLotSequenceGenerator", sequenceName = "SEQ_RECEIPT_LOT", allocationSize = 1)
	@Column(name = "receipt_lot_id", nullable = false)
	private Long receiptLotId;

	// LOT가 포함된 상위 RECEIPT_ITEM 입고 품목을 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "receipt_item_id", nullable = false)
	private ReceiptItem receiptItem;

	// 공급업체가 제공한 LOT 번호를 저장하며 제공되지 않은 경우 null로 유지한다.
	@Column(name = "supplier_lot_number", length = 100)
	private String supplierLotNumber;

	// 공급업체 LOT 번호 또는 검수 완료 시 생성한 LOT000001 형식의 내부 LOT 번호를 저장한다.
	@Column(name = "lot_number", length = 100)
	private String lotNumber;

	// 공급업체 LOT가 없어 내부 LOT 번호를 생성해야 하는지 Y 또는 N으로 저장한다.
	@Column(name = "internal_lot_yn", nullable = false, length = 1, columnDefinition = "CHAR(1)")
	private String internalLotYn;

	// 해당 LOT 품목을 출고에 사용할 수 있는 마지막 날짜를 저장한다.
	@Column(name = "expiry_date", nullable = false)
	private LocalDate expiryDate;

	// 입고 품목의 정상 수량 중 해당 LOT에 실제 반영할 수량을 저장한다.
	@Column(name = "normal_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal normalQuantity;

	// 검수 완료 시 정상 수량을 반영한 INVENTORY_LOT 재고 LOT를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inventory_lot_id")
	private InventoryLot inventoryLot;

	// 입고 LOT 최초 등록 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 적용 LOT 번호와 재고 LOT 연결이 마지막으로 변경된 일시를 저장한다.
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

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

	// ========== 입고 품목·선택 공급업체 LOT·사용기한·정상 수량으로 검수 저장용 입고 LOT를 생성하는 정적 팩토리 메서드 ==========
	// 공급업체 LOT가 없으면 적용 LOT 번호는 완료 전까지 null이고 내부 LOT 여부를 Y로 기록한다.
	public static ReceiptLot create(ReceiptItem receiptItem, String supplierLotNumber, LocalDate expiryDate,
			BigDecimal normalQuantity) {
		ReceiptLot receiptLot = new ReceiptLot();
		receiptLot.receiptItem = receiptItem;
		receiptLot.supplierLotNumber = supplierLotNumber;
		receiptLot.lotNumber = supplierLotNumber;
		receiptLot.internalLotYn = supplierLotNumber == null ? "Y" : "N";
		receiptLot.expiryDate = expiryDate;
		receiptLot.normalQuantity = normalQuantity;
		return receiptLot;
	}

	// ========== 검수 완료에서 생성하거나 재사용한 재고 LOT와 실제 적용 LOT 번호를 연결하는 메서드 ==========
	public void connectInventoryLot(InventoryLot inventoryLot) {
		this.inventoryLot = inventoryLot;
		this.lotNumber = inventoryLot.getLotNumber();
		this.internalLotYn = inventoryLot.isInternalLot() ? "Y" : "N";
	}

	// ========== 공급업체 LOT가 없어 내부 LOT 번호 생성이 필요한 입고 LOT인지 확인하는 메서드 ==========
	public boolean isInternalLot() {
		return "Y".equals(internalLotYn);
	}
}
