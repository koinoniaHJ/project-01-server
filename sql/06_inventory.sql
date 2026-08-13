/* =========================================================
   06_inventory.sql
   재고 업무 테이블 생성
   ========================================================= */


/* =========================================================
   1. INVENTORY_LOT
   재고 LOT
   ========================================================= */

CREATE TABLE INVENTORY_LOT (
    inventory_lot_id       NUMBER(19)       NOT NULL,
    warehouse_id           NUMBER(19)       NOT NULL,
    item_id                NUMBER(19)       NOT NULL,
    supplier_id            NUMBER(19)       NOT NULL,
    lot_number             VARCHAR2(100)    NOT NULL,
    supplier_lot_number    VARCHAR2(100),
    internal_lot_yn        CHAR(1)          DEFAULT 'N' NOT NULL,
    expiry_date            DATE             NOT NULL,
    status                 VARCHAR2(20)     DEFAULT 'AVAILABLE' NOT NULL,
    current_quantity       NUMBER(19,3)     DEFAULT 0 NOT NULL,
    reserved_quantity      NUMBER(19,3)     DEFAULT 0 NOT NULL,
    created_by             NUMBER(19)       NOT NULL,
    created_at             TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version                NUMBER(19)       DEFAULT 0 NOT NULL,

    CONSTRAINT PK_INVENTORY_LOT
        PRIMARY KEY (inventory_lot_id),

    CONSTRAINT UK_INVENTORY_LOT
        UNIQUE (
            warehouse_id,
            item_id,
            supplier_id,
            lot_number
        ),

    CONSTRAINT CK_INVENTORY_LOT_STATUS
        CHECK (
            status IN ('AVAILABLE', 'BLOCKED')
        ),

    CONSTRAINT CK_INVENTORY_LOT_INTERNAL
        CHECK (
            internal_lot_yn IN ('Y', 'N')
        ),

    CONSTRAINT CK_INVENTORY_LOT_SOURCE
        CHECK (
            (internal_lot_yn = 'Y'
                AND supplier_lot_number IS NULL)
            OR
            (internal_lot_yn = 'N'
                AND supplier_lot_number IS NOT NULL
                AND lot_number = supplier_lot_number)
        ),

    CONSTRAINT CK_INVENTORY_LOT_CURRENT_QTY
        CHECK (
            current_quantity >= 0
        ),

    CONSTRAINT CK_INVENTORY_LOT_RESERVED_QTY
        CHECK (
            reserved_quantity >= 0
        ),

    CONSTRAINT CK_INVENTORY_LOT_RESERVED_CURRENT
        CHECK (
            reserved_quantity <= current_quantity
        ),

    CONSTRAINT FK_INVENTORY_LOT_WAREHOUSE
        FOREIGN KEY (warehouse_id)
        REFERENCES WAREHOUSE(warehouse_id),

    CONSTRAINT FK_INVENTORY_LOT_ITEM
        FOREIGN KEY (item_id)
        REFERENCES ITEM(item_id),

    CONSTRAINT FK_INVENTORY_LOT_SUPPLIER
        FOREIGN KEY (supplier_id)
        REFERENCES SUPPLIER(supplier_id),

    CONSTRAINT FK_INVENTORY_LOT_CREATED_BY
        FOREIGN KEY (created_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   2. LOT_STATUS_HISTORY
   LOT 상태 이력
   ========================================================= */

CREATE TABLE LOT_STATUS_HISTORY (
    lot_status_history_id    NUMBER(19)       NOT NULL,
    inventory_lot_id         NUMBER(19)       NOT NULL,
    previous_status          VARCHAR2(20)     NOT NULL,
    changed_status           VARCHAR2(20)     NOT NULL,
    reason                   VARCHAR2(1000)   NOT NULL,
    changed_by               NUMBER(19)       NOT NULL,
    changed_at               TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT PK_LOT_STATUS_HISTORY
        PRIMARY KEY (lot_status_history_id),

    CONSTRAINT CK_LOT_STATUS_HIST_PREVIOUS
        CHECK (
            previous_status IN ('AVAILABLE', 'BLOCKED')
        ),

    CONSTRAINT CK_LOT_STATUS_HIST_CHANGED
        CHECK (
            changed_status IN ('AVAILABLE', 'BLOCKED')
        ),

    CONSTRAINT CK_LOT_STATUS_HIST_CHANGE
        CHECK (
            previous_status <> changed_status
        ),

    CONSTRAINT FK_LOT_STATUS_HIST_LOT
        FOREIGN KEY (inventory_lot_id)
        REFERENCES INVENTORY_LOT(inventory_lot_id),

    CONSTRAINT FK_LOT_STATUS_HIST_CHANGED_BY
        FOREIGN KEY (changed_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   3. STOCKTAKE
   재고 실사
   ========================================================= */

CREATE TABLE STOCKTAKE (
    stocktake_id         NUMBER(19)       NOT NULL,
    warehouse_id        NUMBER(19)       NOT NULL,
    scope               VARCHAR2(20)     NOT NULL,
    target_item_id      NUMBER(19),
    status              VARCHAR2(30)     DEFAULT 'PENDING' NOT NULL,

    started_by          NUMBER(19),
    started_at          TIMESTAMP,

    completed_at        TIMESTAMP,

    canceled_by         NUMBER(19),
    canceled_at         TIMESTAMP,
    cancel_reason       VARCHAR2(1000),

    created_by          NUMBER(19)       NOT NULL,
    created_at          TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version             NUMBER(19)       DEFAULT 0 NOT NULL,

    CONSTRAINT PK_STOCKTAKE
        PRIMARY KEY (stocktake_id),

    CONSTRAINT CK_STOCKTAKE_SCOPE
        CHECK (
            scope IN ('PARTIAL', 'FULL')
        ),

    CONSTRAINT CK_STOCKTAKE_SCOPE_ITEM
        CHECK (
            (scope = 'PARTIAL' AND target_item_id IS NOT NULL)
            OR
            (scope = 'FULL' AND target_item_id IS NULL)
        ),

    CONSTRAINT CK_STOCKTAKE_STATUS
        CHECK (
            status IN (
                'PENDING',
                'COUNTING',
                'RESULT_REGISTERED',
                'COMPLETED',
                'CANCELED'
            )
        ),

    CONSTRAINT FK_STOCKTAKE_WAREHOUSE
        FOREIGN KEY (warehouse_id)
        REFERENCES WAREHOUSE(warehouse_id),

    CONSTRAINT FK_STOCKTAKE_TARGET_ITEM
        FOREIGN KEY (target_item_id)
        REFERENCES ITEM(item_id),

    CONSTRAINT FK_STOCKTAKE_STARTED_BY
        FOREIGN KEY (started_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_STOCKTAKE_CANCELED_BY
        FOREIGN KEY (canceled_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_STOCKTAKE_CREATED_BY
        FOREIGN KEY (created_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   4. STOCKTAKE_ITEM
   재고 실사 대상 LOT
   ========================================================= */

CREATE TABLE STOCKTAKE_ITEM (
    stocktake_item_id       NUMBER(19)      NOT NULL,
    stocktake_id            NUMBER(19)      NOT NULL,
    inventory_lot_id        NUMBER(19)      NOT NULL,
    base_quantity           NUMBER(19,3),
    counted_quantity        NUMBER(19,3),
    difference_quantity     NUMBER(19,3),
    restricted_at           TIMESTAMP,
    released_at             TIMESTAMP,
    created_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT PK_STOCKTAKE_ITEM
        PRIMARY KEY (stocktake_item_id),

    CONSTRAINT UK_STOCKTAKE_ITEM_LOT
        UNIQUE (
            stocktake_id,
            inventory_lot_id
        ),

    CONSTRAINT CK_STOCKTAKE_ITEM_BASE_QTY
        CHECK (
            base_quantity >= 0
        ),

    CONSTRAINT CK_STOCKTAKE_ITEM_COUNT_QTY
        CHECK (
            counted_quantity >= 0
        ),

    CONSTRAINT CK_STOCKTAKE_ITEM_DIFF_QTY
        CHECK (
            difference_quantity IS NULL
            OR (
                base_quantity IS NOT NULL
                AND counted_quantity IS NOT NULL
                AND difference_quantity =
                    counted_quantity - base_quantity
            )
        ),

    CONSTRAINT FK_STOCKTAKE_ITEM_STOCKTAKE
        FOREIGN KEY (stocktake_id)
        REFERENCES STOCKTAKE(stocktake_id),

    CONSTRAINT FK_STOCKTAKE_ITEM_INV_LOT
        FOREIGN KEY (inventory_lot_id)
        REFERENCES INVENTORY_LOT(inventory_lot_id)
);


/* =========================================================
   5. STOCK_ADJUSTMENT
   재고 조정
   ========================================================= */

CREATE TABLE STOCK_ADJUSTMENT (
    stock_adjustment_id     NUMBER(19)       NOT NULL,
    stocktake_item_id       NUMBER(19)       NOT NULL,
    inventory_lot_id        NUMBER(19)       NOT NULL,
    status                  VARCHAR2(30)     DEFAULT 'SUBMITTED' NOT NULL,
    before_quantity         NUMBER(19,3)     NOT NULL,
    counted_quantity        NUMBER(19,3)     NOT NULL,
    difference_quantity     NUMBER(19,3)     NOT NULL,
    movement_type           VARCHAR2(30),
    reason                  VARCHAR2(1000),
    processed_by            NUMBER(19),
    processed_at            TIMESTAMP,
    created_at              TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version                 NUMBER(19)       DEFAULT 0 NOT NULL,

    CONSTRAINT PK_STOCK_ADJUSTMENT
        PRIMARY KEY (stock_adjustment_id),

    CONSTRAINT UK_STOCK_ADJUSTMENT_ITEM
        UNIQUE (stocktake_item_id),

    CONSTRAINT CK_STOCK_ADJUSTMENT_STATUS
        CHECK (
            status IN (
                'SUBMITTED',
                'APPROVED',
                'NOT_REQUIRED'
            )
        ),

    CONSTRAINT CK_STOCK_ADJUSTMENT_MOVEMENT
        CHECK (
            movement_type IN (
                'ADJUSTMENT_IN',
                'ADJUSTMENT_OUT'
            )
        ),

    CONSTRAINT CK_STOCK_ADJUSTMENT_BEFORE_QTY
        CHECK (
            before_quantity >= 0
        ),

    CONSTRAINT CK_STOCK_ADJUSTMENT_COUNT_QTY
        CHECK (
            counted_quantity >= 0
        ),

    CONSTRAINT CK_STOCK_ADJUSTMENT_DIFF
        CHECK (
            difference_quantity <> 0
        ),

    CONSTRAINT CK_STOCK_ADJUSTMENT_DIRECTION
        CHECK (
            movement_type IS NULL
            OR (
                movement_type = 'ADJUSTMENT_IN'
                AND difference_quantity > 0
            )
            OR (
                movement_type = 'ADJUSTMENT_OUT'
                AND difference_quantity < 0
            )
        ),

    CONSTRAINT FK_STOCK_ADJUSTMENT_ITEM
        FOREIGN KEY (stocktake_item_id)
        REFERENCES STOCKTAKE_ITEM(stocktake_item_id),

    CONSTRAINT FK_STOCK_ADJUSTMENT_INV_LOT
        FOREIGN KEY (inventory_lot_id)
        REFERENCES INVENTORY_LOT(inventory_lot_id),

    CONSTRAINT FK_STOCK_ADJUSTMENT_PROCESSED_BY
        FOREIGN KEY (processed_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   6. STOCK_MOVEMENT
   재고 변동 이력

   STOCK_ADJUSTMENT를 참조하므로 재고 테이블 중 마지막에 생성
   ========================================================= */

CREATE TABLE STOCK_MOVEMENT (
    stock_movement_id              NUMBER(19)       NOT NULL,
    inventory_lot_id               NUMBER(19)       NOT NULL,
    warehouse_id                   NUMBER(19)       NOT NULL,
    item_id                        NUMBER(19)       NOT NULL,
    type                           VARCHAR2(30)     NOT NULL,
    change_quantity                NUMBER(19,3)     NOT NULL,
    before_quantity                NUMBER(19,3)     NOT NULL,
    after_quantity                 NUMBER(19,3)     NOT NULL,

    receipt_lot_id                 NUMBER(19),
    shipment_lot_id                NUMBER(19),
    purchase_return_item_id        NUMBER(19),
    customer_return_item_id        NUMBER(19),
    stock_adjustment_id            NUMBER(19),

    reason                         VARCHAR2(1000),
    processed_by                   NUMBER(19)       NOT NULL,
    processed_at                   TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT PK_STOCK_MOVEMENT
        PRIMARY KEY (stock_movement_id),

    CONSTRAINT CK_STOCK_MOVEMENT_TYPE
        CHECK (
            type IN (
                'RECEIPT',
                'SHIPMENT',
                'PURCHASE_RETURN',
                'RETURN_IN',
                'ADJUSTMENT_IN',
                'ADJUSTMENT_OUT',
                'DISPOSAL'
            )
        ),

    CONSTRAINT CK_STOCK_MOVEMENT_BEFORE_QTY
        CHECK (
            before_quantity >= 0
        ),

    CONSTRAINT CK_STOCK_MOVEMENT_AFTER_QTY
        CHECK (
            after_quantity >= 0
        ),

    CONSTRAINT CK_STOCK_MOVEMENT_QUANTITY
        CHECK (
            after_quantity =
                before_quantity + change_quantity
        ),

    CONSTRAINT CK_STOCK_MOVEMENT_DIRECTION
        CHECK (
            (
                type IN (
                    'RECEIPT',
                    'RETURN_IN',
                    'ADJUSTMENT_IN'
                )
                AND change_quantity > 0
            )
            OR
            (
                type IN (
                    'SHIPMENT',
                    'PURCHASE_RETURN',
                    'ADJUSTMENT_OUT',
                    'DISPOSAL'
                )
                AND change_quantity < 0
            )
        ),

    CONSTRAINT CK_STOCK_MOVEMENT_ORIGIN
        CHECK (
            (
                type = 'RECEIPT'
                AND receipt_lot_id IS NOT NULL
                AND shipment_lot_id IS NULL
                AND purchase_return_item_id IS NULL
                AND customer_return_item_id IS NULL
                AND stock_adjustment_id IS NULL
            )
            OR
            (
                type = 'SHIPMENT'
                AND receipt_lot_id IS NULL
                AND shipment_lot_id IS NOT NULL
                AND purchase_return_item_id IS NULL
                AND customer_return_item_id IS NULL
                AND stock_adjustment_id IS NULL
            )
            OR
            (
                type = 'PURCHASE_RETURN'
                AND receipt_lot_id IS NULL
                AND shipment_lot_id IS NULL
                AND purchase_return_item_id IS NOT NULL
                AND customer_return_item_id IS NULL
                AND stock_adjustment_id IS NULL
            )
            OR
            (
                type = 'RETURN_IN'
                AND receipt_lot_id IS NULL
                AND shipment_lot_id IS NULL
                AND purchase_return_item_id IS NULL
                AND customer_return_item_id IS NOT NULL
                AND stock_adjustment_id IS NULL
            )
            OR
            (
                type IN (
                    'ADJUSTMENT_IN',
                    'ADJUSTMENT_OUT'
                )
                AND receipt_lot_id IS NULL
                AND shipment_lot_id IS NULL
                AND purchase_return_item_id IS NULL
                AND customer_return_item_id IS NULL
                AND stock_adjustment_id IS NOT NULL
            )
            OR
            (
                type = 'DISPOSAL'
                AND receipt_lot_id IS NULL
                AND shipment_lot_id IS NULL
                AND purchase_return_item_id IS NULL
                AND customer_return_item_id IS NULL
                AND stock_adjustment_id IS NULL
                AND reason IS NOT NULL
            )
        ),

    CONSTRAINT FK_STOCK_MOVEMENT_INV_LOT
        FOREIGN KEY (inventory_lot_id)
        REFERENCES INVENTORY_LOT(inventory_lot_id),

    CONSTRAINT FK_STOCK_MOVEMENT_WAREHOUSE
        FOREIGN KEY (warehouse_id)
        REFERENCES WAREHOUSE(warehouse_id),

    CONSTRAINT FK_STOCK_MOVEMENT_ITEM
        FOREIGN KEY (item_id)
        REFERENCES ITEM(item_id),

    CONSTRAINT FK_STOCK_MOVEMENT_RECEIPT_LOT
        FOREIGN KEY (receipt_lot_id)
        REFERENCES RECEIPT_LOT(receipt_lot_id),

    CONSTRAINT FK_STOCK_MOVEMENT_SHIPMENT_LOT
        FOREIGN KEY (shipment_lot_id)
        REFERENCES SHIPMENT_LOT(shipment_lot_id),

    CONSTRAINT FK_STOCK_MOVEMENT_PUR_RETURN
        FOREIGN KEY (purchase_return_item_id)
        REFERENCES PURCHASE_RETURN_ITEM(purchase_return_item_id),

    CONSTRAINT FK_STOCK_MOVEMENT_CUST_RETURN
        FOREIGN KEY (customer_return_item_id)
        REFERENCES CUSTOMER_RETURN_ITEM(customer_return_item_id),

    CONSTRAINT FK_STOCK_MOVEMENT_ADJUSTMENT
        FOREIGN KEY (stock_adjustment_id)
        REFERENCES STOCK_ADJUSTMENT(stock_adjustment_id),

    CONSTRAINT FK_STOCK_MOVEMENT_PROCESSED_BY
        FOREIGN KEY (processed_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   7. 앞 단계에서 보류한 INVENTORY_LOT FK 연결
   ========================================================= */

ALTER TABLE RECEIPT_LOT
    ADD CONSTRAINT FK_RECEIPT_LOT_INVENTORY_LOT
    FOREIGN KEY (inventory_lot_id)
    REFERENCES INVENTORY_LOT(inventory_lot_id);

ALTER TABLE PURCHASE_RETURN_ITEM
    ADD CONSTRAINT FK_PUR_RETURN_ITEM_INV_LOT
    FOREIGN KEY (inventory_lot_id)
    REFERENCES INVENTORY_LOT(inventory_lot_id);

ALTER TABLE SHIPMENT_LOT
    ADD CONSTRAINT FK_SHIPMENT_LOT_INV_LOT
    FOREIGN KEY (inventory_lot_id)
    REFERENCES INVENTORY_LOT(inventory_lot_id);

ALTER TABLE CUSTOMER_RETURN_ITEM
    ADD CONSTRAINT FK_CUST_RETURN_ITEM_INV_LOT
    FOREIGN KEY (inventory_lot_id)
    REFERENCES INVENTORY_LOT(inventory_lot_id);