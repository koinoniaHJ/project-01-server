package com.erp.server.master.customer.service;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.domain.UserRole;
import com.erp.server.common.user.repository.AppUserRepository;
import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.customer.domain.Customer;
import com.erp.server.master.customer.domain.CustomerTradeStatus;
import com.erp.server.master.customer.domain.CustomerTradeStatusHistory;
import com.erp.server.master.customer.dto.CustomerCreateRequest;
import com.erp.server.master.customer.dto.CustomerDetailResponse;
import com.erp.server.master.customer.dto.CustomerListResponse;
import com.erp.server.master.customer.dto.CustomerStatusRequest;
import com.erp.server.master.customer.dto.CustomerTradeStatusChangeResponse;
import com.erp.server.master.customer.dto.CustomerTradeStatusHistoryResponse;
import com.erp.server.master.customer.dto.CustomerTradeStatusRequest;
import com.erp.server.master.customer.dto.CustomerUpdateRequest;
import com.erp.server.master.customer.repository.CustomerRepository;
import com.erp.server.master.customer.repository.CustomerTradeStatusHistoryRepository;

import lombok.RequiredArgsConstructor;

// ********** 거래처 목록 조회와 거래처 관련 업무 규칙을 처리하기 위한 Service 클래스 **********
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

	private final AppUserRepository appUserRepository;
	private final CustomerRepository customerRepository;
	private final CustomerTradeStatusHistoryRepository customerTradeStatusHistoryRepository;

	// ========== 키워드·사용 상태·거래 상태 조건을 적용하여 거래처 목록을 페이지 조회하는 메서드 ==========
	public Page<CustomerListResponse> getCustomers(String keyword, MasterStatus status, CustomerTradeStatus tradeStatus,
			UserRole currentUserRole, Pageable pageable) {

		String normalizedKeyword = normalizeKeyword(keyword);
		String phoneKeyword = extractPhoneKeyword(normalizedKeyword);

		// 목록 변환 시 현재 역할을 전달하여 WAREHOUSE의 총미수금을 숨긴다.
		return customerRepository.findAllByFilters(normalizedKeyword, phoneKeyword, status, tradeStatus, pageable)
				.map(customer -> CustomerListResponse.from(customer, currentUserRole));
	}

	// ========== 신규 거래처를 등록하는 메서드 ==========
	@Transactional
	public CustomerDetailResponse createCustomer(CustomerCreateRequest request, Long currentUserId) {

		AppUser currentUser = findUser(currentUserId);
		String customerCode = customerRepository.generateCustomerCode();

		// 사용 상태는 ACTIVE, 거래 상태는 NORMAL, 총미수금은 0으로 Entity에서 설정
		// createdBy와 updatedBy에는 현재 로그인 사용자가 저장
		Customer customer = Customer.create(customerCode, // SEQ_CUSTOMER_CODE로 자동 생성
				request.customerName(), request.phone(), request.email(), request.postalCode(), request.address(),
				request.addressDetail(), request.deliveryPostalCode(), request.deliveryAddress(),
				request.deliveryAddressDetail(), request.recipientName(), request.recipientPhone(), request.memo(),
				currentUser);

		Customer savedCustomer = customerRepository.saveAndFlush(customer);

		return CustomerDetailResponse.from(savedCustomer, currentUser.getRole());
	}

	// ========== customerId로 거래처 상세정보를 조회하는 메서드 ==========
	public CustomerDetailResponse getCustomer(Long customerId, UserRole currentUserRole) {

		Customer customer = findCustomer(customerId);

		return CustomerDetailResponse.from(customer, currentUserRole);
	}

	// ========== 거래처 기본정보와 기본 배송정보를 수정하는 메서드 ==========
	@Transactional
	public CustomerDetailResponse updateCustomer(Long customerId, CustomerUpdateRequest request, Long currentUserId) {

		Customer customer = findCustomer(customerId);

		// 사용자가 수정 화면을 열어둔 사이 다른 사용자가 이미 저장했는지 확인
		validateVersion(customer, request.version());

		AppUser currentUser = findUser(currentUserId);

		// DB가 아니라 서버 메모리에 올라온 Customer 객체만 바꾼다. JPA는 변경된 Entity를 모아뒀다가 트랜잭션이 끝날 때 SQL을
		// 보내는 방식으로 동작한다.
		customer.update(request.customerName(), request.phone(), request.email(), request.postalCode(),
				request.address(), request.addressDetail(), request.deliveryPostalCode(), request.deliveryAddress(),
				request.deliveryAddressDetail(), request.recipientName(), request.recipientPhone(), request.memo(),
				currentUser);

		// 실제 DB 저장 순간의 동시 수정 검증
		flushCustomerChanges(); // 이때 JPA가 모아둔 변경 내용을 실제 DB에 전송
		// flush()는 커밋이 아니기 때문에 SQL을 실행해서 성공 여부를 확인할 뿐이고, 이후 오류가 발생하면 여전히 전체 롤백할 수 있다.

		return CustomerDetailResponse.from(customer, currentUser.getRole());
	}

	// ========== 거래처를 비관적 잠금으로 조회하여 거래 상태 변경과 변경 이력 저장을 하나의 트랜잭션으로 처리하는 메서드
	// ==========
	@Transactional
	public CustomerTradeStatusChangeResponse changeTradeStatus(Long customerId, CustomerTradeStatusRequest request,
			Long currentUserId) {

		Customer customer = findCustomerForUpdate(customerId);

		validateVersion(customer, request.version());
		validateTradeStatusChange(customer, request.tradeStatus());

		AppUser currentUser = findUser(currentUserId);
		CustomerTradeStatus previousStatus = customer.getTradeStatus();

		customer.changeTradeStatus(request.tradeStatus(), currentUser);

		CustomerTradeStatusHistory history = CustomerTradeStatusHistory.create(customer, previousStatus,
				request.tradeStatus(), request.reason(), currentUser);

		customerTradeStatusHistoryRepository.save(history);

		// Customer UPDATE와 거래 상태 이력 INSERT를 실행하여 동시 수정 충돌과 DB 제약조건을 확인한다.
		flushCustomerChanges();

		return CustomerTradeStatusChangeResponse.from(customer, history, currentUser.getRole());
	}

	// ========== 거래처 존재 여부를 확인하고 거래 상태 변경 이력을 최근 변경 순서로 페이지 조회하는 메서드 ==========
	public Page<CustomerTradeStatusHistoryResponse> getTradeStatusHistory(Long customerId, Pageable pageable) {

		// 이력이 없더라도 존재하지 않는 거래처를 빈 목록으로 반환하지 않도록 거래처를 먼저 확인한다.
		findCustomer(customerId);

		return customerTradeStatusHistoryRepository.findAllByCustomerId(customerId, pageable)
				.map(CustomerTradeStatusHistoryResponse::from);
	}

	// ========== 진행 업무 참조 조건을 검증하고 거래처 사용 상태를 변경하는 메서드 ==========
	@Transactional
	public CustomerDetailResponse changeStatus(Long customerId, CustomerStatusRequest request, Long currentUserId) {

		Customer customer = findCustomerForUpdate(customerId);

		validateVersion(customer, request.version());
		validateCustomerCanBeInactivated(customer, request.status());

		AppUser currentUser = findUser(currentUserId);

		customer.changeStatus(request.status(), currentUser);

		flushCustomerChanges();

		return CustomerDetailResponse.from(customer, currentUser.getRole());
	}

	// ========== customerId로 거래처를 조회하고 없으면 404 오류를 발생시키는 메서드 ==========
	private Customer findCustomer(Long customerId) {

		return customerRepository.findById(customerId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "거래처를 찾을 수 없습니다."));
	}

	// ========== userId로 현재 사용자를 조회하고 없으면 404 오류를 발생시키는 메서드 ==========
	private AppUser findUser(Long userId) {

		return appUserRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));
	}

	// ========== 요청 version과 현재 거래처 Entity version이 같은지 검증하는 메서드 ==========
	private void validateVersion(Customer customer, Long requestVersion) {

		if (!Objects.equals(customer.getVersion(), requestVersion)) {
			throw new BusinessException(ErrorCode.CONFLICT, "다른 사용자가 먼저 수정했습니다. 최신 거래처 정보를 다시 조회해 주세요.");
		}
	}

	// ========== ACTIVE 거래처를 INACTIVE로 변경할 때 진행 업무 참조가 없는지 검증하는 메서드 ==========
	private void validateCustomerCanBeInactivated(Customer customer, MasterStatus nextStatus) {

		boolean inactivationRequested = customer.getStatus() == MasterStatus.ACTIVE
				&& nextStatus == MasterStatus.INACTIVE;

		if (!inactivationRequested) {
			return;
		}

		long ongoingReferenceCount = customerRepository.countOngoingBusinessReferences(customer.getCustomerId());

		if (ongoingReferenceCount > 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "진행 중인 주문·반품, 미결 매출 전표 또는 미배분 입금이 있어 거래처를 사용 중지할 수 없습니다.");
		}
	}

	// ========== 현재 거래 상태와 변경할 거래 상태가 다른지 검증하는 메서드 ==========
	private void validateTradeStatusChange(Customer customer, CustomerTradeStatus nextTradeStatus) {

		if (customer.getTradeStatus() == nextTradeStatus) {
			throw new BusinessException(ErrorCode.CONFLICT, "현재 거래 상태와 동일한 상태로 변경할 수 없습니다.");
		}
	}

	// ========== 거래처 UPDATE를 즉시 실행하여 최종 낙관적 잠금 충돌을 확인하는 메서드 ==========
	private void flushCustomerChanges() {

		try {
			customerRepository.flush();

		} catch (ObjectOptimisticLockingFailureException exception) {
			throw new BusinessException(ErrorCode.CONFLICT, "다른 사용자가 먼저 수정했습니다. 최신 거래처 정보를 다시 조회해 주세요.");
		}
	}

	// ========== 상태 변경 중 동시 처리를 막기 위해 customerId로 거래처를 비관적 잠금 조회하는 메서드 ==========
	private Customer findCustomerForUpdate(Long customerId) {

		return customerRepository.findByIdForUpdate(customerId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "거래처를 찾을 수 없습니다."));
	}

	// ========== 키워드의 앞뒤 공백을 제거하고 검색값이 없으면 null로 변환하는 메서드 ==========
	private String normalizeKeyword(String keyword) {

		if (keyword == null) {
			return null;
		}

		String normalizedKeyword = keyword.trim();

		return normalizedKeyword.isEmpty() ? null : normalizedKeyword;
	}

	// ========== 대표 연락처 검색을 위해 전화번호 형식의 키워드에서 숫자만 추출하는 메서드 ==========
	private String extractPhoneKeyword(String keyword) {

		if (keyword == null) {
			return null;
		}

		// 거래처 코드나 거래처명에 포함된 숫자가 연락처 검색에 사용되지 않도록 문자가 있으면 제외한다.
		boolean containsLetter = keyword.codePoints().anyMatch(Character::isLetter);

		if (containsLetter) {
			return null;
		}

		String phoneKeyword = keyword.replaceAll("[^0-9]", "");

		return phoneKeyword.isEmpty() ? null : phoneKeyword;
	}
}