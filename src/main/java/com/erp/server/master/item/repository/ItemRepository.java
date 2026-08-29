package com.erp.server.master.item.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.item.domain.Item;

import jakarta.persistence.LockModeType;

// ********** ITEM의 기본 CRUD와 키워드·사용 상태·취급 공급업체 조건을 적용한 품목 목록 조회를 처리하기 위한 Repository interface **********
public interface ItemRepository extends JpaRepository<Item, Long> {

	// ========== 품목 코드·품목명과 사용 상태·취급 공급업체를 조건으로 품목 목록을 페이지 조회하는 메서드 ==========
	// keyword는 앞뒤 공백을 제거한 후 품목 코드와 품목명을 대소문자 구분 없이 부분 검색한다.
	// supplierId는 SUPPLIER_ITEM 관계가 존재하는 품목만 조회하며 keyword, status와 함께 전달되면 모든 조건을 적용한다.
	@Query("""
			select i
			from Item i
			where (
			    :keyword is null
			    or lower(i.itemCode) like lower(concat('%', :keyword, '%'))
			    or lower(i.itemName) like lower(concat('%', :keyword, '%'))
			)
			  and (:status is null or i.status = :status)
			  and (
			      :supplierId is null
			      or exists (
			          select si.supplierItemId
			          from SupplierItem si
			          where si.item = i
			            and si.supplier.supplierId = :supplierId
			      )
			  )
			""")
	Page<Item> findAllByFilters(@Param("keyword") String keyword, @Param("status") MasterStatus status,
			@Param("supplierId") Long supplierId, Pageable pageable);

	// ========== 품목 상태 변경과 최신 상태 검증 중 동시 처리를 막기 위해 PESSIMISTIC_WRITE 비관적 잠금으로 조회하는 메서드 ==========
	// 먼저 잠금을 얻은 트랜잭션이 끝날 때까지 같은 품목을 잠금 조회하는 다른 요청은 대기한다.
	// ACTIVE/INACTIVE 변경과 이후 발주·주문처럼 최신 품목 사용 상태 검증이 필요한 업무에서 사용한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select i
			from Item i
			where i.itemId = :itemId
			""")
	Optional<Item> findByIdForUpdate(@Param("itemId") Long itemId);

	// ========== Oracle의 품목 업무 코드 Sequence를 이용하여 ITM + 6자리 형식의 다음 품목 코드를 생성하는 메서드 ==========
	// 실제 PK용 SEQ_ITEM과 업무 코드용 SEQ_ITEM_CODE를 분리한다.
	@Query(value = """
			SELECT 'ITM' || LPAD(SEQ_ITEM_CODE.NEXTVAL, 6, '0')
			FROM DUAL
			""", nativeQuery = true)
	String generateItemCode();

	// ========== 품목 사용 중지를 차단하는 진행 발주·입고·반품·주문·재고 업무의 전체 건수를 조회하는 메서드 ==========
	/**
	 * DRAFT, SUBMITTED, APPROVED, ORDERED 발주
	 * PENDING, INSPECTING 입고
	 * REGISTERED 매입 반품
	 * DRAFT, REGISTERED 주문
	 * REGISTERED 거래처 반품
	 * PENDING, COUNTING, RESULT_REGISTERED 재고 실사
	 * SUBMITTED 재고 조정
	 */
	@Query(value = """
			SELECT
			    (
			        SELECT COUNT(*)
			        FROM PURCHASE_ORDER_ITEM purchase_order_item
			        JOIN PURCHASE_ORDER purchase_order
			          ON purchase_order.purchase_order_id = purchase_order_item.purchase_order_id
			        WHERE purchase_order_item.item_id = :itemId
			          AND purchase_order.status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'ORDERED')
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM RECEIPT_ITEM receipt_item
			        JOIN PURCHASE_ORDER_ITEM purchase_order_item
			          ON purchase_order_item.purchase_order_item_id = receipt_item.purchase_order_item_id
			        JOIN RECEIPT receipt
			          ON receipt.receipt_id = receipt_item.receipt_id
			        WHERE purchase_order_item.item_id = :itemId
			          AND receipt.status IN ('PENDING', 'INSPECTING')
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM PURCHASE_RETURN_ITEM purchase_return_item
			        JOIN PURCHASE_RETURN purchase_return
			          ON purchase_return.purchase_return_id = purchase_return_item.purchase_return_id
			        WHERE purchase_return_item.item_id = :itemId
			          AND purchase_return.status = 'REGISTERED'
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM SALES_ORDER_ITEM sales_order_item
			        JOIN SALES_ORDER sales_order
			          ON sales_order.sales_order_id = sales_order_item.sales_order_id
			        WHERE sales_order_item.item_id = :itemId
			          AND sales_order.status IN ('DRAFT', 'REGISTERED')
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM CUSTOMER_RETURN_ITEM customer_return_item
			        JOIN CUSTOMER_RETURN customer_return
			          ON customer_return.customer_return_id = customer_return_item.customer_return_id
			        WHERE customer_return_item.item_id = :itemId
			          AND customer_return.status = 'REGISTERED'
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM STOCKTAKE stocktake
			        WHERE stocktake.status IN ('PENDING', 'COUNTING', 'RESULT_REGISTERED')
			          AND (
			              stocktake.target_item_id = :itemId
			              OR EXISTS (
			                  SELECT 1
			                  FROM STOCKTAKE_ITEM stocktake_item
			                  JOIN INVENTORY_LOT inventory_lot
			                    ON inventory_lot.inventory_lot_id = stocktake_item.inventory_lot_id
			                  WHERE stocktake_item.stocktake_id = stocktake.stocktake_id
			                    AND inventory_lot.item_id = :itemId
			              )
			          )
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM STOCK_ADJUSTMENT stock_adjustment
			        JOIN INVENTORY_LOT inventory_lot
			          ON inventory_lot.inventory_lot_id = stock_adjustment.inventory_lot_id
			        WHERE inventory_lot.item_id = :itemId
			          AND stock_adjustment.status = 'SUBMITTED'
			    )
			FROM DUAL
			""", nativeQuery = true)
	long countOngoingBusinessReferences(@Param("itemId") Long itemId);
}
