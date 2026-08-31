package com.erp.server.sales.order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import com.erp.server.master.item.domain.Item;
import com.erp.server.master.item.domain.ItemUnit;

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

// ********** Oracle Database의 SALES_ORDER_ITEM 테이블과 주문 품목·판매 단가·접수 시점 스냅샷을 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "SALES_ORDER_ITEM")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesOrderItem {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "salesOrderItemSequenceGenerator")
	@SequenceGenerator(name = "salesOrderItemSequenceGenerator", sequenceName = "SEQ_SALES_ORDER_ITEM", allocationSize = 1)
	@Column(name = "sales_order_item_id", nullable = false)
	private Long salesOrderItemId;

	// 품목이 포함된 상위 SALES_ORDER 주문을 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_order_id", nullable = false)
	private SalesOrder salesOrder;

	// 주문 화면과 납품서에서 품목을 표시할 순서를 1부터 저장한다.
	@Column(name = "line_no", nullable = false)
	private Integer lineNo;

	// 주문 대상 ITEM 품목을 참조하며 접수 이후에도 원본 기준정보 관계를 유지한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	// DRAFT에서는 현재 품목 코드를 표시하고 접수 이후에는 접수 시점 값을 보존한다.
	@Column(name = "item_code_snapshot", length = 20)
	private String itemCodeSnapshot;

	// DRAFT에서는 현재 품목명을 표시하고 접수 이후에는 접수 시점 값을 보존한다.
	@Column(name = "item_name_snapshot", length = 150)
	private String itemNameSnapshot;

	// DRAFT에서는 현재 단위를 표시하고 접수 이후에는 접수 시점 단위 문자열을 보존한다.
	@Column(name = "unit_snapshot", length = 50)
	private String unitSnapshot;

	// 거래처가 주문한 품목 수량을 소수점 셋째 자리까지 저장한다.
	@Column(name = "order_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal orderQuantity;

	// 품목 기본 판매가격 또는 DRAFT에서 입력한 예외 판매 단가를 저장한다.
	@Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal unitPrice;

	// 주문 수량과 판매 단가를 곱하고 소수 둘째 자리로 반올림한 품목 금액을 저장한다.
	@Column(name = "line_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal lineAmount;

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

	// ========== DRAFT 주문 품목과 수량·판매 단가를 생성하고 현재 기준정보를 표시값으로 설정하는 정적 팩토리 메서드 ==========
	public static SalesOrderItem create(SalesOrder salesOrder, int lineNo, Item item,
			BigDecimal orderQuantity, BigDecimal unitPrice) {
		SalesOrderItem salesOrderItem = new SalesOrderItem();
		salesOrderItem.salesOrder = salesOrder;
		salesOrderItem.lineNo = lineNo;
		salesOrderItem.item = item;
		salesOrderItem.orderQuantity = orderQuantity;
		salesOrderItem.unitPrice = unitPrice;
		salesOrderItem.lineAmount = orderQuantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
		salesOrderItem.freezeSnapshot();
		return salesOrderItem;
	}

	// ========== 주문 접수 시점의 품목 코드·품목명·단위를 변경 불가 스냅샷으로 확정하는 메서드 ==========
	public void freezeSnapshot() {
		itemCodeSnapshot = item.getItemCode();
		itemNameSnapshot = item.getItemName();
		unitSnapshot = item.getUnit() == ItemUnit.OTHER ? item.getOtherUnitName() : item.getUnit().name();
	}
}
