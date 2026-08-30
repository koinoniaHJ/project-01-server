package com.erp.server.purchase.receipt.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.erp.server.purchase.order.domain.PurchaseOrderItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ********** Oracle Database의 RECEIPT_ITEM 테이블과 발주 품목별 실제·정상·불합격 입고 수량을 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "RECEIPT_ITEM")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReceiptItem {

	// Oracle의 SEQ_RECEIPT_ITEM에서 다음 값을 받아 PK로 사용하는 입고 품목 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "receiptItemSequenceGenerator")
	@SequenceGenerator(name = "receiptItemSequenceGenerator", sequenceName = "SEQ_RECEIPT_ITEM", allocationSize = 1)
	@Column(name = "receipt_item_id", nullable = false)
	private Long receiptItemId;

	// 품목이 포함된 상위 RECEIPT 입고를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "receipt_id", nullable = false)
	private Receipt receipt;

	// 입고 수량의 발주 수량·단가·누적 입고 기준이 되는 PURCHASE_ORDER_ITEM을 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "purchase_order_item_id", nullable = false)
	private PurchaseOrderItem purchaseOrderItem;

	// 현장에서 확인한 전체 입고 수량을 저장하며 정상 수량과 불합격 수량의 합과 일치해야 한다.
	@Column(name = "actual_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal actualQuantity = BigDecimal.ZERO;

	// 재고와 매입 전표에 실제 반영할 정상 입고 수량을 저장한다.
	@Column(name = "normal_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal normalQuantity = BigDecimal.ZERO;

	// 재고와 매입 전표에 반영하지 않을 불합격 수량을 저장한다.
	@Column(name = "rejected_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal rejectedQuantity = BigDecimal.ZERO;

	// 품목별 검수 결과에서 참고할 특이사항을 저장한다.
	@Column(name = "note", length = 1000)
	private String note;

	// 입고 품목 최초 등록 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 검수 수량·메모·LOT 구성이 마지막으로 변경된 일시를 저장한다.
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// 정상 입고 수량을 나누어 반영할 RECEIPT_LOT 목록을 관리하며 검수 결과 전체 저장 시 함께 교체한다.
	@OneToMany(mappedBy = "receiptItem", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReceiptLot> lots = new ArrayList<>();

	// ========== 신규 Entity가 저장되기 전에 등록·수정 일시와 수량 기본값을 설정하는 메서드 ==========
	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
		actualQuantity = actualQuantity == null ? BigDecimal.ZERO : actualQuantity;
		normalQuantity = normalQuantity == null ? BigDecimal.ZERO : normalQuantity;
		rejectedQuantity = rejectedQuantity == null ? BigDecimal.ZERO : rejectedQuantity;
	}

	// ========== 기존 Entity가 수정되기 전에 최근 수정 일시를 갱신하는 메서드 ==========
	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	// ========== 상위 입고와 원본 발주 품목으로 수량 0의 신규 입고 품목 Entity를 생성하는 정적 팩토리 메서드 ==========
	public static ReceiptItem create(Receipt receipt, PurchaseOrderItem purchaseOrderItem) {
		ReceiptItem receiptItem = new ReceiptItem();
		receiptItem.receipt = receipt;
		receiptItem.purchaseOrderItem = purchaseOrderItem;
		receiptItem.actualQuantity = BigDecimal.ZERO;
		receiptItem.normalQuantity = BigDecimal.ZERO;
		receiptItem.rejectedQuantity = BigDecimal.ZERO;
		return receiptItem;
	}

	// ========== 검수 결과 전체 저장 요청의 수량·메모로 기존 값을 교체하고 이전 LOT 구성을 초기화하는 메서드 ==========
	public void replaceInspection(BigDecimal actualQuantity, BigDecimal normalQuantity,
			BigDecimal rejectedQuantity, String note) {
		this.actualQuantity = actualQuantity;
		this.normalQuantity = normalQuantity;
		this.rejectedQuantity = rejectedQuantity;
		this.note = note;
		this.lots.clear();
	}

	// ========== 검수 결과에 정상 수량을 구성하는 입고 LOT를 추가하는 메서드 ==========
	public void addLot(ReceiptLot lot) {
		lots.add(lot);
	}
}
