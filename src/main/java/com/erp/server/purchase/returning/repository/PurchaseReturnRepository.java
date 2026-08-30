package com.erp.server.purchase.returning.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.purchase.returning.domain.PurchaseReturn;
import com.erp.server.purchase.returning.domain.PurchaseReturnStatus;

import jakarta.persistence.LockModeType;

// ********** PURCHASE_RETURN의 기본 CRUD와 원본 입고·상태별 목록·상세·비관적 잠금 조회를 처리하기 위한 Repository interface **********
public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {

	// ========== 원본 입고와 상태 조건을 함께 적용하여 매입 반품 목록을 페이지 조회하는 메서드 ==========
	// 목록 정렬은 Service에서 등록 일시·매입 반품 식별자 내림차순으로 고정한다.
	@Query(value = """
			select purchaseReturn
			from PurchaseReturn purchaseReturn
			join fetch purchaseReturn.receipt receipt
			join fetch receipt.purchaseOrder purchaseOrder
			join fetch purchaseOrder.supplier
			join fetch receipt.warehouse
			where (:receiptId is null or receipt.receiptId = :receiptId)
			  and (:status is null or purchaseReturn.status = :status)
			""",
			countQuery = """
			select count(purchaseReturn)
			from PurchaseReturn purchaseReturn
			where (:receiptId is null or purchaseReturn.receipt.receiptId = :receiptId)
			  and (:status is null or purchaseReturn.status = :status)
			""")
	Page<PurchaseReturn> findAllByFilters(@Param("receiptId") Long receiptId,
			@Param("status") PurchaseReturnStatus status, Pageable pageable);

	// ========== purchaseReturnId로 반품·원본 입고·공급업체·창고·처리 사용자를 한 번에 상세 조회하는 메서드 ==========
	// LOT별 품목은 별도 Repository에서 원본 발주 품목 순서와 LOT 식별자 순서로 조회한다.
	@Query("""
			select purchaseReturn
			from PurchaseReturn purchaseReturn
			join fetch purchaseReturn.receipt receipt
			join fetch receipt.purchaseOrder purchaseOrder
			join fetch purchaseOrder.supplier
			join fetch receipt.warehouse
			join fetch purchaseReturn.createdBy
			left join fetch purchaseReturn.completedBy
			left join fetch purchaseReturn.canceledBy
			where purchaseReturn.purchaseReturnId = :purchaseReturnId
			""")
	Optional<PurchaseReturn> findDetailById(@Param("purchaseReturnId") Long purchaseReturnId);

	// ========== 수정·완료·취소 중 같은 매입 반품의 동시 처리를 막기 위해 PESSIMISTIC_WRITE 잠금으로 조회하는 메서드 ==========
	// 잠금 획득 후 REGISTERED 상태와 최신 version을 다시 검증한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select purchaseReturn
			from PurchaseReturn purchaseReturn
			where purchaseReturn.purchaseReturnId = :purchaseReturnId
			""")
	Optional<PurchaseReturn> findByIdForUpdate(@Param("purchaseReturnId") Long purchaseReturnId);
}
