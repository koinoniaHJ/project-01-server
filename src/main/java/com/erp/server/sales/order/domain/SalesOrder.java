package com.erp.server.sales.order.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.master.customer.domain.Customer;

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

// ********** Oracle Database의 SALES_ORDER 테이블과 주문 작성·접수·완료·취소 및 스냅샷을 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "SALES_ORDER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesOrder {

	// Oracle의 SEQ_SALES_ORDER에서 다음 값을 받아 PK로 사용하는 주문 식별자를 저장한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "salesOrderSequenceGenerator")
	@SequenceGenerator(name = "salesOrderSequenceGenerator", sequenceName = "SEQ_SALES_ORDER", allocationSize = 1)
	@Column(name = "sales_order_id", nullable = false)
	private Long salesOrderId;

	// 주문 대상 CUSTOMER 거래처를 참조하며 접수 이후에도 원본 기준정보 관계를 유지한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	// 주문 접수 경로를 VISIT, PHONE 또는 MESSAGE 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "channel", nullable = false, length = 20)
	private OrderChannel channel;

	// 주문 진행 상태를 DRAFT, REGISTERED, COMPLETED 또는 CANCELED 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private SalesOrderStatus status = SalesOrderStatus.DRAFT;

	// DRAFT에서는 현재 거래처 코드를 표시하고 접수 이후에는 접수 시점 값을 보존한다.
	@Column(name = "customer_code_snapshot", length = 20)
	private String customerCodeSnapshot;

	// DRAFT에서는 현재 거래처명을 표시하고 접수 이후에는 접수 시점 값을 보존한다.
	@Column(name = "customer_name_snapshot", length = 150)
	private String customerNameSnapshot;

	// DRAFT에서는 편집 가능한 배송지 우편번호이며 접수 이후에는 변경 불가 스냅샷으로 사용한다.
	@Column(name = "delivery_postal_code_snapshot", length = 10)
	private String deliveryPostalCodeSnapshot;

	// DRAFT에서는 편집 가능한 배송지 주소이며 접수 이후에는 변경 불가 스냅샷으로 사용한다.
	@Column(name = "delivery_address_snapshot", length = 500)
	private String deliveryAddressSnapshot;

	// DRAFT에서는 편집 가능한 배송지 상세 주소이며 접수 이후에는 변경 불가 스냅샷으로 사용한다.
	@Column(name = "delivery_address_detail_snapshot", length = 300)
	private String deliveryAddressDetailSnapshot;

	// DRAFT에서는 편집 가능한 수령인이며 접수 이후에는 변경 불가 스냅샷으로 사용한다.
	@Column(name = "recipient_name_snapshot", length = 100)
	private String recipientNameSnapshot;

	// DRAFT에서는 편집 가능한 수령인 연락처이며 접수 이후에는 변경 불가 스냅샷으로 사용한다.
	@Column(name = "recipient_phone_snapshot", length = 30)
	private String recipientPhoneSnapshot;

	// 주문 품목별 주문 수량과 판매 단가로 계산한 품목 금액의 합계를 저장한다.
	@Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	// 주문 작성자가 남긴 내부 업무 메모를 저장한다.
	@Column(name = "memo", length = 2000)
	private String memo;

	// 주문 접수를 처리한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "registered_by")
	private AppUser registeredBy;

	// DRAFT 주문이 REGISTERED로 변경된 일시를 저장한다.
	@Column(name = "registered_at")
	private LocalDateTime registeredAt;

	// 주문 취소를 처리한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "canceled_by")
	private AppUser canceledBy;

	// REGISTERED 주문이 CANCELED로 변경된 일시를 저장한다.
	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	// 주문 취소 시 입력한 필수 업무 사유를 저장한다.
	@Column(name = "cancel_reason", length = 1000)
	private String cancelReason;

	// DRAFT 주문을 최초 등록한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private AppUser createdBy;

	// 주문이 최초 등록된 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 주문 입력값 또는 상태가 마지막으로 변경된 일시를 저장한다.
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// 조회 당시 값과 DB 값을 비교하여 수정·삭제·접수·취소의 동시 처리 충돌을 확인한다.
	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	// 주문에 포함된 SALES_ORDER_ITEM 품목을 상위 주문과 같은 생명주기로 관리한다.
	@OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SalesOrderItem> items = new ArrayList<>();

	// ========== 신규 Entity 저장 전에 등록·수정 일시와 주문 금액 기본값을 설정하는 메서드 ==========
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

	// ========== 거래처 기본 배송정보를 적용하여 DRAFT 주문을 생성하는 정적 팩토리 메서드 ==========
	public static SalesOrder create(Customer customer, OrderChannel channel, String deliveryPostalCode,
			String deliveryAddress, String deliveryAddressDetail, String recipientName, String recipientPhone,
			String memo, AppUser createdBy) {
		SalesOrder salesOrder = new SalesOrder();
		salesOrder.customer = customer;
		salesOrder.channel = channel;
		salesOrder.status = SalesOrderStatus.DRAFT;
		salesOrder.customerCodeSnapshot = customer.getCustomerCode();
		salesOrder.customerNameSnapshot = customer.getCustomerName();
		salesOrder.deliveryPostalCodeSnapshot = deliveryPostalCode;
		salesOrder.deliveryAddressSnapshot = deliveryAddress;
		salesOrder.deliveryAddressDetailSnapshot = deliveryAddressDetail;
		salesOrder.recipientNameSnapshot = recipientName;
		salesOrder.recipientPhoneSnapshot = recipientPhone;
		salesOrder.memo = memo;
		salesOrder.createdBy = createdBy;
		return salesOrder;
	}

	// ========== DRAFT 주문의 거래처·접수 경로·배송정보·메모를 수정하는 메서드 ==========
	public void updateDraft(Customer customer, OrderChannel channel, String deliveryPostalCode,
			String deliveryAddress, String deliveryAddressDetail, String recipientName, String recipientPhone,
			String memo) {
		this.customer = customer;
		this.channel = channel;
		this.customerCodeSnapshot = customer.getCustomerCode();
		this.customerNameSnapshot = customer.getCustomerName();
		this.deliveryPostalCodeSnapshot = deliveryPostalCode;
		this.deliveryAddressSnapshot = deliveryAddress;
		this.deliveryAddressDetailSnapshot = deliveryAddressDetail;
		this.recipientNameSnapshot = recipientName;
		this.recipientPhoneSnapshot = recipientPhone;
		this.memo = memo;
	}

	// ========== 수정 요청에서 기존 주문 품목을 제거하고 주문 총액을 0으로 초기화하는 메서드 ==========
	public void clearItems() {
		items.clear();
		totalAmount = BigDecimal.ZERO;
	}

	// ========== DRAFT 주문에 품목을 추가하고 최신 품목 금액 합계로 주문 총액을 다시 계산하는 메서드 ==========
	public void addItem(SalesOrderItem item) {
		items.add(item);
		totalAmount = items.stream().map(SalesOrderItem::getLineAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	// ========== 최신 거래처·품목 값을 스냅샷으로 확정하고 주문을 REGISTERED 상태로 변경하는 메서드 ==========
	public void register(AppUser registeredBy) {
		customerCodeSnapshot = customer.getCustomerCode();
		customerNameSnapshot = customer.getCustomerName();
		items.forEach(SalesOrderItem::freezeSnapshot);
		status = SalesOrderStatus.REGISTERED;
		this.registeredBy = registeredBy;
		registeredAt = LocalDateTime.now();
	}

	// ========== 출고 완료 처리와 같은 트랜잭션에서 주문을 COMPLETED 상태로 변경하는 메서드 ==========
	public void complete() {
		status = SalesOrderStatus.COMPLETED;
	}

	// ========== 연결 출고 취소 처리 후 주문을 CANCELED 상태로 변경하고 취소 이력을 기록하는 메서드 ==========
	public void cancel(AppUser canceledBy, String cancelReason) {
		status = SalesOrderStatus.CANCELED;
		this.canceledBy = canceledBy;
		canceledAt = LocalDateTime.now();
		this.cancelReason = cancelReason;
	}
}
