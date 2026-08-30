package com.erp.server.settlement.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.erp.server.common.user.domain.AppUser;

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

// ********** Oracle Database의 PAYMENT_ALLOCATION 테이블과 입금·매출 전표 간 자동 배분 및 해제 이력을 Java 객체로 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "PAYMENT_ALLOCATION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAllocation {

	// Oracle의 SEQ_PAYMENT_ALLOCATION에서 다음 값을 받아 PK로 사용하는 입금 배분 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "paymentAllocationSequenceGenerator")
	@SequenceGenerator(name = "paymentAllocationSequenceGenerator", sequenceName = "SEQ_PAYMENT_ALLOCATION", allocationSize = 1)
	@Column(name = "payment_allocation_id", nullable = false)
	private Long paymentAllocationId;

	// 미배분 금액을 제공한 PAYMENT 입금을 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "payment_id", nullable = false)
	private Payment payment;

	// 입금액이 연결되어 미수 잔액이 감소한 SALES VOUCHER 전표를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "voucher_id", nullable = false)
	private Voucher voucher;

	// 해당 입금에서 해당 매출 전표에 자동 배분된 금액을 저장한다.
	@Column(name = "allocated_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal allocatedAmount;

	// 입금 등록·신규 매출·매출 반품 등의 자동 배분을 발생시킨 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "allocated_by")
	private AppUser allocatedBy;

	// 입금액이 매출 전표에 자동 배분된 일시를 저장한다.
	@Column(name = "allocated_at", nullable = false, updatable = false)
	private LocalDateTime allocatedAt;

	// 입금 취소 또는 매출 반품으로 유효 배분을 해제한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "released_by")
	private AppUser releasedBy;

	// 배분이 해제되어 정산 계산에서 제외되기 시작한 일시를 저장한다.
	@Column(name = "released_at")
	private LocalDateTime releasedAt;

	// 입금 취소 또는 매출 반품으로 배분을 해제한 사유를 저장한다.
	@Column(name = "release_reason", length = 1000)
	private String releaseReason;

	// ========== 신규 입금 배분이 저장되기 전에 배분 일시를 설정하는 메서드 ==========
	@PrePersist
	protected void onCreate() {
		allocatedAt = allocatedAt == null ? LocalDateTime.now() : allocatedAt;
	}

	// ========== PAYMENT의 미배분 금액을 SALES VOUCHER에 연결하는 유효 배분을 생성하는 정적 팩토리 메서드 ==========
	public static PaymentAllocation create(Payment payment, Voucher voucher, BigDecimal allocatedAmount,
			AppUser allocatedBy) {

		PaymentAllocation allocation = new PaymentAllocation();
		allocation.payment = payment;
		allocation.voucher = voucher;
		allocation.allocatedAmount = allocatedAmount;
		allocation.allocatedBy = allocatedBy;
		return allocation;
	}

	// ========== 활성 배분 전액을 해제하고 처리자·일시·사유를 기록하는 메서드 ==========
	public void release(AppUser releasedBy, String releaseReason) {
		this.releasedBy = releasedBy;
		releasedAt = LocalDateTime.now();
		this.releaseReason = releaseReason;
	}

	// ========== 활성 배분 일부를 유지하고 해제 금액만 별도 이력 행으로 분리하는 메서드 ==========
	// 원래 배분 행은 유지 금액으로 줄이고 반환된 행에는 기존 배분 시점과 해제 정보를 함께 보존한다.
	public PaymentAllocation splitReleasedAmount(BigDecimal releasedAmount, AppUser releasedBy,
			String releaseReason) {

		allocatedAmount = allocatedAmount.subtract(releasedAmount);

		PaymentAllocation releasedAllocation = new PaymentAllocation();
		releasedAllocation.payment = payment;
		releasedAllocation.voucher = voucher;
		releasedAllocation.allocatedAmount = releasedAmount;
		releasedAllocation.allocatedBy = allocatedBy;
		releasedAllocation.allocatedAt = allocatedAt;
		releasedAllocation.releasedBy = releasedBy;
		releasedAllocation.releasedAt = LocalDateTime.now();
		releasedAllocation.releaseReason = releaseReason;

		return releasedAllocation;
	}

	// ========== released_at이 없어 현재 전표 정산에 포함되는 유효 배분인지 확인하는 메서드 ==========
	public boolean isActive() {
		return releasedAt == null;
	}
}
