package com.erp.server.settlement.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.master.customer.domain.Customer;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ********** Oracle Database의 PAYMENT 테이블과 거래처 입금·미배분 금액·취소 이력을 Java 객체로 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "PAYMENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

	// Oracle의 SEQ_PAYMENT에서 다음 값을 받아 PK로 사용하는 입금 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "paymentSequenceGenerator")
	@SequenceGenerator(name = "paymentSequenceGenerator", sequenceName = "SEQ_PAYMENT", allocationSize = 1)
	@Column(name = "payment_id", nullable = false)
	private Long paymentId;

	// 입금액을 미결 매출 전표에 배분할 CUSTOMER 거래처를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	// 실제 거래처 입금이 발생한 일자를 저장한다.
	@Column(name = "payment_date", nullable = false)
	private LocalDate paymentDate;

	// 거래처가 입금한 전체 금액을 저장하며 등록 이후 수정하지 않는다.
	@Column(name = "amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	// 입금 방법을 BANK_TRANSFER, CASH, CARD 또는 OTHER 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "method", nullable = false, length = 30)
	private PaymentMethod method;

	// 입금 확인과 정산에서 참고할 내용을 저장한다.
	@Column(name = "memo", length = 2000)
	private String memo;

	// 정산에 사용할 수 있는 입금인지 ACTIVE 또는 CANCELED 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PaymentStatus status = PaymentStatus.ACTIVE;

	// 아직 미결 SALES 전표에 연결되지 않아 이후 자동 배분할 수 있는 입금 잔액을 저장한다.
	@Column(name = "unallocated_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal unallocatedAmount = BigDecimal.ZERO;

	// 입금을 최초 등록한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private AppUser createdBy;

	// 입금이 최초 등록된 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// ACTIVE 입금을 CANCELED로 처리한 APP_USER 사용자를 참조하며 활성 상태에서는 null이다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "canceled_by")
	private AppUser canceledBy;

	// 입금 취소 처리 일시를 저장하며 활성 상태에서는 null이다.
	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	// 입금을 취소한 업무 사유를 저장하며 활성 상태에서는 null이다.
	@Column(name = "cancel_reason", length = 1000)
	private String cancelReason;

	// 조회 당시 값과 DB 값을 비교하여 입금 배분·취소의 동시 처리 충돌을 확인한다.
	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	// ========== 신규 입금이 저장되기 전에 등록 일시와 초기 상태를 설정하는 메서드 ==========
	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		status = status == null ? PaymentStatus.ACTIVE : status;
		unallocatedAmount = unallocatedAmount == null ? amount : unallocatedAmount;
	}

	// ========== 거래처·입금일·금액·방법·메모·등록자를 이용해 전액 미배분 상태의 신규 입금을 생성하는 정적 팩토리 메서드 ==========
	public static Payment create(Customer customer, LocalDate paymentDate, BigDecimal amount,
			PaymentMethod method, String memo, AppUser createdBy) {

		Payment payment = new Payment();
		payment.customer = customer;
		payment.paymentDate = paymentDate;
		payment.amount = amount;
		payment.method = method;
		payment.memo = memo;
		payment.status = PaymentStatus.ACTIVE;
		payment.unallocatedAmount = amount;
		payment.createdBy = createdBy;
		return payment;
	}

	// ========== 미결 SALES 전표에 배분한 금액만큼 미배분 입금액을 감소시키는 메서드 ==========
	public void allocate(BigDecimal allocatedAmount) {
		unallocatedAmount = unallocatedAmount.subtract(allocatedAmount);
	}

	// ========== 매출 반품 또는 입금 배분 해제로 회수한 금액만큼 미배분 입금액을 증가시키는 메서드 ==========
	public void restoreUnallocatedAmount(BigDecimal releasedAmount) {
		unallocatedAmount = unallocatedAmount.add(releasedAmount);
	}

	// ========== ACTIVE 입금을 CANCELED로 변경하고 처리자·일시·사유와 미배분 금액 0을 기록하는 메서드 ==========
	// 연결된 활성 배분은 Service에서 먼저 모두 해제한 뒤 이 메서드를 호출한다.
	public void cancel(AppUser canceledBy, String cancelReason) {
		status = PaymentStatus.CANCELED;
		this.canceledBy = canceledBy;
		canceledAt = LocalDateTime.now();
		this.cancelReason = cancelReason;
		unallocatedAmount = BigDecimal.ZERO;
	}

	// ========== 미결 매출 전표에 자동 배분할 수 있는 ACTIVE 상태의 미배분 입금인지 확인하는 메서드 ==========
	public boolean hasUnallocatedAmount() {
		return status == PaymentStatus.ACTIVE && unallocatedAmount.signum() > 0;
	}
}
