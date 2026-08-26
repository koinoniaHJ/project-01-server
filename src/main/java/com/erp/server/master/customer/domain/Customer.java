package com.erp.server.master.customer.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.master.common.domain.MasterStatus;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ********** Oracle Database의 CUSTOMER 테이블과 거래처 정보를 Java 객체로 매핑하고 거래처 정보 변경 규칙을 관리하기 위한 Entity 클래스 **********
@Entity
@Table(name = "CUSTOMER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {

	// CUSTOMER.customer_id 컬럼과 매핑하며 Oracle의 SEQ_CUSTOMER에서 다음 값을 받아 PK로 사용한다.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customerSequenceGenerator")
	@SequenceGenerator(name = "customerSequenceGenerator", sequenceName = "SEQ_CUSTOMER", allocationSize = 1)
	@Column(name = "customer_id", nullable = false)
	private Long customerId;

	// CUSTOMER.customer_code 컬럼과 매핑하며 CUS + 6자리 형식으로 자동 생성되는 거래처 코드를 저장한다.
	@Column(name = "customer_code", nullable = false, length = 20)
	private String customerCode;

	// CUSTOMER.customer_name 컬럼과 매핑하며 거래처명을 저장한다.
	@Column(name = "customer_name", nullable = false, length = 150)
	private String customerName;

	// CUSTOMER.phone 컬럼과 매핑하며 거래처 대표 연락처를 저장한다.
	@Column(name = "phone", length = 30)
	private String phone;

	// CUSTOMER.email 컬럼과 매핑하며 거래명세서 수신 이메일을 저장한다.
	@Column(name = "email", length = 255)
	private String email;

	// CUSTOMER.postal_code 컬럼과 매핑하며 거래처 사업장 우편번호를 저장한다.
	@Column(name = "postal_code", length = 10)
	private String postalCode;

	// CUSTOMER.address 컬럼과 매핑하며 거래처 사업장 기본 주소를 저장한다.
	@Column(name = "address", length = 500)
	private String address;

	// CUSTOMER.address_detail 컬럼과 매핑하며 거래처 사업장 상세 주소를 저장한다.
	@Column(name = "address_detail", length = 300)
	private String addressDetail;

	// CUSTOMER.delivery_postal_code 컬럼과 매핑하며 주문 작성 시 기본 적용할 배송지 우편번호를 저장한다.
	@Column(name = "delivery_postal_code", length = 10)
	private String deliveryPostalCode;

	// CUSTOMER.delivery_address 컬럼과 매핑하며 주문 작성 시 기본 적용할 배송지 주소를 저장한다.
	@Column(name = "delivery_address", length = 500)
	private String deliveryAddress;

	// CUSTOMER.delivery_address_detail 컬럼과 매핑하며 주문 작성 시 기본 적용할 배송지 상세 주소를 저장한다.
	@Column(name = "delivery_address_detail", length = 300)
	private String deliveryAddressDetail;

	// CUSTOMER.recipient_name 컬럼과 매핑하며 주문 작성 시 기본 적용할 수령인 이름을 저장한다.
	@Column(name = "recipient_name", length = 100)
	private String recipientName;

	// CUSTOMER.recipient_phone 컬럼과 매핑하며 주문 작성 시 기본 적용할 수령인 연락처를 저장한다.
	@Column(name = "recipient_phone", length = 30)
	private String recipientPhone;

	// CUSTOMER.memo 컬럼과 매핑하며 예외 판매 단가 등 거래처 참고사항을 저장한다.
	@Column(name = "memo", length = 2000)
	private String memo;

	// CUSTOMER.status 컬럼과 매핑하며 거래처 사용 상태를 ACTIVE 또는 INACTIVE 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private MasterStatus status = MasterStatus.ACTIVE;

	// CUSTOMER.trade_status 컬럼과 매핑하며 거래 상태를 NORMAL 또는 HOLD 문자열로 저장한다.
	@Enumerated(EnumType.STRING)
	@Column(name = "trade_status", nullable = false, length = 20)
	private CustomerTradeStatus tradeStatus = CustomerTradeStatus.NORMAL;

	// CUSTOMER.total_receivable_amount 컬럼과 매핑하며 거래처의 전체 미수금 합계를 저장한다.
	@Column(name = "total_receivable_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalReceivableAmount = BigDecimal.ZERO;

	// CUSTOMER.created_by 컬럼과 매핑하며 거래처를 등록한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private AppUser createdBy;

	// CUSTOMER.created_at 컬럼과 매핑하며 거래처 등록 일시를 저장한다.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// CUSTOMER.updated_by 컬럼과 매핑하며 거래처를 마지막으로 수정한 APP_USER 사용자를 참조한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "updated_by", nullable = false)
	private AppUser updatedBy;

	// CUSTOMER.updated_at 컬럼과 매핑하며 거래처의 최근 수정 일시를 저장한다.
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// CUSTOMER.version 컬럼과 매핑하며 조회 당시 값과 DB 값을 비교하여 동시 수정 충돌을 확인한다.
	@Version
	@Column(name = "version", nullable = false)
	private Long version;

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

	// ========== 신규 거래처 Entity를 생성하는 정적 팩토리 메서드 ==========
	public static Customer create(String customerCode, String customerName, String phone, String email,
			String postalCode, String address, String addressDetail, String deliveryPostalCode,
			String deliveryAddress, String deliveryAddressDetail, String recipientName, String recipientPhone,
			String memo, AppUser createdBy) {

		Customer customer = new Customer();

		customer.customerCode = customerCode;
		customer.customerName = customerName;
		customer.phone = phone;
		customer.email = email;
		customer.postalCode = postalCode;
		customer.address = address;
		customer.addressDetail = addressDetail;
		customer.deliveryPostalCode = deliveryPostalCode;
		customer.deliveryAddress = deliveryAddress;
		customer.deliveryAddressDetail = deliveryAddressDetail;
		customer.recipientName = recipientName;
		customer.recipientPhone = recipientPhone;
		customer.memo = memo;
		customer.status = MasterStatus.ACTIVE;
		customer.tradeStatus = CustomerTradeStatus.NORMAL;
		customer.totalReceivableAmount = BigDecimal.ZERO;
		customer.createdBy = createdBy;
		customer.updatedBy = createdBy;

		return customer;
	}

	// ========== 거래처 기본정보와 배송정보를 변경하는 메서드 ==========
	public void update(String customerName, String phone, String email, String postalCode, String address,
			String addressDetail, String deliveryPostalCode, String deliveryAddress,
			String deliveryAddressDetail, String recipientName, String recipientPhone, String memo,
			AppUser updatedBy) {

		this.customerName = customerName;
		this.phone = phone;
		this.email = email;
		this.postalCode = postalCode;
		this.address = address;
		this.addressDetail = addressDetail;
		this.deliveryPostalCode = deliveryPostalCode;
		this.deliveryAddress = deliveryAddress;
		this.deliveryAddressDetail = deliveryAddressDetail;
		this.recipientName = recipientName;
		this.recipientPhone = recipientPhone;
		this.memo = memo;
		this.updatedBy = updatedBy;
	}

	// ========== 거래처 사용 상태를 변경하는 메서드 ==========
	public void changeStatus(MasterStatus status, AppUser updatedBy) {

		this.status = status;
		this.updatedBy = updatedBy;
	}

	// ========== 거래처 거래 상태를 변경하는 메서드 ==========
	public void changeTradeStatus(CustomerTradeStatus tradeStatus, AppUser updatedBy) {

		this.tradeStatus = tradeStatus;
		this.updatedBy = updatedBy;
	}
}