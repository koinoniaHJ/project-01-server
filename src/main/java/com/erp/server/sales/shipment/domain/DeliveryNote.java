package com.erp.server.sales.shipment.domain;

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

// ********** Oracle Database의 DELIVERY_NOTE 테이블과 출고 포장 회차별 납품서 발행·무효 이력을 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "DELIVERY_NOTE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryNote {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "deliveryNoteSequenceGenerator")
	@SequenceGenerator(name = "deliveryNoteSequenceGenerator", sequenceName = "SEQ_DELIVERY_NOTE", allocationSize = 1)
	@Column(name = "delivery_note_id", nullable = false)
	private Long deliveryNoteId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipment_id", nullable = false)
	private Shipment shipment;

	@Column(name = "issue_sequence", nullable = false)
	private Integer issueSequence;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private DeliveryNoteStatus status = DeliveryNoteStatus.ACTIVE;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "issued_by", nullable = false)
	private AppUser issuedBy;

	@Column(name = "issued_at", nullable = false, updatable = false)
	private LocalDateTime issuedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "voided_by")
	private AppUser voidedBy;

	@Column(name = "voided_at")
	private LocalDateTime voidedAt;

	@Column(name = "void_reason", length = 1000)
	private String voidReason;

	@PrePersist
	protected void onCreate() {
		issuedAt = issuedAt == null ? LocalDateTime.now() : issuedAt;
	}

	// ========== 포장 확정 회차와 처리자를 연결한 ACTIVE 납품서 발행 이력을 생성하는 정적 팩토리 메서드 ==========
	public static DeliveryNote create(Shipment shipment, Integer issueSequence, AppUser issuedBy) {
		DeliveryNote deliveryNote = new DeliveryNote();
		deliveryNote.shipment = shipment;
		deliveryNote.issueSequence = issueSequence;
		deliveryNote.status = DeliveryNoteStatus.ACTIVE;
		deliveryNote.issuedBy = issuedBy;
		deliveryNote.issuedAt = LocalDateTime.now();
		return deliveryNote;
	}

	// ========== PACKED 주문 취소 또는 포장 취소 시 현재 유효한 납품서를 VOID 상태로 변경하는 메서드 ==========
	public void voidNote(AppUser voidedBy, String voidReason) {
		status = DeliveryNoteStatus.VOID;
		this.voidedBy = voidedBy;
		voidedAt = LocalDateTime.now();
		this.voidReason = voidReason;
	}
}
