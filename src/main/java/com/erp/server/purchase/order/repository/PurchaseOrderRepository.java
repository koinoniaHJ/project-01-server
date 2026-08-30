package com.erp.server.purchase.order.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.purchase.order.domain.PurchaseOrder;
import com.erp.server.purchase.order.domain.PurchaseOrderEmailStatus;
import com.erp.server.purchase.order.domain.PurchaseOrderStatus;

import jakarta.persistence.LockModeType;

// ********** PURCHASE_ORDER의 기본 CRUD와 조건별 목록 집계·상세 연관정보 조회를 처리하기 위한 Repository interface **********
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

	// ========== 상태·이메일 상태·공급업체·등록 기간을 조건으로 발주 목록과 수량 합계를 페이지 조회하는 메서드 ==========
	// 시작일과 종료일은 발주 등록 일시를 기준으로 모두 포함하며 Service에서 시작 시각과 종료 다음 날 시각으로 변환한다.
	// 모든 선택 조건은 함께 전달되면 동시에 적용하고 정렬은 Service에서 등록 일시·발주 식별자 내림차순으로 고정한다.
	@Query(value = """
			select po.purchaseOrderId as purchaseOrderId,
			       s.supplierId as supplierId,
			       s.supplierCode as supplierCode,
			       s.supplierName as supplierName,
			       po.status as status,
			       po.emailStatus as emailStatus,
			       coalesce(sum(poi.orderedQuantity), 0) as totalOrderedQuantity,
			       coalesce(sum(poi.receivedQuantity), 0) as totalReceivedQuantity,
			       coalesce(sum(poi.orderedQuantity - poi.receivedQuantity), 0) as totalRemainingQuantity,
			       po.totalAmount as totalAmount,
			       po.createdAt as createdAt,
			       po.orderedAt as orderedAt,
			       po.version as version
			from PurchaseOrder po
			join po.supplier s
			left join po.items poi
			where (:status is null or po.status = :status)
			  and (:emailStatus is null or po.emailStatus = :emailStatus)
			  and (:supplierId is null or s.supplierId = :supplierId)
			  and (:startAt is null or po.createdAt >= :startAt)
			  and (:endAtExclusive is null or po.createdAt < :endAtExclusive)
			group by po.purchaseOrderId, s.supplierId, s.supplierCode, s.supplierName,
			         po.status, po.emailStatus, po.totalAmount, po.createdAt, po.orderedAt, po.version
			""",
			countQuery = """
			select count(po)
			from PurchaseOrder po
			where (:status is null or po.status = :status)
			  and (:emailStatus is null or po.emailStatus = :emailStatus)
			  and (:supplierId is null or po.supplier.supplierId = :supplierId)
			  and (:startAt is null or po.createdAt >= :startAt)
			  and (:endAtExclusive is null or po.createdAt < :endAtExclusive)
			""")
	Page<PurchaseOrderListProjection> findAllByFilters(@Param("status") PurchaseOrderStatus status,
			@Param("emailStatus") PurchaseOrderEmailStatus emailStatus,
			@Param("supplierId") Long supplierId,
			@Param("startAt") LocalDateTime startAt,
			@Param("endAtExclusive") LocalDateTime endAtExclusive,
			Pageable pageable);

	// ========== 발주 상세 응답에 필요한 공급업체와 각 처리 사용자를 한 번에 조회하는 메서드 ==========
	// 발주 품목은 한 발주에 여러 건이므로 별도 Repository에서 표시 순서대로 조회하여 페이지 조회 중복을 피한다.
	@Query("""
			select po
			from PurchaseOrder po
			join fetch po.supplier
			join fetch po.createdBy
			left join fetch po.submittedBy
			left join fetch po.approvedBy
			left join fetch po.orderedBy
			left join fetch po.canceledBy
			left join fetch po.closedBy
			left join fetch po.supplierCancelConfirmedBy
			where po.purchaseOrderId = :purchaseOrderId
			""")
	Optional<PurchaseOrder> findDetailById(@Param("purchaseOrderId") Long purchaseOrderId);

	// ========== 발주 수정·삭제·상태 전이 중 같은 발주의 동시 처리를 막기 위해 PESSIMISTIC_WRITE 비관적 잠금으로 조회하는 메서드 ==========
	// 먼저 잠금을 얻은 트랜잭션이 끝날 때까지 같은 발주를 변경하려는 다른 요청은 대기한 후 최신 상태와 version을 검증한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select po
			from PurchaseOrder po
			where po.purchaseOrderId = :purchaseOrderId
			""")
	Optional<PurchaseOrder> findByIdForUpdate(@Param("purchaseOrderId") Long purchaseOrderId);

	// ========== ORDERED 발주 취소를 차단할 정상 입고 누적 수량과 진행 중 입고의 전체 건수를 조회하는 메서드 ==========
	// PURCHASE_ORDER_ITEM.received_quantity는 완료된 입고의 정상 수량 누계이며 PENDING·INSPECTING 입고는 별도로 검사한다.
	@Query(value = """
			SELECT
			    (
			        SELECT COUNT(*)
			        FROM PURCHASE_ORDER_ITEM purchase_order_item
			        WHERE purchase_order_item.purchase_order_id = :purchaseOrderId
			          AND purchase_order_item.received_quantity > 0
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM RECEIPT receipt
			        WHERE receipt.purchase_order_id = :purchaseOrderId
			          AND receipt.status IN ('PENDING', 'INSPECTING')
			    )
			FROM DUAL
			""", nativeQuery = true)
	long countCancellationBlockingReferences(@Param("purchaseOrderId") Long purchaseOrderId);

	// ========== 목록 집계 Query 결과를 Entity 전체 조회 없이 필요한 필드만 전달하기 위한 Projection interface ==========
	interface PurchaseOrderListProjection {
		Long getPurchaseOrderId();
		Long getSupplierId();
		String getSupplierCode();
		String getSupplierName();
		PurchaseOrderStatus getStatus();
		PurchaseOrderEmailStatus getEmailStatus();
		BigDecimal getTotalOrderedQuantity();
		BigDecimal getTotalReceivedQuantity();
		BigDecimal getTotalRemainingQuantity();
		BigDecimal getTotalAmount();
		LocalDateTime getCreatedAt();
		LocalDateTime getOrderedAt();
		Long getVersion();
	}
}
