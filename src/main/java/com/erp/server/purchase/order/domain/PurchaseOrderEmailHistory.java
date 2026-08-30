package com.erp.server.purchase.order.domain;

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

// ********** Oracle Database의 PURCHASE_ORDER_EMAIL_HISTORY 테이블과 발주서 이메일 전송 시도 결과를 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "PURCHASE_ORDER_EMAIL_HISTORY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrderEmailHistory {

	// Oracle의 SEQ_PURCHASE_ORDER_EMAIL_HISTORY에서 다음 값을 받아 PK로 사용하는 이메일 이력 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchaseOrderEmailHistorySequenceGenerator")
	@SequenceGenerator(name = "purchaseOrderEmailHistorySequenceGenerator", sequenceName = "SEQ_PURCHASE_ORDER_EMAIL_HISTORY", allocationSize = 1)
	@Column(name = "email_history_id", nullable = false)
	private Long emailHistoryId;

	// 이메일을 전송하거나 재전송한 대상 PURCHASE_ORDER 발주를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "purchase_order_id", nullable = false)
	private PurchaseOrder purchaseOrder;

	// 한 발주에서 이메일 전송을 시도한 순서를 1부터 증가시켜 저장한다.
	@Column(name = "attempt_no", nullable = false, precision = 5)
	private Integer attemptNo;

	// 전송 시점의 공급업체 발주 이메일을 보존하여 이후 공급업체 정보 변경과 분리한다.
	@Column(name = "recipient_email", nullable = false, length = 255)
	private String recipientEmail;

	// 개별 이메일 전송 시도의 결과를 SENT 또는 FAILED 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PurchaseOrderEmailStatus status;

	// 전송 실패 시 원인 확인에 사용할 오류 내용을 저장하고 성공 시에는 null로 유지한다.
	@Column(name = "error_message", length = 2000)
	private String errorMessage;

	// 이메일 전송 또는 재전송을 실행한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "attempted_by")
	private AppUser attemptedBy;

	// 이메일 전송을 시도한 일시를 저장한다.
	@Column(name = "attempted_at", nullable = false, updatable = false)
	private LocalDateTime attemptedAt;

	// ========== 신규 Entity가 저장되기 전에 이메일 전송 시도 일시를 설정하는 메서드 ==========
	@PrePersist
	protected void onCreate() {
		attemptedAt = LocalDateTime.now();
	}

	// ========== 발주별 시도 순번과 전송 결과를 이용해 신규 이메일 전송 이력 Entity를 생성하는 정적 팩토리 메서드 ==========
	public static PurchaseOrderEmailHistory create(PurchaseOrder purchaseOrder, Integer attemptNo,
			String recipientEmail, PurchaseOrderEmailStatus status, String errorMessage, AppUser attemptedBy) {
		PurchaseOrderEmailHistory history = new PurchaseOrderEmailHistory();

		history.purchaseOrder = purchaseOrder;
		history.attemptNo = attemptNo;
		history.recipientEmail = recipientEmail;
		history.status = status;
		history.errorMessage = errorMessage;
		history.attemptedBy = attemptedBy;

		return history;
	}
}
