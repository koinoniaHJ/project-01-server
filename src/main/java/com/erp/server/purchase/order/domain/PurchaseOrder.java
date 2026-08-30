package com.erp.server.purchase.order.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.master.supplier.domain.Supplier;

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

// ********** Oracle Database의 PURCHASE_ORDER 테이블과 발주 기본정보·상태·처리 이력을 Java 객체로 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "PURCHASE_ORDER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrder {

	// Oracle의 SEQ_PURCHASE_ORDER에서 다음 값을 받아 PK로 사용하는 발주 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchaseOrderSequenceGenerator")
	@SequenceGenerator(name = "purchaseOrderSequenceGenerator", sequenceName = "SEQ_PURCHASE_ORDER", allocationSize = 1)
	@Column(name = "purchase_order_id", nullable = false)
	private Long purchaseOrderId;

	// 한 발주서에서 모든 품목을 공급할 SUPPLIER 공급업체를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id", nullable = false)
	private Supplier supplier;

	// 발주 진행 상태를 DRAFT, SUBMITTED, APPROVED, ORDERED, CANCELED, RECEIVED 또는 CLOSED 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

	// 발주 확정 전에는 null이고 이메일 전송 시도 후 SENT 또는 FAILED 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "email_status", length = 20)
	private PurchaseOrderEmailStatus emailStatus;

	// 모든 발주 품목의 발주 금액을 합산한 발주 총액을 저장한다.
	@Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	// 발주 작성과 후속 처리에서 참고할 특이사항을 저장한다.
	@Column(name = "memo", length = 2000)
	private String memo;

	// 발주를 승인 요청 상태로 변경한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "submitted_by")
	private AppUser submittedBy;

	// 발주 승인 요청 일시를 저장한다.
	@Column(name = "submitted_at")
	private LocalDateTime submittedAt;

	// 승인 대기 발주를 승인한 ADMIN 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approved_by")
	private AppUser approvedBy;

	// 발주 승인 완료 일시를 저장한다.
	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	// 승인 완료 발주를 공급업체 발주로 확정한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ordered_by")
	private AppUser orderedBy;

	// 공급업체 발주 확정 일시를 저장한다.
	@Column(name = "ordered_at")
	private LocalDateTime orderedAt;

	// 허용된 상태의 발주를 취소한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "canceled_by")
	private AppUser canceledBy;

	// 발주 취소 일시를 저장한다.
	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	// 발주를 취소한 업무 사유를 저장한다.
	@Column(name = "cancel_reason", length = 1000)
	private String cancelReason;

	// 일부 입고 후 잔여 미입고 수량을 종료한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "closed_by")
	private AppUser closedBy;

	// 잔여 미입고 종료 일시를 저장한다.
	@Column(name = "closed_at")
	private LocalDateTime closedAt;

	// 잔여 미입고 수량을 종료한 업무 사유를 저장한다.
	@Column(name = "close_reason", length = 1000)
	private String closeReason;

	// 이메일 전송 성공 후 공급업체와 취소를 확인한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_cancel_confirmed_by")
	private AppUser supplierCancelConfirmedBy;

	// 공급업체 취소 확인 일시를 저장한다.
	@Column(name = "supplier_cancel_confirmed_at")
	private LocalDateTime supplierCancelConfirmedAt;

	// 발주를 최초 작성한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private AppUser createdBy;

	// 발주 최초 등록 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 발주 내용 또는 상태가 마지막으로 변경된 일시를 저장한다.
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// 조회 당시 값과 DB 값을 비교하여 발주 수정과 상태 전이의 동시 처리 충돌을 확인한다.
	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	// 발주에 포함된 PURCHASE_ORDER_ITEM 품목을 관리하며 작성 중 제거된 품목은 DB에서도 함께 삭제한다.
	@OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PurchaseOrderItem> items = new ArrayList<>();

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

	// ========== 공급업체·메모·작성자를 이용해 DRAFT 상태의 신규 발주 Entity를 생성하는 정적 팩토리 메서드 ==========
	public static PurchaseOrder create(Supplier supplier, String memo, AppUser createdBy) {
		PurchaseOrder purchaseOrder = new PurchaseOrder();

		purchaseOrder.supplier = supplier;
		purchaseOrder.status = PurchaseOrderStatus.DRAFT;
		purchaseOrder.emailStatus = null;
		purchaseOrder.totalAmount = BigDecimal.ZERO;
		purchaseOrder.memo = memo;
		purchaseOrder.createdBy = createdBy;

		return purchaseOrder;
	}

	// ========== 발주 품목을 추가하고 최신 품목 금액 합계로 발주 총액을 다시 계산하는 메서드 ==========
	public void addItem(PurchaseOrderItem item) {
		items.add(item);
		recalculateTotalAmount();
	}

	// ========== 작성 중 발주의 기존 품목을 모두 제거하고 발주 총액을 초기화하는 메서드 ==========
	public void clearItems() {
		items.clear();
		totalAmount = BigDecimal.ZERO;
	}

	// ========== DRAFT 발주의 공급업체와 메모를 변경하는 메서드 ==========
	// 발주 품목은 Service에서 기존 자식 행 삭제를 먼저 반영한 뒤 최신 요청 순서대로 다시 추가한다.
	public void updateDraft(Supplier supplier, String memo) {
		this.supplier = supplier;
		this.memo = memo;
	}

	// ========== DRAFT 발주를 SUBMITTED 승인 대기 상태로 변경하고 요청자와 요청 일시를 기록하는 메서드 ==========
	public void submit(AppUser submittedBy) {
		this.status = PurchaseOrderStatus.SUBMITTED;
		this.submittedBy = submittedBy;
		this.submittedAt = LocalDateTime.now();
	}

	// ========== SUBMITTED 발주를 APPROVED 승인 완료 상태로 변경하고 승인자와 승인 일시를 기록하는 메서드 ==========
	public void approve(AppUser approvedBy) {
		this.status = PurchaseOrderStatus.APPROVED;
		this.approvedBy = approvedBy;
		this.approvedAt = LocalDateTime.now();
	}

	// ========== APPROVED 발주를 ORDERED 발주 확정 상태로 변경하고 확정자와 확정 일시를 기록하는 메서드 ==========
	// 이메일 상태는 다음 외부 전송 단계에서 실제 결과가 확인된 후 SENT 또는 FAILED로 별도 변경한다.
	public void order(AppUser orderedBy) {
		this.status = PurchaseOrderStatus.ORDERED;
		this.orderedBy = orderedBy;
		this.orderedAt = LocalDateTime.now();
	}

	// ========== 발주 상태와 별도로 최근 발주서 이메일 전송 결과를 SENT 또는 FAILED로 변경하는 메서드 ==========
	// 이메일 실패가 발생해도 ORDERED 발주 상태는 유지하고 재전송할 수 있게 한다.
	public void changeEmailStatus(PurchaseOrderEmailStatus emailStatus) {
		this.emailStatus = emailStatus;
	}

	// ========== 허용된 상태의 발주를 CANCELED로 변경하고 취소 처리 내용을 기록하는 메서드 ==========
	// ORDERED 발주에서 공급업체 취소 확인이 완료된 경우에만 확인 사용자와 확인 일시를 함께 저장한다.
	public void cancel(AppUser canceledBy, String cancelReason, boolean supplierCancelConfirmed) {
		LocalDateTime now = LocalDateTime.now();

		this.status = PurchaseOrderStatus.CANCELED;
		this.canceledBy = canceledBy;
		this.canceledAt = now;
		this.cancelReason = cancelReason;

		if (supplierCancelConfirmed) {
			this.supplierCancelConfirmedBy = canceledBy;
			this.supplierCancelConfirmedAt = now;
		}
	}

	// ========== 모든 발주 품목이 전량 정상 입고되었을 때 ORDERED 발주를 RECEIVED 상태로 변경하는 메서드 ==========
	public void completeReceipt() {
		this.status = PurchaseOrderStatus.RECEIVED;
	}

	// ========== 일부 정상 입고 후 잔여 미입고 수량을 종료하고 발주를 CLOSED 상태로 변경하는 메서드 ==========
	public void closeRemainder(AppUser closedBy, String closeReason) {
		this.status = PurchaseOrderStatus.CLOSED;
		this.closedBy = closedBy;
		this.closedAt = LocalDateTime.now();
		this.closeReason = closeReason;
	}

	// ========== 모든 발주 품목 금액의 합계를 발주 총액으로 저장하는 메서드 ==========
	private void recalculateTotalAmount() {
		totalAmount = items.stream().map(PurchaseOrderItem::getLineAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
