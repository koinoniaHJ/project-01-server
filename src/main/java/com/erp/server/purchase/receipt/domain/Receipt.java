package com.erp.server.purchase.receipt.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.master.warehouse.domain.Warehouse;
import com.erp.server.purchase.order.domain.PurchaseOrder;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ********** Oracle Database의 RECEIPT 테이블과 발주별 입고 검수 상태·창고·잔여 처리·취소 이력을 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "RECEIPT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Receipt {

	// Oracle의 SEQ_RECEIPT에서 다음 값을 받아 PK로 사용하는 입고 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "receiptSequenceGenerator")
	@SequenceGenerator(name = "receiptSequenceGenerator", sequenceName = "SEQ_RECEIPT", allocationSize = 1)
	@Column(name = "receipt_id", nullable = false)
	private Long receiptId;

	// 입고 검수의 원본이 되는 PURCHASE_ORDER 발주를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "purchase_order_id", nullable = false)
	private PurchaseOrder purchaseOrder;

	// 한 입고 건의 모든 정상 수량을 반영할 WAREHOUSE 창고를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;

	// 입고 검수 진행 상태를 PENDING, INSPECTING, COMPLETED 또는 CANCELED 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private ReceiptStatus status = ReceiptStatus.PENDING;

	// 검수 완료 후 발주 잔여 수량을 추가 입고하거나 종료하는 방식을 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "remainder_action", length = 30)
	private ReceiptRemainderAction remainderAction;

	// 발주 잔여 수량이 있을 때 선택한 처리 방식의 업무 사유를 저장한다.
	@Column(name = "remainder_reason", length = 1000)
	private String remainderReason;

	// PENDING 입고의 검수를 시작한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inspection_started_by")
	private AppUser inspectionStartedBy;

	// 입고 검수를 시작하여 INSPECTING 상태로 전환한 일시를 저장한다.
	@Column(name = "inspection_started_at")
	private LocalDateTime inspectionStartedAt;

	// 검수 결과를 확정하고 재고·매입 전표를 반영한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "completed_by")
	private AppUser completedBy;

	// 입고 검수 완료 일시를 저장한다.
	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	// 완료 전 입고 검수를 취소한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "canceled_by")
	private AppUser canceledBy;

	// 입고 검수 취소 일시를 저장한다.
	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	// 완료 전 입고 검수를 취소한 필수 업무 사유를 저장한다.
	@Column(name = "cancel_reason", length = 1000)
	private String cancelReason;

	// 입고를 최초 등록한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private AppUser createdBy;

	// 입고 최초 등록 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 입고 창고·검수 결과·상태가 마지막으로 변경된 일시를 저장한다.
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// 조회 당시 값과 DB 값을 비교하여 창고 수정·검수 저장·상태 전이의 동시 처리 충돌을 확인한다.
	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	// 입고에 포함된 RECEIPT_ITEM 품목을 관리하며 입고와 생명주기를 함께 유지한다.
	@OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReceiptItem> items = new ArrayList<>();

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

	// ========== 발주·입고 창고·등록 사용자를 이용해 PENDING 상태의 신규 입고 Entity를 생성하는 정적 팩토리 메서드 ==========
	public static Receipt create(PurchaseOrder purchaseOrder, Warehouse warehouse, AppUser createdBy) {
		Receipt receipt = new Receipt();
		receipt.purchaseOrder = purchaseOrder;
		receipt.warehouse = warehouse;
		receipt.status = ReceiptStatus.PENDING;
		receipt.createdBy = createdBy;
		return receipt;
	}

	// ========== 원본 발주 품목에 대응하는 입고 품목을 추가하는 메서드 ==========
	public void addItem(ReceiptItem item) {
		items.add(item);
	}

	// ========== PENDING 상태에서 실제 입고 수량을 반영할 창고를 변경하는 메서드 ==========
	public void changeWarehouse(Warehouse warehouse) {
		this.warehouse = warehouse;
	}

	// ========== PENDING 입고를 INSPECTING으로 변경하고 검수 시작 사용자와 일시를 기록하는 메서드 ==========
	public void startInspection(AppUser inspectionStartedBy) {
		this.status = ReceiptStatus.INSPECTING;
		this.inspectionStartedBy = inspectionStartedBy;
		this.inspectionStartedAt = LocalDateTime.now();
	}

	// ========== 자식 입고 품목의 검수 결과가 저장되었음을 상위 입고의 수정 일시와 version에 함께 반영하는 메서드 ==========
	public void markInspectionSaved() {
		this.updatedAt = LocalDateTime.now();
	}

	// ========== 검수 완료 결과와 잔여 처리 내용을 기록하고 입고를 COMPLETED 상태로 변경하는 메서드 ==========
	public void complete(ReceiptRemainderAction remainderAction, String remainderReason, AppUser completedBy) {
		this.status = ReceiptStatus.COMPLETED;
		this.remainderAction = remainderAction;
		this.remainderReason = remainderReason;
		this.completedBy = completedBy;
		this.completedAt = LocalDateTime.now();
	}

	// ========== PENDING 또는 INSPECTING 입고를 CANCELED로 변경하고 취소 사용자·일시·사유를 기록하는 메서드 ==========
	public void cancel(AppUser canceledBy, String cancelReason) {
		this.status = ReceiptStatus.CANCELED;
		this.canceledBy = canceledBy;
		this.canceledAt = LocalDateTime.now();
		this.cancelReason = cancelReason;
	}
}
