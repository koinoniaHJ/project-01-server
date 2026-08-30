package com.erp.server.purchase.order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ********** Oracle Database의 PURCHASE_ORDER_ITEM 테이블과 발주 품목별 수량·단가·금액·누적 입고 수량을 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "PURCHASE_ORDER_ITEM")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrderItem {

	// Oracle의 SEQ_PURCHASE_ORDER_ITEM에서 다음 값을 받아 PK로 사용하는 발주 품목 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchaseOrderItemSequenceGenerator")
	@SequenceGenerator(name = "purchaseOrderItemSequenceGenerator", sequenceName = "SEQ_PURCHASE_ORDER_ITEM", allocationSize = 1)
	@Column(name = "purchase_order_item_id", nullable = false)
	private Long purchaseOrderItemId;

	// 품목이 포함된 상위 PURCHASE_ORDER 발주를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "purchase_order_id", nullable = false)
	private PurchaseOrder purchaseOrder;

	// 발주서 안에서 품목을 표시하는 1부터 시작하는 순번을 저장한다.
	@Column(name = "line_no", nullable = false, precision = 5)
	private Integer lineNo;

	// 발주 대상 ITEM 품목을 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	// 품목 기준 재고 단위로 계산한 발주 수량을 저장한다.
	@Column(name = "ordered_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal orderedQuantity;

	// 발주 작성 시점에 입력하여 이후 입고·반품 금액 계산 기준으로 사용하는 매입 단가를 저장한다.
	@Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal unitPrice;

	// 발주 수량과 단가를 곱한 뒤 소수 둘째 자리로 반올림한 품목 금액을 저장한다.
	@Column(name = "line_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal lineAmount;

	// 완료된 입고의 정상 수량을 누적하여 발주 잔여 수량 계산에 사용하는 값을 저장한다.
	@Column(name = "received_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal receivedQuantity = BigDecimal.ZERO;

	// 발주 품목 최초 등록 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 발주 품목이 마지막으로 변경된 일시를 저장한다.
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

	// ========== 발주·표시 순번·품목·수량·단가로 신규 발주 품목 Entity를 생성하는 정적 팩토리 메서드 ==========
	public static PurchaseOrderItem create(PurchaseOrder purchaseOrder, Integer lineNo, Item item,
			BigDecimal orderedQuantity, BigDecimal unitPrice) {
		PurchaseOrderItem purchaseOrderItem = new PurchaseOrderItem();

		purchaseOrderItem.purchaseOrder = purchaseOrder;
		purchaseOrderItem.lineNo = lineNo;
		purchaseOrderItem.item = item;
		purchaseOrderItem.orderedQuantity = orderedQuantity;
		purchaseOrderItem.unitPrice = unitPrice;
		purchaseOrderItem.lineAmount = calculateLineAmount(orderedQuantity, unitPrice);
		purchaseOrderItem.receivedQuantity = BigDecimal.ZERO;

		return purchaseOrderItem;
	}

	// ========== 발주 수량과 단가를 곱하고 DB 제약조건과 같은 소수 둘째 자리 금액으로 반올림하는 메서드 ==========
	private static BigDecimal calculateLineAmount(BigDecimal orderedQuantity, BigDecimal unitPrice) {
		return orderedQuantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
	}
}
