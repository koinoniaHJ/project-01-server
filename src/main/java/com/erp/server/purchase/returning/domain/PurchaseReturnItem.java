package com.erp.server.purchase.returning.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import com.erp.server.inventory.domain.InventoryLot;
import com.erp.server.master.item.domain.Item;
import com.erp.server.purchase.receipt.domain.ReceiptLot;

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

// ********** Oracle Database의 PURCHASE_RETURN_ITEM 테이블과 원본 입고 LOT별 반품 수량·매입 단가·금액을 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "PURCHASE_RETURN_ITEM")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseReturnItem {

	// Oracle의 SEQ_PURCHASE_RETURN_ITEM에서 다음 값을 받아 PK로 사용하는 매입 반품 품목 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchaseReturnItemSequenceGenerator")
	@SequenceGenerator(name = "purchaseReturnItemSequenceGenerator", sequenceName = "SEQ_PURCHASE_RETURN_ITEM", allocationSize = 1)
	@Column(name = "purchase_return_item_id", nullable = false)
	private Long purchaseReturnItemId;

	// 품목이 포함된 상위 PURCHASE_RETURN 매입 반품을 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "purchase_return_id", nullable = false)
	private PurchaseReturn purchaseReturn;

	// 원본 정상 입고 수량과 누적 완료 반품 수량의 기준이 되는 RECEIPT_LOT를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "receipt_lot_id", nullable = false)
	private ReceiptLot receiptLot;

	// 반품 완료 시 현재·예약 수량을 검증하고 감소시킬 INVENTORY_LOT를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inventory_lot_id", nullable = false)
	private InventoryLot inventoryLot;

	// 원본 입고 LOT에 연결된 ITEM 품목을 조회와 전표 스냅샷 생성 기준으로 저장한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	// 공급업체에 실제 반환할 LOT별 수량을 저장한다.
	@Column(name = "return_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal returnQuantity;

	// 원본 입고의 발주 품목에 적용된 매입 단가를 반품 등록 시점에 고정하여 저장한다.
	@Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal unitPrice;

	// 반품 수량과 매입 단가를 곱하여 소수점 둘째 자리로 반올림한 양수 금액을 저장한다.
	@Column(name = "line_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal lineAmount;

	// 매입 반품 품목 최초 등록 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 반품 수량이 마지막으로 변경된 일시를 저장한다.
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// ========== 신규 Entity 저장 전에 등록·수정 일시를 설정하는 메서드 ==========
	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	// ========== 기존 Entity 수정 전에 최근 수정 일시를 갱신하는 메서드 ==========
	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	// ========== 상위 반품·원본 입고 LOT·반품 수량·원본 매입 단가로 반품 품목을 생성하는 정적 팩토리 메서드 ==========
	public static PurchaseReturnItem create(PurchaseReturn purchaseReturn, ReceiptLot receiptLot,
			BigDecimal returnQuantity, BigDecimal unitPrice) {
		PurchaseReturnItem item = new PurchaseReturnItem();
		item.purchaseReturn = purchaseReturn;
		item.receiptLot = receiptLot;
		item.inventoryLot = receiptLot.getInventoryLot();
		item.item = receiptLot.getReceiptItem().getPurchaseOrderItem().getItem();
		item.returnQuantity = returnQuantity;
		item.unitPrice = unitPrice;
		item.lineAmount = returnQuantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
		return item;
	}
}
