package com.erp.server.master.customer.domain;

import java.time.LocalDateTime;

import com.erp.server.common.user.domain.AppUser;

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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ********** Oracle Database의 CUSTOMER_TRADE_STATUS_HISTORY 테이블과 거래처 거래 상태 변경 이력을 매핑하기 위한 Entity 클래스 **********
// 거래처 상태가 NORMAL과 HOLD 사이에서 변경될 때 변경 전·후 상태, 사유, 처리자, 처리 일시를 보존
@Entity
@Table(name = "CUSTOMER_TRADE_STATUS_HISTORY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerTradeStatusHistory {

	// CUSTOMER_TRADE_STATUS_HISTORY.trade_status_history_id 컬럼과 매핑하며 SEQ_CUSTOMER_TRADE_STATUS_HISTORY에서 다음 값을 받아 PK로 사용한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customerTradeStatusHistorySequenceGenerator")
	@SequenceGenerator(
			name = "customerTradeStatusHistorySequenceGenerator",
			sequenceName = "SEQ_CUSTOMER_TRADE_STATUS_HISTORY",
			allocationSize = 1
	)
	@Column(name = "trade_status_history_id", nullable = false)
	private Long tradeStatusHistoryId;

	// CUSTOMER_TRADE_STATUS_HISTORY.customer_id 컬럼과 매핑하며 거래 상태가 변경된 CUSTOMER 거래처를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	// CUSTOMER_TRADE_STATUS_HISTORY.previous_status 컬럼과 매핑하며 변경 전 거래 상태를 NORMAL 또는 HOLD 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "previous_status", nullable = false, length = 20)
	private CustomerTradeStatus previousStatus;

	// CUSTOMER_TRADE_STATUS_HISTORY.changed_status 컬럼과 매핑하며 변경 후 거래 상태를 NORMAL 또는 HOLD 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "changed_status", nullable = false, length = 20)
	private CustomerTradeStatus changedStatus;

	// CUSTOMER_TRADE_STATUS_HISTORY.reason 컬럼과 매핑하며 거래 상태를 수동으로 변경한 사유를 저장한다.
	@Column(name = "reason", nullable = false, length = 1000)
	private String reason;

	// CUSTOMER_TRADE_STATUS_HISTORY.changed_by 컬럼과 매핑하며 거래 상태를 변경한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "changed_by", nullable = false)
	private AppUser changedBy;

	// CUSTOMER_TRADE_STATUS_HISTORY.changed_at 컬럼과 매핑하며 거래 상태를 변경한 일시를 저장한다.
	@Column(name = "changed_at", nullable = false, updatable = false)
	private LocalDateTime changedAt;

	// ========== 신규 거래 상태 이력이 저장되기 전에 변경 일시를 설정하는 메서드 ==========
	@PrePersist
	protected void onCreate() {
		changedAt = LocalDateTime.now();
	}

	// ========== 신규 거래처 거래 상태 변경 이력을 생성하는 정적 팩토리 메서드 ==========
	public static CustomerTradeStatusHistory create(Customer customer, CustomerTradeStatus previousStatus,
			CustomerTradeStatus changedStatus, String reason, AppUser changedBy) {

		CustomerTradeStatusHistory history = new CustomerTradeStatusHistory();

		history.customer = customer;
		history.previousStatus = previousStatus;
		history.changedStatus = changedStatus;
		history.reason = reason;
		history.changedBy = changedBy;

		return history;
	}
}