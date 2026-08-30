package com.erp.server.purchase.returning.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.purchase.receipt.domain.Receipt;

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

// ********** Oracle Database의 PURCHASE_RETURN 테이블과 완료 입고 기준 매입 반품·금액·처리 이력을 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "PURCHASE_RETURN")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseReturn {

	// Oracle의 SEQ_PURCHASE_RETURN에서 다음 값을 받아 PK로 사용하는 매입 반품 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchaseReturnSequenceGenerator")
	@SequenceGenerator(name = "purchaseReturnSequenceGenerator", sequenceName = "SEQ_PURCHASE_RETURN", allocationSize = 1)
	@Column(name = "purchase_return_id", nullable = false)
	private Long purchaseReturnId;

	// 반품 품목·원본 LOT·창고·공급업체·매입 단가의 기준이 되는 완료 RECEIPT 입고를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "receipt_id", nullable = false)
	private Receipt receipt;

	// 매입 반품 진행 상태를 REGISTERED, COMPLETED 또는 CANCELED 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private PurchaseReturnStatus status = PurchaseReturnStatus.REGISTERED;

	// 공급업체에 품목을 반품하는 필수 업무 사유를 저장한다.
	@Column(name = "reason", nullable = false, length = 1000)
	private String reason;

	// 반품 LOT별 수량과 원본 발주 단가를 곱한 품목 금액의 양수 합계를 저장한다.
	@Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	// REGISTERED 반품을 재고·변동 이력·전표에 반영하여 완료한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "completed_by")
	private AppUser completedBy;

	// 매입 반품 재고와 전표 반영을 완료한 일시를 저장한다.
	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	// 완료 전에 매입 반품을 취소한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "canceled_by")
	private AppUser canceledBy;

	// 매입 반품을 취소한 일시를 저장한다.
	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	// REGISTERED 매입 반품을 취소한 필수 업무 사유를 저장한다.
	@Column(name = "cancel_reason", length = 1000)
	private String cancelReason;

	// 매입 반품을 최초 등록한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private AppUser createdBy;

	// 매입 반품 최초 등록 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 반품 사유·품목·상태가 마지막으로 변경된 일시를 저장한다.
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// 조회 당시 값과 DB 값을 비교하여 수정·완료·취소의 동시 처리 충돌을 확인한다.
	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	// 매입 반품에 포함된 PURCHASE_RETURN_ITEM 품목을 상위 반품과 같은 생명주기로 관리한다.
	@OneToMany(mappedBy = "purchaseReturn", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PurchaseReturnItem> items = new ArrayList<>();

	// ========== 신규 Entity 저장 전에 등록·수정 일시와 금액 기본값을 설정하는 메서드 ==========
	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
		totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
	}

	// ========== 기존 Entity 수정 전에 최근 수정 일시를 갱신하는 메서드 ==========
	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	// ========== 완료 입고·반품 사유·등록 사용자를 이용해 REGISTERED 매입 반품을 생성하는 정적 팩토리 메서드 ==========
	public static PurchaseReturn create(Receipt receipt, String reason, AppUser createdBy) {
		PurchaseReturn purchaseReturn = new PurchaseReturn();
		purchaseReturn.receipt = receipt;
		purchaseReturn.status = PurchaseReturnStatus.REGISTERED;
		purchaseReturn.reason = reason;
		purchaseReturn.totalAmount = BigDecimal.ZERO;
		purchaseReturn.createdBy = createdBy;
		return purchaseReturn;
	}

	// ========== REGISTERED 반품의 사유를 변경하는 메서드 ==========
	public void updateReason(String reason) {
		this.reason = reason;
	}

	// ========== 수정 요청에서 기존 반품 품목을 제거하고 총액을 0으로 초기화하는 메서드 ==========
	public void clearItems() {
		items.clear();
		totalAmount = BigDecimal.ZERO;
	}

	// ========== 원본 LOT별 반품 품목을 추가하고 최신 품목 금액 합계로 반품 총액을 다시 계산하는 메서드 ==========
	public void addItem(PurchaseReturnItem item) {
		items.add(item);
		totalAmount = items.stream().map(PurchaseReturnItem::getLineAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	// ========== 재고 감소·변동 이력·매입 반품 전표 반영 후 COMPLETED 상태와 처리 이력을 기록하는 메서드 ==========
	public void complete(AppUser completedBy) {
		status = PurchaseReturnStatus.COMPLETED;
		this.completedBy = completedBy;
		completedAt = LocalDateTime.now();
	}

	// ========== 완료 전 REGISTERED 반품을 사유와 함께 CANCELED 상태로 변경하는 메서드 ==========
	public void cancel(AppUser canceledBy, String cancelReason) {
		status = PurchaseReturnStatus.CANCELED;
		this.canceledBy = canceledBy;
		canceledAt = LocalDateTime.now();
		this.cancelReason = cancelReason;
	}
}
