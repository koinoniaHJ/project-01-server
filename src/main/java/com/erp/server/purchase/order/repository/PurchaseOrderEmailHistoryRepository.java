package com.erp.server.purchase.order.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.purchase.order.domain.PurchaseOrderEmailHistory;

// ********** PURCHASE_ORDER_EMAIL_HISTORY의 기본 CRUD와 발주별 이메일 전송 이력 조회를 처리하기 위한 Repository interface **********
public interface PurchaseOrderEmailHistoryRepository extends JpaRepository<PurchaseOrderEmailHistory, Long> {

	// ========== 한 발주의 이메일 전송 이력과 실행 사용자를 최근 시도 순서로 페이지 조회하는 메서드 ==========
	// 시도 일시가 같으면 이메일 이력 식별자가 큰 항목을 먼저 조회하여 정렬 순서를 고정한다.
	@Query(value = """
			select history
			from PurchaseOrderEmailHistory history
			left join fetch history.attemptedBy
			where history.purchaseOrder.purchaseOrderId = :purchaseOrderId
			""",
			countQuery = """
			select count(history)
			from PurchaseOrderEmailHistory history
			where history.purchaseOrder.purchaseOrderId = :purchaseOrderId
			""")
	Page<PurchaseOrderEmailHistory> findAllByPurchaseOrderId(@Param("purchaseOrderId") Long purchaseOrderId,
			Pageable pageable);

	// ========== 한 발주에서 다음 이메일 전송 시도 순번을 생성하기 위해 현재 최대 순번을 조회하는 메서드 ==========
	@Query("""
			select coalesce(max(history.attemptNo), 0)
			from PurchaseOrderEmailHistory history
			where history.purchaseOrder.purchaseOrderId = :purchaseOrderId
			""")
	int findMaxAttemptNo(@Param("purchaseOrderId") Long purchaseOrderId);
}
