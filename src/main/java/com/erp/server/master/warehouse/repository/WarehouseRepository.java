package com.erp.server.master.warehouse.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.warehouse.domain.Warehouse;

import jakarta.persistence.LockModeType;

// ********** WAREHOUSE의 기본 CRUD와 키워드·사용 상태 조건을 적용한 창고 목록 조회를 처리하기 위한 Repository interface **********
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

	// ========== 창고 코드·창고명·기본 주소와 사용 상태를 조건으로 창고 목록을 페이지 조회하는 메서드 ==========
	// keyword는 앞뒤 공백을 제거한 후 창고 코드, 창고명과 기본 주소를 대소문자 구분 없이 부분 검색한다.
	// keyword와 status가 함께 전달되면 두 조건을 모두 적용한다.
	@Query("""
			select w
			from Warehouse w
			where (
			    :keyword is null
			    or lower(w.warehouseCode) like lower(concat('%', :keyword, '%'))
			    or lower(w.warehouseName) like lower(concat('%', :keyword, '%'))
			    or lower(w.address) like lower(concat('%', :keyword, '%'))
			)
			  and (:status is null or w.status = :status)
			""")
	Page<Warehouse> findAllByFilters(@Param("keyword") String keyword, @Param("status") MasterStatus status,
			Pageable pageable);

	// ========== 창고 상태 변경과 최신 상태 검증 중 동시 처리를 막기 위해 PESSIMISTIC_WRITE 비관적 잠금으로 조회하는 메서드 ==========
	// 먼저 잠금을 얻은 트랜잭션이 끝날 때까지 같은 창고를 잠금 조회하는 다른 요청은 대기한다.
	// ACTIVE/INACTIVE 변경과 이후 입고·출고처럼 최신 창고 사용 상태 검증이 필요한 업무에서 사용한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select w
			from Warehouse w
			where w.warehouseId = :warehouseId
			""")
	Optional<Warehouse> findByIdForUpdate(@Param("warehouseId") Long warehouseId);

	// ========== Oracle의 창고 업무 코드 Sequence를 이용하여 WH + 6자리 형식의 다음 창고 코드를 생성하는 메서드 ==========
	// 실제 PK용 SEQ_WAREHOUSE와 업무 코드용 SEQ_WAREHOUSE_CODE를 분리한다.
	@Query(value = """
			SELECT 'WH' || LPAD(SEQ_WAREHOUSE_CODE.NEXTVAL, 6, '0')
			FROM DUAL
			""", nativeQuery = true)
	String generateWarehouseCode();

	// ========== 창고 사용 중지를 차단하는 현재 재고와 진행 입고·반품·출고·재고 업무의 전체 건수를 조회하는 메서드 ==========
	/**
	 * 현재 수량이 0보다 큰 재고 LOT
	 * PENDING, INSPECTING 입고
	 * REGISTERED 매입 반품
	 * PENDING, PACKED 출고
	 * REGISTERED 거래처 반품
	 * PENDING, COUNTING, RESULT_REGISTERED 재고 실사
	 * SUBMITTED 재고 조정
	 */
	@Query(value = """
			SELECT
			    (
			        SELECT COUNT(*)
			        FROM INVENTORY_LOT inventory_lot
			        WHERE inventory_lot.warehouse_id = :warehouseId
			          AND inventory_lot.current_quantity > 0
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM RECEIPT receipt
			        WHERE receipt.warehouse_id = :warehouseId
			          AND receipt.status IN ('PENDING', 'INSPECTING')
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM PURCHASE_RETURN purchase_return
			        JOIN RECEIPT receipt
			          ON receipt.receipt_id = purchase_return.receipt_id
			        WHERE receipt.warehouse_id = :warehouseId
			          AND purchase_return.status = 'REGISTERED'
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM SHIPMENT shipment
			        WHERE shipment.warehouse_id = :warehouseId
			          AND shipment.status IN ('PENDING', 'PACKED')
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM CUSTOMER_RETURN customer_return
			        WHERE customer_return.warehouse_id = :warehouseId
			          AND customer_return.status = 'REGISTERED'
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM STOCKTAKE stocktake
			        WHERE stocktake.warehouse_id = :warehouseId
			          AND stocktake.status IN ('PENDING', 'COUNTING', 'RESULT_REGISTERED')
			    )
			    +
			    (
			        SELECT COUNT(*)
			        FROM STOCK_ADJUSTMENT stock_adjustment
			        JOIN INVENTORY_LOT inventory_lot
			          ON inventory_lot.inventory_lot_id = stock_adjustment.inventory_lot_id
			        WHERE inventory_lot.warehouse_id = :warehouseId
			          AND stock_adjustment.status = 'SUBMITTED'
			    )
			FROM DUAL
			""", nativeQuery = true)
	long countStockAndOngoingBusinessReferences(@Param("warehouseId") Long warehouseId);
}
