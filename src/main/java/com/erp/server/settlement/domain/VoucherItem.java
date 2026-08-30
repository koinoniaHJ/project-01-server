package com.erp.server.settlement.domain;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ********** Oracle Database의 VOUCHER_ITEM 테이블과 전표 생성 시점의 품목·단위·수량·단가·금액 스냅샷을 Java 객체로 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "VOUCHER_ITEM")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoucherItem {

	// Oracle의 SEQ_VOUCHER_ITEM에서 다음 값을 받아 PK로 사용하는 전표 품목 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "voucherItemSequenceGenerator")
	@SequenceGenerator(name = "voucherItemSequenceGenerator", sequenceName = "SEQ_VOUCHER_ITEM", allocationSize = 1)
	@Column(name = "voucher_item_id", nullable = false)
	private Long voucherItemId;

	// 품목 스냅샷이 포함된 상위 VOUCHER 전표를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "voucher_id", nullable = false)
	private Voucher voucher;

	// 전표 안에서 품목을 표시하는 1부터 시작하는 순번을 저장한다.
	@Column(name = "line_no", nullable = false, precision = 5)
	private Integer lineNo;

	// 전표 원본 업무에 포함된 ITEM 품목을 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	// 기준정보가 이후 변경되어도 전표 내용을 보존하도록 생성 시점의 품목명을 저장한다.
	@Column(name = "item_name_snapshot", nullable = false, length = 150)
	private String itemNameSnapshot;

	// 생성 시점의 품목 기준 단위를 저장하며 OTHER 단위는 실제 기타 단위명을 저장한다.
	@Column(name = "unit_snapshot", nullable = false, length = 50)
	private String unitSnapshot;

	// 원본 입고·반품·출고 업무에서 확정된 전표 품목 수량을 양수로 저장한다.
	@Column(name = "quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal quantity;

	// 원본 업무에서 확정된 매입 또는 판매 단가를 저장한다.
	@Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal unitPrice;

	// 수량과 단가를 곱하여 반올림한 금액을 저장하며 반품 전표에서는 음수로 저장한다.
	@Column(name = "line_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal lineAmount;

	// 원본 완료 업무에서 전표 품목 스냅샷이 생성된 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// ========== 신규 전표 품목 스냅샷이 저장되기 전에 생성 일시를 설정하는 메서드 ==========
	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}

	// ========== 전표·순번·품목·수량·단가로 생성 시점 정보를 보존하는 신규 전표 품목을 생성하는 정적 팩토리 메서드 ==========
	public static VoucherItem create(Voucher voucher, Integer lineNo, Item item, BigDecimal quantity,
			BigDecimal unitPrice) {

		VoucherItem voucherItem = new VoucherItem();
		BigDecimal calculatedAmount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

		voucherItem.voucher = voucher;
		voucherItem.lineNo = lineNo;
		voucherItem.item = item;
		voucherItem.itemNameSnapshot = item.getItemName();
		voucherItem.unitSnapshot = createUnitSnapshot(item);
		voucherItem.quantity = quantity;
		voucherItem.unitPrice = unitPrice;
		voucherItem.lineAmount = voucher.getType().isReturnType() ? calculatedAmount.negate() : calculatedAmount;

		return voucherItem;
	}

	// ========== OTHER 품목 단위는 기타 단위명을, 나머지는 Enum 이름을 전표 단위 스냅샷으로 반환하는 메서드 ==========
	private static String createUnitSnapshot(Item item) {
		return item.getUnit() == ItemUnit.OTHER ? item.getOtherUnitName() : item.getUnit().name();
	}
}
