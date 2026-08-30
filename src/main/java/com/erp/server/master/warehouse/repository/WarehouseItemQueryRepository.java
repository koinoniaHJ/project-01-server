package com.erp.server.master.warehouse.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.erp.server.master.item.domain.ItemUnit;
import com.erp.server.master.warehouse.dto.WarehouseItemListResponse;

// ********** 사용 중인 창고·품목 조합과 LOT 가용재고를 함께 계산하여 안전재고 화면용 목록을 조회하기 위한 읽기 전용 Repository 클래스 **********
@Repository
public class WarehouseItemQueryRepository {

	// 출고 가능한 LOT 중 사용기한과 실사·조정 제한 조건을 충족하는 수량만 창고·품목별로 합산한다.
	// STOCKTAKE_ITEM.released_at이 null인 제한은 실사 또는 후속 재고 조정이 끝나지 않은 상태이므로 가용재고에서 제외한다.
	private static final String SAFETY_STOCK_BASE_QUERY = """
			WITH available_stock AS (
			    SELECT
			        inventory_lot.warehouse_id,
			        inventory_lot.item_id,
			        SUM(
			            CASE
			                WHEN inventory_lot.status = 'AVAILABLE'
			                 AND TRUNC(inventory_lot.expiry_date) >= TRUNC(CURRENT_DATE)
			                 AND NOT EXISTS (
			                     SELECT 1
			                     FROM STOCKTAKE_ITEM stocktake_item
			                     WHERE stocktake_item.inventory_lot_id = inventory_lot.inventory_lot_id
			                       AND stocktake_item.restricted_at IS NOT NULL
			                       AND stocktake_item.released_at IS NULL
			                 )
			                THEN inventory_lot.current_quantity - inventory_lot.reserved_quantity
			                ELSE 0
			            END
			        ) AS available_stock_quantity
			    FROM INVENTORY_LOT inventory_lot
			    WHERE (:warehouseId IS NULL OR inventory_lot.warehouse_id = :warehouseId)
			      AND (:itemId IS NULL OR inventory_lot.item_id = :itemId)
			    GROUP BY inventory_lot.warehouse_id, inventory_lot.item_id
			), safety_stock_list AS (
			    SELECT
			        warehouse.warehouse_id,
			        warehouse.warehouse_code,
			        warehouse.warehouse_name,
			        item.item_id,
			        item.item_code,
			        item.item_name,
			        item.unit,
			        item.other_unit_name,
			        COALESCE(warehouse_item.safety_stock_quantity, 0) AS safety_stock_quantity,
			        COALESCE(available_stock.available_stock_quantity, 0) AS available_stock_quantity,
			        GREATEST(
			            COALESCE(warehouse_item.safety_stock_quantity, 0)
			            - COALESCE(available_stock.available_stock_quantity, 0),
			            0
			        ) AS shortage_quantity,
			        CASE
			            WHEN COALESCE(available_stock.available_stock_quantity, 0)
			               < COALESCE(warehouse_item.safety_stock_quantity, 0)
			            THEN 1
			            ELSE 0
			        END AS below_safety_stock,
			        warehouse_item.version
			    FROM WAREHOUSE warehouse
			    CROSS JOIN ITEM item
			    LEFT JOIN WAREHOUSE_ITEM warehouse_item
			      ON warehouse_item.warehouse_id = warehouse.warehouse_id
			     AND warehouse_item.item_id = item.item_id
			    LEFT JOIN available_stock
			      ON available_stock.warehouse_id = warehouse.warehouse_id
			     AND available_stock.item_id = item.item_id
			    WHERE warehouse.status = 'ACTIVE'
			      AND item.status = 'ACTIVE'
			      AND (:warehouseId IS NULL OR warehouse.warehouse_id = :warehouseId)
			      AND (:itemId IS NULL OR item.item_id = :itemId)
			)
			""";

	// 미등록 WAREHOUSE_ITEM 조합도 안전재고 0과 version null로 포함하고 서버 고정 정렬과 페이지 범위를 적용한다.
	private static final String SAFETY_STOCK_LIST_QUERY = SAFETY_STOCK_BASE_QUERY + """
			SELECT
			    warehouse_id,
			    warehouse_code,
			    warehouse_name,
			    item_id,
			    item_code,
			    item_name,
			    unit,
			    other_unit_name,
			    safety_stock_quantity,
			    available_stock_quantity,
			    shortage_quantity,
			    below_safety_stock,
			    version
			FROM safety_stock_list
			WHERE (:belowSafetyStock IS NULL OR below_safety_stock = :belowSafetyStock)
			ORDER BY warehouse_code ASC, item_code ASC
			OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY
			""";

	// 목록과 같은 조건을 사용하여 PageMeta에 필요한 전체 창고·품목 조합 건수를 계산한다.
	private static final String SAFETY_STOCK_COUNT_QUERY = SAFETY_STOCK_BASE_QUERY + """
			SELECT COUNT(*)
			FROM safety_stock_list
			WHERE (:belowSafetyStock IS NULL OR below_safety_stock = :belowSafetyStock)
			""";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public WarehouseItemQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	// ========== 창고·품목·안전재고 미달 조건을 적용하여 안전재고 목록을 페이지 조회하는 메서드 ==========
	public Page<WarehouseItemListResponse> findAllByFilters(Long warehouseId, Long itemId,
			Boolean belowSafetyStock, Pageable pageable) {

		Integer belowSafetyStockValue = belowSafetyStock == null ? null : belowSafetyStock ? 1 : 0;
		MapSqlParameterSource parameters = new MapSqlParameterSource()
				// Oracle가 null 선택 조건의 NUMBER 타입을 정확히 판별할 수 있도록 JDBC 타입을 함께 지정한다.
				.addValue("warehouseId", warehouseId, Types.NUMERIC)
				.addValue("itemId", itemId, Types.NUMERIC)
				.addValue("belowSafetyStock", belowSafetyStockValue, Types.NUMERIC)
				.addValue("offset", pageable.getOffset(), Types.NUMERIC)
				.addValue("pageSize", pageable.getPageSize(), Types.NUMERIC);

		List<WarehouseItemListResponse> content = jdbcTemplate.query(SAFETY_STOCK_LIST_QUERY, parameters,
				this::mapWarehouseItemListResponse);
		Long totalElements = jdbcTemplate.queryForObject(SAFETY_STOCK_COUNT_QUERY, parameters, Long.class);

		return new PageImpl<>(content, pageable, totalElements == null ? 0 : totalElements);
	}

	// ========== JDBC 조회 결과 한 행을 안전재고 목록 응답으로 변환하는 메서드 ==========
	private WarehouseItemListResponse mapWarehouseItemListResponse(ResultSet resultSet, int rowNumber)
			throws SQLException {

		Number version = (Number) resultSet.getObject("version");

		return new WarehouseItemListResponse(
				resultSet.getLong("warehouse_id"),
				resultSet.getString("warehouse_code"),
				resultSet.getString("warehouse_name"),
				resultSet.getLong("item_id"),
				resultSet.getString("item_code"),
				resultSet.getString("item_name"),
				ItemUnit.valueOf(resultSet.getString("unit")),
				resultSet.getString("other_unit_name"),
				resultSet.getBigDecimal("safety_stock_quantity"),
				resultSet.getBigDecimal("available_stock_quantity"),
				resultSet.getBigDecimal("shortage_quantity"),
				resultSet.getInt("below_safety_stock") == 1,
				version == null ? null : version.longValue());
	}
}
