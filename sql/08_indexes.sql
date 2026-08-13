/* =========================================================
   08_indexes.sql
   FK 및 주요 업무 조회 인덱스 생성
   ========================================================= */


/* =========================================================
   1. 공통·기준정보
   ========================================================= */

/* APP_USER */
CREATE INDEX IX_APP_USER_CREATED_BY
    ON APP_USER (created_by);

CREATE INDEX IX_APP_USER_UPDATED_BY
    ON APP_USER (updated_by);


/* CUSTOMER */
CREATE INDEX IX_CUSTOMER_CREATED_BY
    ON CUSTOMER (created_by);

CREATE INDEX IX_CUSTOMER_UPDATED_BY
    ON CUSTOMER (updated_by);

CREATE INDEX IX_CUSTOMER_STATUS
    ON CUSTOMER (status);

CREATE INDEX IX_CUSTOMER_TRADE_STATUS
    ON CUSTOMER (trade_status);


/* CUSTOMER_TRADE_STATUS_HISTORY */
CREATE INDEX IX_CUST_TRADE_HIST_CUSTOMER
    ON CUSTOMER_TRADE_STATUS_HISTORY (customer_id);

CREATE INDEX IX_CUST_TRADE_HIST_CHANGED_BY
    ON CUSTOMER_TRADE_STATUS_HISTORY (changed_by);


/* SUPPLIER */
CREATE INDEX IX_SUPPLIER_CREATED_BY
    ON SUPPLIER (created_by);

CREATE INDEX IX_SUPPLIER_UPDATED_BY
    ON SUPPLIER (updated_by);


/* ITEM */
CREATE INDEX IX_ITEM_CREATED_BY
    ON ITEM (created_by);

CREATE INDEX IX_ITEM_UPDATED_BY
    ON ITEM (updated_by);


/* SUPPLIER_ITEM
   UNIQUE(supplier_id, item_id)가 supplier_id 조회를 담당하고,
   아래 인덱스는 품목 기준 공급업체 조회용
*/
CREATE INDEX IX_SUPPLIER_ITEM_ITEM_SUPPLIER
    ON SUPPLIER_ITEM (item_id, supplier_id);

CREATE INDEX IX_SUPPLIER_ITEM_CREATED_BY
    ON SUPPLIER_ITEM (created_by);


/* WAREHOUSE */
CREATE INDEX IX_WAREHOUSE_CREATED_BY
    ON WAREHOUSE (created_by);

CREATE INDEX IX_WAREHOUSE_UPDATED_BY
    ON WAREHOUSE (updated_by);


/* WAREHOUSE_ITEM
   UNIQUE(warehouse_id, item_id)가 warehouse_id 기준 조회를 담당
*/
CREATE INDEX IX_WAREHOUSE_ITEM_ITEM
    ON WAREHOUSE_ITEM (item_id);

CREATE INDEX IX_WAREHOUSE_ITEM_CREATED_BY
    ON WAREHOUSE_ITEM (created_by);

CREATE INDEX IX_WAREHOUSE_ITEM_UPDATED_BY
    ON WAREHOUSE_ITEM (updated_by);


/* =========================================================
   2. 구매
   ========================================================= */

/* PURCHASE_ORDER */
CREATE INDEX IX_PO_STATUS_CREATED
    ON PURCHASE_ORDER (status, created_at);

CREATE INDEX IX_PO_SUPPLIER
    ON PURCHASE_ORDER (supplier_id);

CREATE INDEX IX_PO_SUBMITTED_BY
    ON PURCHASE_ORDER (submitted_by);

CREATE INDEX IX_PO_APPROVED_BY
    ON PURCHASE_ORDER (approved_by);

CREATE INDEX IX_PO_ORDERED_BY
    ON PURCHASE_ORDER (ordered_by);

CREATE INDEX IX_PO_CANCELED_BY
    ON PURCHASE_ORDER (canceled_by);

CREATE INDEX IX_PO_CLOSED_BY
    ON PURCHASE_ORDER (closed_by);

CREATE INDEX IX_PO_SUP_CANCEL_CONF_BY
    ON PURCHASE_ORDER (supplier_cancel_confirmed_by);

CREATE INDEX IX_PO_CREATED_BY
    ON PURCHASE_ORDER (created_by);


/* PURCHASE_ORDER_ITEM
   purchase_order_id는 기존 UNIQUE 인덱스의 선두 컬럼이므로
   별도 중복 인덱스를 만들지 않는다.
*/
CREATE INDEX IX_PO_ITEM_ITEM
    ON PURCHASE_ORDER_ITEM (item_id);


/* PURCHASE_ORDER_EMAIL_HISTORY
   purchase_order_id는 UNIQUE(purchase_order_id, attempt_no)로 처리
*/
CREATE INDEX IX_PO_EMAIL_ATTEMPTED_BY
    ON PURCHASE_ORDER_EMAIL_HISTORY (attempted_by);


/* RECEIPT */
CREATE INDEX IX_RECEIPT_PO_STATUS
    ON RECEIPT (purchase_order_id, status);

CREATE INDEX IX_RECEIPT_WAREHOUSE
    ON RECEIPT (warehouse_id);

CREATE INDEX IX_RECEIPT_STARTED_BY
    ON RECEIPT (inspection_started_by);

CREATE INDEX IX_RECEIPT_COMPLETED_BY
    ON RECEIPT (completed_by);

CREATE INDEX IX_RECEIPT_CANCELED_BY
    ON RECEIPT (canceled_by);

CREATE INDEX IX_RECEIPT_CREATED_BY
    ON RECEIPT (created_by);


/* RECEIPT_ITEM
   receipt_id는 UNIQUE(receipt_id, purchase_order_item_id)로 처리
*/
CREATE INDEX IX_RECEIPT_ITEM_PO_ITEM
    ON RECEIPT_ITEM (purchase_order_item_id);


/* RECEIPT_LOT
   receipt_item_id는 기존 UNIQUE 인덱스로 처리
*/
CREATE INDEX IX_RECEIPT_LOT_INV_LOT
    ON RECEIPT_LOT (inventory_lot_id);


/* PURCHASE_RETURN */
CREATE INDEX IX_PUR_RETURN_RECEIPT
    ON PURCHASE_RETURN (receipt_id);

CREATE INDEX IX_PUR_RETURN_COMPLETED_BY
    ON PURCHASE_RETURN (completed_by);

CREATE INDEX IX_PUR_RETURN_CANCELED_BY
    ON PURCHASE_RETURN (canceled_by);

CREATE INDEX IX_PUR_RETURN_CREATED_BY
    ON PURCHASE_RETURN (created_by);


/* PURCHASE_RETURN_ITEM */
CREATE INDEX IX_PUR_RETURN_ITEM_RECEIPT_LOT
    ON PURCHASE_RETURN_ITEM (receipt_lot_id);

CREATE INDEX IX_PUR_RETURN_ITEM_INV_LOT
    ON PURCHASE_RETURN_ITEM (inventory_lot_id);

CREATE INDEX IX_PUR_RETURN_ITEM_ITEM
    ON PURCHASE_RETURN_ITEM (item_id);


/* =========================================================
   3. 판매
   ========================================================= */

/* SALES_ORDER */
CREATE INDEX IX_SALES_ORDER_CUST_STATUS_REG
    ON SALES_ORDER (
        customer_id,
        status,
        registered_at
    );

CREATE INDEX IX_SALES_ORDER_REGISTERED_BY
    ON SALES_ORDER (registered_by);

CREATE INDEX IX_SALES_ORDER_CANCELED_BY
    ON SALES_ORDER (canceled_by);

CREATE INDEX IX_SALES_ORDER_CREATED_BY
    ON SALES_ORDER (created_by);


/* SALES_ORDER_ITEM */
CREATE INDEX IX_SALES_ORDER_ITEM_ITEM
    ON SALES_ORDER_ITEM (item_id);


/* SHIPMENT */
CREATE INDEX IX_SHIPMENT_STATUS_CREATED
    ON SHIPMENT (status, created_at);

CREATE INDEX IX_SHIPMENT_WAREHOUSE
    ON SHIPMENT (warehouse_id);

CREATE INDEX IX_SHIPMENT_PACKED_BY
    ON SHIPMENT (packed_by);

CREATE INDEX IX_SHIPMENT_COMPLETED_BY
    ON SHIPMENT (completed_by);

CREATE INDEX IX_SHIPMENT_CANCELED_BY
    ON SHIPMENT (canceled_by);


/* SHIPMENT_LOT */
CREATE INDEX IX_SHIPMENT_LOT_INV_RESERVED
    ON SHIPMENT_LOT (
        inventory_lot_id,
        reserved_yn
    );

CREATE INDEX IX_SHIPMENT_LOT_ORDER_ITEM
    ON SHIPMENT_LOT (sales_order_item_id);


/* DELIVERY_NOTE */
CREATE INDEX IX_DELIVERY_NOTE_ISSUED_BY
    ON DELIVERY_NOTE (issued_by);

CREATE INDEX IX_DELIVERY_NOTE_VOIDED_BY
    ON DELIVERY_NOTE (voided_by);


/* CUSTOMER_RETURN */
CREATE INDEX IX_CUST_RETURN_SHIPMENT
    ON CUSTOMER_RETURN (shipment_id);

CREATE INDEX IX_CUST_RETURN_WAREHOUSE
    ON CUSTOMER_RETURN (warehouse_id);

CREATE INDEX IX_CUST_RETURN_COMPLETED_BY
    ON CUSTOMER_RETURN (completed_by);

CREATE INDEX IX_CUST_RETURN_NOT_ALLOWED_BY
    ON CUSTOMER_RETURN (not_allowed_by);

CREATE INDEX IX_CUST_RETURN_CREATED_BY
    ON CUSTOMER_RETURN (created_by);


/* CUSTOMER_RETURN_ITEM */
CREATE INDEX IX_CUST_RETURN_ITEM_SHIP_LOT
    ON CUSTOMER_RETURN_ITEM (shipment_lot_id);

CREATE INDEX IX_CUST_RETURN_ITEM_ORDER_ITEM
    ON CUSTOMER_RETURN_ITEM (sales_order_item_id);

CREATE INDEX IX_CUST_RETURN_ITEM_INV_LOT
    ON CUSTOMER_RETURN_ITEM (inventory_lot_id);

CREATE INDEX IX_CUST_RETURN_ITEM_ITEM
    ON CUSTOMER_RETURN_ITEM (item_id);


/* =========================================================
   4. 재고
   ========================================================= */

/* INVENTORY_LOT */
CREATE INDEX IX_INV_LOT_WH_ITEM_STATUS_EXP
    ON INVENTORY_LOT (
        warehouse_id,
        item_id,
        status,
        expiry_date,
        created_at
    );

CREATE INDEX IX_INV_LOT_ITEM
    ON INVENTORY_LOT (item_id);

CREATE INDEX IX_INV_LOT_SUPPLIER
    ON INVENTORY_LOT (supplier_id);

CREATE INDEX IX_INV_LOT_CREATED_BY
    ON INVENTORY_LOT (created_by);


/* 내부 생성 LOT 번호는 시스템 전체에서 중복 불가 */
CREATE UNIQUE INDEX UX_INV_LOT_INTERNAL_LOT
    ON INVENTORY_LOT (
        CASE
            WHEN internal_lot_yn = 'Y'
            THEN lot_number
        END
    );


/* LOT_STATUS_HISTORY */
CREATE INDEX IX_LOT_STATUS_HIST_INV_LOT
    ON LOT_STATUS_HISTORY (inventory_lot_id);

CREATE INDEX IX_LOT_STATUS_HIST_CHANGED_BY
    ON LOT_STATUS_HISTORY (changed_by);


/* STOCK_MOVEMENT */
CREATE INDEX IX_STOCK_MOVEMENT_LOT_TIME
    ON STOCK_MOVEMENT (
        inventory_lot_id,
        processed_at
    );

CREATE INDEX IX_STOCK_MOVEMENT_WAREHOUSE
    ON STOCK_MOVEMENT (warehouse_id);

CREATE INDEX IX_STOCK_MOVEMENT_ITEM
    ON STOCK_MOVEMENT (item_id);

CREATE INDEX IX_STOCK_MOVEMENT_RECEIPT_LOT
    ON STOCK_MOVEMENT (receipt_lot_id);

CREATE INDEX IX_STOCK_MOVEMENT_SHIPMENT_LOT
    ON STOCK_MOVEMENT (shipment_lot_id);

CREATE INDEX IX_STOCK_MOVEMENT_PUR_RETURN
    ON STOCK_MOVEMENT (purchase_return_item_id);

CREATE INDEX IX_STOCK_MOVEMENT_CUST_RETURN
    ON STOCK_MOVEMENT (customer_return_item_id);

CREATE INDEX IX_STOCK_MOVEMENT_ADJUSTMENT
    ON STOCK_MOVEMENT (stock_adjustment_id);

CREATE INDEX IX_STOCK_MOVEMENT_PROCESSED_BY
    ON STOCK_MOVEMENT (processed_by);


/* STOCKTAKE */
CREATE INDEX IX_STOCKTAKE_WH_STATUS
    ON STOCKTAKE (
        warehouse_id,
        status
    );

CREATE INDEX IX_STOCKTAKE_TARGET_ITEM
    ON STOCKTAKE (target_item_id);

CREATE INDEX IX_STOCKTAKE_STARTED_BY
    ON STOCKTAKE (started_by);

CREATE INDEX IX_STOCKTAKE_CANCELED_BY
    ON STOCKTAKE (canceled_by);

CREATE INDEX IX_STOCKTAKE_CREATED_BY
    ON STOCKTAKE (created_by);


/* STOCKTAKE_ITEM */
CREATE INDEX IX_STOCKTAKE_ITEM_LOT_RELEASED
    ON STOCKTAKE_ITEM (
        inventory_lot_id,
        released_at
    );


/* STOCK_ADJUSTMENT */
CREATE INDEX IX_STOCK_ADJUSTMENT_INV_LOT
    ON STOCK_ADJUSTMENT (inventory_lot_id);

CREATE INDEX IX_STOCK_ADJUSTMENT_PROCESSED_BY
    ON STOCK_ADJUSTMENT (processed_by);


/* =========================================================
   5. 정산
   ========================================================= */

/* VOUCHER */
CREATE INDEX IX_VOUCHER_CUST_TYPE_DATE
    ON VOUCHER (
        customer_id,
        type,
        voucher_date
    );

CREATE INDEX IX_VOUCHER_SUP_TYPE_DATE
    ON VOUCHER (
        supplier_id,
        type,
        voucher_date
    );

CREATE INDEX IX_VOUCHER_CUST_SETTLE_DATE
    ON VOUCHER (
        customer_id,
        settlement_status,
        voucher_date,
        voucher_id
    );

CREATE INDEX IX_VOUCHER_ORIGINAL
    ON VOUCHER (original_voucher_id);


/* VOUCHER_ITEM */
CREATE INDEX IX_VOUCHER_ITEM_ITEM
    ON VOUCHER_ITEM (item_id);


/* PAYMENT */
CREATE INDEX IX_PAYMENT_CUST_STATUS_DATE
    ON PAYMENT (
        customer_id,
        status,
        payment_date
    );

CREATE INDEX IX_PAYMENT_CREATED_BY
    ON PAYMENT (created_by);

CREATE INDEX IX_PAYMENT_CANCELED_BY
    ON PAYMENT (canceled_by);


/* PAYMENT_ALLOCATION */
CREATE INDEX IX_PAYMENT_ALLOC_VOUCHER_RELEASED
    ON PAYMENT_ALLOCATION (
        voucher_id,
        released_at
    );

CREATE INDEX IX_PAYMENT_ALLOC_PAYMENT
    ON PAYMENT_ALLOCATION (payment_id);

CREATE INDEX IX_PAYMENT_ALLOC_ALLOCATED_BY
    ON PAYMENT_ALLOCATION (allocated_by);

CREATE INDEX IX_PAYMENT_ALLOC_RELEASED_BY
    ON PAYMENT_ALLOCATION (released_by);