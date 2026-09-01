package com.erp.server.sales.shipment.domain;

import java.time.LocalDateTime;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.master.warehouse.domain.Warehouse;
import com.erp.server.sales.order.domain.SalesOrder;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ********** Oracle Database의 SHIPMENT 테이블과 주문별 단일 출고의 대기·포장·완료·취소 상태를 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "SHIPMENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shipment {

	// Oracle의 SEQ_SHIPMENT에서 다음 값을 받아 PK로 사용하는 출고 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shipmentSequenceGenerator")
	@SequenceGenerator(name = "shipmentSequenceGenerator", sequenceName = "SEQ_SHIPMENT", allocationSize = 1)
	@Column(name = "shipment_id", nullable = false)
	private Long shipmentId;

	// 주문 접수 시 1:1로 연결되는 SALES_ORDER 주문을 참조한다.
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_order_id", nullable = false)
	private SalesOrder salesOrder;

	// 포장 업무에서 전량 출고할 WAREHOUSE 창고를 참조하며 PENDING 생성 시에는 null이다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "warehouse_id")
	private Warehouse warehouse;

	// 출고 진행 상태를 PENDING, PACKED, COMPLETED 또는 CANCELED 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private ShipmentStatus status = ShipmentStatus.PENDING;

	// 포장 취소 후 재포장할 때마다 증가하는 포장 회차를 저장한다.
	@Column(name = "packing_sequence", nullable = false)
	private Integer packingSequence = 0;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "packed_by")
	private AppUser packedBy;

	@Column(name = "packed_at")
	private LocalDateTime packedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "completed_by")
	private AppUser completedBy;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	// 주문 취소 또는 출고 취소를 처리한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "canceled_by")
	private AppUser canceledBy;

	// 연결 주문과 출고가 취소된 일시를 저장한다.
	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// 조회 당시 값과 DB 값을 비교하여 포장·완료·취소의 동시 처리 충돌을 확인한다.
	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
		packingSequence = packingSequence == null ? 0 : packingSequence;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	// ========== REGISTERED 주문과 1:1로 연결되는 PENDING 출고 대기 건을 생성하는 정적 팩토리 메서드 ==========
	// 주문 접수 시에는 창고·LOT를 선택하거나 재고를 예약하지 않고 이후 출고 포장 단계에서 처리한다.
	public static Shipment createPending(SalesOrder salesOrder) {
		Shipment shipment = new Shipment();
		shipment.salesOrder = salesOrder;
		shipment.status = ShipmentStatus.PENDING;
		shipment.packingSequence = 0;
		return shipment;
	}

	// ========== PENDING 출고의 단일 출고 창고를 지정하고 포장안 저장에 따른 version 변경을 발생시키는 메서드 ==========
	// SHIPMENT_LOT에는 별도 version이 없으므로 같은 창고에서 LOT 배정만 바뀌어도 updatedAt을 명시적으로 변경한다.
	public void savePackingPlan(Warehouse warehouse) {
		this.warehouse = warehouse;
		updatedAt = LocalDateTime.now();
	}

	// ========== PENDING 출고의 현재 포장안을 확정하고 포장 회차·처리자·처리 일시를 기록하는 메서드 ==========
	public void pack(AppUser packedBy) {
		status = ShipmentStatus.PACKED;
		packingSequence += 1;
		this.packedBy = packedBy;
		packedAt = LocalDateTime.now();
	}

	// ========== PACKED 출고의 재고 예약을 해제한 뒤 다시 포장 가능한 PENDING 상태로 되돌리는 메서드 ==========
	// 최근 배정안은 재포장 편의를 위해 SHIPMENT_LOT에 남기고 실제 예약 여부만 ShipmentLot에서 해제한다.
	public void unpack() {
		status = ShipmentStatus.PENDING;
		packedBy = null;
		packedAt = null;
	}

	// ========== PACKED 출고를 COMPLETED 상태로 변경하고 실제 인계 처리자와 완료 일시를 기록하는 메서드 ==========
	public void complete(AppUser completedBy) {
		status = ShipmentStatus.COMPLETED;
		this.completedBy = completedBy;
		completedAt = LocalDateTime.now();
	}

	// ========== PENDING 또는 PACKED 출고를 CANCELED 상태로 변경하고 처리 이력을 기록하는 메서드 ==========
	public void cancel(AppUser canceledBy) {
		status = ShipmentStatus.CANCELED;
		this.canceledBy = canceledBy;
		canceledAt = LocalDateTime.now();
	}
}
