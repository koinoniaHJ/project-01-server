package com.erp.server.sales.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.sales.order.domain.SalesOrderItem;

// ********** SALES_ORDER_ITEM의 기본 CRUD와 주문별 품목 상세 조회를 처리하기 위한 Repository interface **********
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {

	// ========== DRAFT 주문 수정 시 기존 품목을 먼저 삭제하여 동일 품목·순번 UNIQUE 충돌을 방지하는 메서드 ==========
	@Modifying(clearAutomatically = false, flushAutomatically = true)
	@Query("""
			delete from SalesOrderItem salesOrderItem
			where salesOrderItem.salesOrder.salesOrderId = :salesOrderId
			""")
	void deleteAllBySalesOrderId(@Param("salesOrderId") Long salesOrderId);

	// ========== 한 주문의 품목과 원본 ITEM 기준정보를 화면 순서대로 상세 조회하는 메서드 ==========
	@Query("""
			select salesOrderItem
			from SalesOrderItem salesOrderItem
			join fetch salesOrderItem.item
			where salesOrderItem.salesOrder.salesOrderId = :salesOrderId
			order by salesOrderItem.lineNo asc
			""")
	List<SalesOrderItem> findAllBySalesOrderIdWithItem(@Param("salesOrderId") Long salesOrderId);
}
