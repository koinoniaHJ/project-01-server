package com.erp.server.master.item.domain;

import java.time.LocalDateTime;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.master.supplier.domain.Supplier;

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

// ********** Oracle Database의 SUPPLIER_ITEM 테이블과 품목별 취급 공급업체 관계를 Java 객체로 매핑하기 위한 Entity 클래스 **********
@Entity
@Table(name = "SUPPLIER_ITEM")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplierItem {

	// Oracle의 SEQ_SUPPLIER_ITEM에서 다음 값을 받아 PK로 사용하는 취급 공급업체 관계 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "supplierItemSequenceGenerator")
	@SequenceGenerator(name = "supplierItemSequenceGenerator", sequenceName = "SEQ_SUPPLIER_ITEM", allocationSize = 1)
	@Column(name = "supplier_item_id", nullable = false)
	private Long supplierItemId;

	// 품목을 취급하는 SUPPLIER 공급업체를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id", nullable = false)
	private Supplier supplier;

	// 취급 공급업체 관계의 대상 ITEM 품목을 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	// 품목과 공급업체의 취급 관계를 등록한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private AppUser createdBy;

	// 품목과 공급업체의 취급 관계를 등록한 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// ========== 신규 Entity가 저장되기 전에 관계 등록 일시를 설정하는 메서드 ==========
	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}

	// ========== 품목과 공급업체의 신규 취급 관계 Entity를 생성하는 정적 팩토리 메서드 ==========
	public static SupplierItem create(Supplier supplier, Item item, AppUser createdBy) {

		SupplierItem supplierItem = new SupplierItem();

		supplierItem.supplier = supplier;
		supplierItem.item = item;
		supplierItem.createdBy = createdBy;

		return supplierItem;
	}
}
