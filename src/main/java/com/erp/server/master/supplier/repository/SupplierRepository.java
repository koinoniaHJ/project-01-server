package com.erp.server.master.supplier.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.supplier.domain.Supplier;

import jakarta.persistence.LockModeType;

// ********** SUPPLIER의 기본 CRUD와 키워드·사용 상태·취급 품목 조건을 적용한 공급업체 목록 조회를 처리하기 위한 Repository interface **********
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

	// ========== 공급업체 코드·공급업체명·대표 연락처·발주 이메일과 사용 상태·취급 품목을 조건으로 공급업체 목록을 페이지 조회하는 메서드 ==========
	// keyword는 앞뒤 공백을 제거한 검색어이고 phoneKeyword는 숫자만 남긴 검색어이며, itemId는 SUPPLIER_ITEM 관계가 존재하는 공급업체만 조회할 때 전달한다.
	@Query("""
			select s
			from Supplier s
			where (
			    :keyword is null
			    or lower(s.supplierCode) like lower(concat('%', :keyword, '%'))
			    or lower(s.supplierName) like lower(concat('%', :keyword, '%'))
			    or lower(s.email) like lower(concat('%', :keyword, '%'))
			    or (
			        :phoneKeyword is not null
			        and cast(function('regexp_replace', s.phone, '[^0-9]', '') as String)
			            like concat('%', :phoneKeyword, '%')
			    )
			)
			  and (:status is null or s.status = :status)
			  and (
			      :itemId is null
			      or exists (
			          select si.supplierItemId
			          from SupplierItem si
			          where si.supplier = s
			            and si.item.itemId = :itemId
			      )
			  )
			""")
	Page<Supplier> findAllByFilters(@Param("keyword") String keyword,
			@Param("phoneKeyword") String phoneKeyword, @Param("status") MasterStatus status,
			@Param("itemId") Long itemId, Pageable pageable);

	// ========== 공급업체 상태 변경과 최신 상태 검증 중 동시 처리를 막기 위해 PESSIMISTIC_WRITE 비관적 잠금으로 조회하는 메서드 ==========
	// 먼저 잠금을 얻은 트랜잭션이 끝날 때까지 같은 공급업체를 잠금 조회하는 다른 요청은 대기한다.
	// ACTIVE/INACTIVE 변경과 이후 발주처럼 최신 공급업체 사용 상태 검증이 필요한 업무에서 사용한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select s
			from Supplier s
			where s.supplierId = :supplierId
			""")
	Optional<Supplier> findByIdForUpdate(@Param("supplierId") Long supplierId);

	// ========== Oracle의 공급업체 업무 코드 Sequence를 이용하여 SUP + 6자리 형식의 다음 공급업체 코드를 생성하는 메서드 ==========
	// 실제 PK용 SEQ_SUPPLIER와 업무 코드용 SEQ_SUPPLIER_CODE를 분리한다.
	@Query(value = """
			SELECT 'SUP' || LPAD(SEQ_SUPPLIER_CODE.NEXTVAL, 6, '0')
			FROM DUAL
			""", nativeQuery = true)
	String generateSupplierCode();

	// ========== 공급업체 사용 중지를 차단하는 진행 발주·입고·매입 반품·미결 매입 전표의 전체 건수를 조회하는 메서드 ==========
	/**
	 * DRAFT, SUBMITTED, APPROVED, ORDERED 발주
	 * PENDING, INSPECTING 입고
	 * REGISTERED 매입 반품
	 * UNPAID, PARTIALLY_PAID 매입·매입 반품 전표
	 */
	@Query(value = """
			SELECT
			    (
			        SELECT COUNT(*)
			        FROM PURCHASE_ORDER purchase_order
			        WHERE purchase_order.supplier_id = :supplierId
			          AND purchase_order.status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'ORDERED')
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM RECEIPT receipt
			        JOIN PURCHASE_ORDER purchase_order
			          ON purchase_order.purchase_order_id = receipt.purchase_order_id
			        WHERE purchase_order.supplier_id = :supplierId
			          AND receipt.status IN ('PENDING', 'INSPECTING')
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM PURCHASE_RETURN purchase_return
			        JOIN RECEIPT receipt
			          ON receipt.receipt_id = purchase_return.receipt_id
			        JOIN PURCHASE_ORDER purchase_order
			          ON purchase_order.purchase_order_id = receipt.purchase_order_id
			        WHERE purchase_order.supplier_id = :supplierId
			          AND purchase_return.status = 'REGISTERED'
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM VOUCHER voucher
			        WHERE voucher.supplier_id = :supplierId
			          AND voucher.type IN ('PURCHASE', 'PURCHASE_RETURN')
			          AND voucher.settlement_status IN ('UNPAID', 'PARTIALLY_PAID')
			    )
			FROM DUAL
			""", nativeQuery = true)
	long countOngoingBusinessReferences(@Param("supplierId") Long supplierId);
}
