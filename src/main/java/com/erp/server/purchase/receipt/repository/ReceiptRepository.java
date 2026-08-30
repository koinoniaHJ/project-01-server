package com.erp.server.purchase.receipt.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.purchase.receipt.domain.Receipt;
import com.erp.server.purchase.receipt.domain.ReceiptStatus;

import jakarta.persistence.LockModeType;

// ********** RECEIPT의 기본 CRUD와 조건별 목록·상세·비관적 잠금 조회를 처리하기 위한 Repository interface **********
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

	// ========== 발주·검수 상태·등록일 조건을 함께 적용하여 입고 목록을 페이지 조회하는 메서드 ==========
	// 시작일과 종료일은 입고 등록 일시 기준으로 모두 포함하며 목록 정렬은 Service에서 등록 일시·입고 식별자 내림차순으로 고정한다.
	@Query(value = """
			select receipt
			from Receipt receipt
			join fetch receipt.purchaseOrder purchaseOrder
			join fetch purchaseOrder.supplier
			join fetch receipt.warehouse
			where (:purchaseOrderId is null or purchaseOrder.purchaseOrderId = :purchaseOrderId)
			  and (:status is null or receipt.status = :status)
			  and (:startAt is null or receipt.createdAt >= :startAt)
			  and (:endAtExclusive is null or receipt.createdAt < :endAtExclusive)
			""",
			countQuery = """
			select count(receipt)
			from Receipt receipt
			where (:purchaseOrderId is null or receipt.purchaseOrder.purchaseOrderId = :purchaseOrderId)
			  and (:status is null or receipt.status = :status)
			  and (:startAt is null or receipt.createdAt >= :startAt)
			  and (:endAtExclusive is null or receipt.createdAt < :endAtExclusive)
			""")
	Page<Receipt> findAllByFilters(@Param("purchaseOrderId") Long purchaseOrderId,
			@Param("status") ReceiptStatus status, @Param("startAt") LocalDateTime startAt,
			@Param("endAtExclusive") LocalDateTime endAtExclusive, Pageable pageable);

	// ========== 입고 상세 응답에 필요한 발주·공급업체·창고·처리 사용자를 한 번에 조회하는 메서드 ==========
	// 입고 품목과 LOT는 별도 Repository에서 발주 품목 순서대로 조회하여 컬렉션 중복을 피한다.
	@Query("""
			select receipt
			from Receipt receipt
			join fetch receipt.purchaseOrder purchaseOrder
			join fetch purchaseOrder.supplier
			join fetch receipt.warehouse
			join fetch receipt.createdBy
			left join fetch receipt.inspectionStartedBy
			left join fetch receipt.completedBy
			left join fetch receipt.canceledBy
			where receipt.receiptId = :receiptId
			""")
	Optional<Receipt> findDetailById(@Param("receiptId") Long receiptId);

	// ========== 입고 창고·검수 결과·상태 변경 중 같은 입고의 동시 처리를 막기 위해 PESSIMISTIC_WRITE 잠금으로 조회하는 메서드 ==========
	// 먼저 잠금을 얻은 트랜잭션이 끝날 때까지 같은 입고를 처리하려는 요청은 대기한 후 최신 상태와 version을 검증한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select receipt
			from Receipt receipt
			where receipt.receiptId = :receiptId
			""")
	Optional<Receipt> findByIdForUpdate(@Param("receiptId") Long receiptId);
}
