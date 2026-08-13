/* =========================================================
   04_purchase.sql
   구매 업무 테이블 생성
   ========================================================= */


/* =========================================================
   1. PURCHASE_ORDER
   발주
   ========================================================= */

CREATE TABLE PURCHASE_ORDER (
    purchase_order_id               NUMBER(19)       NOT NULL,
    supplier_id                     NUMBER(19)       NOT NULL,
    status                          VARCHAR2(30)     DEFAULT 'DRAFT' NOT NULL,
    email_status                    VARCHAR2(20),
    total_amount                    NUMBER(19,2)     DEFAULT 0 NOT NULL,
    memo                            VARCHAR2(2000),

    submitted_by                    NUMBER(19),
    submitted_at                    TIMESTAMP,

    approved_by                     NUMBER(19),
    approved_at                     TIMESTAMP,

    ordered_by                      NUMBER(19),
    ordered_at                      TIMESTAMP,

    canceled_by                     NUMBER(19),
    canceled_at                     TIMESTAMP,
    cancel_reason                   VARCHAR2(1000),

    closed_by                       NUMBER(19),
    closed_at                       TIMESTAMP,
    close_reason                    VARCHAR2(1000),

    supplier_cancel_confirmed_by    NUMBER(19),
    supplier_cancel_confirmed_at    TIMESTAMP,

    created_by                      NUMBER(19)       NOT NULL,
    created_at                      TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at                      TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version                         NUMBER(19)       DEFAULT 0 NOT NULL,

    CONSTRAINT PK_PURCHASE_ORDER
        PRIMARY KEY (purchase_order_id),

    CONSTRAINT CK_PO_STATUS
        CHECK (
            status IN (
                'DRAFT',
                'SUBMITTED',
                'APPROVED',
                'ORDERED',
                'CANCELED',
                'RECEIVED',
                'CLOSED'
            )
        ),

    CONSTRAINT CK_PO_EMAIL_STATUS
        CHECK (email_status IN ('SENT', 'FAILED')),

    CONSTRAINT CK_PO_TOTAL_AMOUNT
        CHECK (total_amount >= 0),

    CONSTRAINT FK_PO_SUPPLIER
        FOREIGN KEY (supplier_id)
        REFERENCES SUPPLIER(supplier_id),

    CONSTRAINT FK_PO_SUBMITTED_BY
        FOREIGN KEY (submitted_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_PO_APPROVED_BY
        FOREIGN KEY (approved_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_PO_ORDERED_BY
        FOREIGN KEY (ordered_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_PO_CANCELED_BY
        FOREIGN KEY (canceled_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_PO_CLOSED_BY
        FOREIGN KEY (closed_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_PO_CANCEL_CONF_BY
        FOREIGN KEY (supplier_cancel_confirmed_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_PO_CREATED_BY
        FOREIGN KEY (created_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   2. PURCHASE_ORDER_ITEM
   발주 품목
   ========================================================= */

CREATE TABLE PURCHASE_ORDER_ITEM (
    purchase_order_item_id    NUMBER(19)      NOT NULL,
    purchase_order_id         NUMBER(19)      NOT NULL,
    line_no                   NUMBER(5)       NOT NULL,
    item_id                   NUMBER(19)      NOT NULL,
    ordered_quantity          NUMBER(19,3)    NOT NULL,
    unit_price                NUMBER(19,2)    NOT NULL,
    line_amount               NUMBER(19,2)    NOT NULL,
    received_quantity         NUMBER(19,3)    DEFAULT 0 NOT NULL,
    created_at                TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at                TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT PK_PURCHASE_ORDER_ITEM
        PRIMARY KEY (purchase_order_item_id),

    CONSTRAINT UK_PO_ITEM_LINE
        UNIQUE (purchase_order_id, line_no),

    CONSTRAINT UK_PO_ITEM_ITEM
        UNIQUE (purchase_order_id, item_id),

    CONSTRAINT CK_PO_ITEM_ORDER_QTY
        CHECK (ordered_quantity > 0),

    CONSTRAINT CK_PO_ITEM_UNIT_PRICE
        CHECK (unit_price >= 0),

    CONSTRAINT CK_PO_ITEM_RECEIVED_QTY
        CHECK (
            received_quantity >= 0
            AND received_quantity <= ordered_quantity
        ),

    CONSTRAINT CK_PO_ITEM_LINE_AMOUNT
        CHECK (
            line_amount = ROUND(ordered_quantity * unit_price, 2)
        ),

    CONSTRAINT FK_PO_ITEM_ORDER
        FOREIGN KEY (purchase_order_id)
        REFERENCES PURCHASE_ORDER(purchase_order_id),

    CONSTRAINT FK_PO_ITEM_ITEM
        FOREIGN KEY (item_id)
        REFERENCES ITEM(item_id)
);


/* =========================================================
   3. PURCHASE_ORDER_EMAIL_HISTORY
   발주 이메일 전송 이력
   ========================================================= */

CREATE TABLE PURCHASE_ORDER_EMAIL_HISTORY (
    email_history_id       NUMBER(19)       NOT NULL,
    purchase_order_id      NUMBER(19)       NOT NULL,
    attempt_no             NUMBER(5)        NOT NULL,
    recipient_email        VARCHAR2(255)    NOT NULL,
    status                 VARCHAR2(20)     NOT NULL,
    error_message          VARCHAR2(2000),
    attempted_by           NUMBER(19),
    attempted_at           TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT PK_PO_EMAIL_HISTORY
        PRIMARY KEY (email_history_id),

    CONSTRAINT UK_PO_EMAIL_ATTEMPT
        UNIQUE (purchase_order_id, attempt_no),

    CONSTRAINT CK_PO_EMAIL_HIST_STATUS
        CHECK (status IN ('SENT', 'FAILED')),

    CONSTRAINT FK_PO_EMAIL_ORDER
        FOREIGN KEY (purchase_order_id)
        REFERENCES PURCHASE_ORDER(purchase_order_id),

    CONSTRAINT FK_PO_EMAIL_ATTEMPTED_BY
        FOREIGN KEY (attempted_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   4. RECEIPT
   입고
   ========================================================= */

CREATE TABLE RECEIPT (
    receipt_id                  NUMBER(19)       NOT NULL,
    purchase_order_id           NUMBER(19)       NOT NULL,
    warehouse_id                NUMBER(19)       NOT NULL,
    status                      VARCHAR2(30)     DEFAULT 'PENDING' NOT NULL,
    remainder_action            VARCHAR2(30),
    remainder_reason            VARCHAR2(1000),

    inspection_started_by       NUMBER(19),
    inspection_started_at       TIMESTAMP,

    completed_by                NUMBER(19),
    completed_at                TIMESTAMP,

    canceled_by                 NUMBER(19),
    canceled_at                 TIMESTAMP,
    cancel_reason               VARCHAR2(1000),

    created_by                  NUMBER(19)       NOT NULL,
    created_at                  TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version                     NUMBER(19)       DEFAULT 0 NOT NULL,

    CONSTRAINT PK_RECEIPT
        PRIMARY KEY (receipt_id),

    CONSTRAINT CK_RECEIPT_STATUS
        CHECK (
            status IN (
                'PENDING',
                'INSPECTING',
                'COMPLETED',
                'CANCELED'
            )
        ),

    CONSTRAINT CK_RECEIPT_REMAINDER_ACTION
        CHECK (
            remainder_action IN (
                'ADDITIONAL_RECEIPT',
                'CLOSE_REMAINDER'
            )
        ),

    CONSTRAINT FK_RECEIPT_ORDER
        FOREIGN KEY (purchase_order_id)
        REFERENCES PURCHASE_ORDER(purchase_order_id),

    CONSTRAINT FK_RECEIPT_WAREHOUSE
        FOREIGN KEY (warehouse_id)
        REFERENCES WAREHOUSE(warehouse_id),

    CONSTRAINT FK_RECEIPT_STARTED_BY
        FOREIGN KEY (inspection_started_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_RECEIPT_COMPLETED_BY
        FOREIGN KEY (completed_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_RECEIPT_CANCELED_BY
        FOREIGN KEY (canceled_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_RECEIPT_CREATED_BY
        FOREIGN KEY (created_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   5. RECEIPT_ITEM
   입고 품목
   ========================================================= */

CREATE TABLE RECEIPT_ITEM (
    receipt_item_id            NUMBER(19)      NOT NULL,
    receipt_id                 NUMBER(19)      NOT NULL,
    purchase_order_item_id     NUMBER(19)      NOT NULL,
    actual_quantity            NUMBER(19,3)    DEFAULT 0 NOT NULL,
    normal_quantity            NUMBER(19,3)    DEFAULT 0 NOT NULL,
    rejected_quantity          NUMBER(19,3)    DEFAULT 0 NOT NULL,
    note                       VARCHAR2(1000),
    created_at                 TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at                 TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT PK_RECEIPT_ITEM
        PRIMARY KEY (receipt_item_id),

    CONSTRAINT UK_RECEIPT_ORDER_ITEM
        UNIQUE (receipt_id, purchase_order_item_id),

    CONSTRAINT CK_RECEIPT_ITEM_ACTUAL
        CHECK (actual_quantity >= 0),

    CONSTRAINT CK_RECEIPT_ITEM_NORMAL
        CHECK (normal_quantity >= 0),

    CONSTRAINT CK_RECEIPT_ITEM_REJECTED
        CHECK (rejected_quantity >= 0),

    CONSTRAINT CK_RECEIPT_ITEM_QTY_SUM
        CHECK (
            actual_quantity = normal_quantity + rejected_quantity
        ),

    CONSTRAINT FK_RECEIPT_ITEM_RECEIPT
        FOREIGN KEY (receipt_id)
        REFERENCES RECEIPT(receipt_id),

    CONSTRAINT FK_RECEIPT_ITEM_PO_ITEM
        FOREIGN KEY (purchase_order_item_id)
        REFERENCES PURCHASE_ORDER_ITEM(purchase_order_item_id)
);


/* =========================================================
   6. RECEIPT_LOT
   입고 LOT
   ========================================================= */

CREATE TABLE RECEIPT_LOT (
    receipt_lot_id          NUMBER(19)       NOT NULL,
    receipt_item_id         NUMBER(19)       NOT NULL,
    supplier_lot_number     VARCHAR2(100),
    lot_number              VARCHAR2(100),
    internal_lot_yn         CHAR(1)          DEFAULT 'N' NOT NULL,
    expiry_date             DATE             NOT NULL,
    normal_quantity         NUMBER(19,3)     NOT NULL,
    inventory_lot_id        NUMBER(19),
    created_at              TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT PK_RECEIPT_LOT
        PRIMARY KEY (receipt_lot_id),

    CONSTRAINT UK_RECEIPT_LOT
        UNIQUE (
            receipt_item_id,
            supplier_lot_number,
            expiry_date
        ),

    CONSTRAINT CK_RECEIPT_LOT_INTERNAL
        CHECK (internal_lot_yn IN ('Y', 'N')),

    CONSTRAINT CK_RECEIPT_LOT_QUANTITY
        CHECK (normal_quantity > 0),

    CONSTRAINT CK_RECEIPT_LOT_SOURCE
        CHECK (
            (internal_lot_yn = 'Y' AND supplier_lot_number IS NULL)
            OR
            (internal_lot_yn = 'N' AND supplier_lot_number IS NOT NULL)
        ),

    CONSTRAINT FK_RECEIPT_LOT_ITEM
        FOREIGN KEY (receipt_item_id)
        REFERENCES RECEIPT_ITEM(receipt_item_id)

    /* inventory_lot_id FK는 INVENTORY_LOT 생성 후 추가 */
);


/* =========================================================
   7. PURCHASE_RETURN
   매입 반품
   ========================================================= */

CREATE TABLE PURCHASE_RETURN (
    purchase_return_id     NUMBER(19)       NOT NULL,
    receipt_id             NUMBER(19)       NOT NULL,
    status                 VARCHAR2(30)     DEFAULT 'REGISTERED' NOT NULL,
    reason                 VARCHAR2(1000)   NOT NULL,
    total_amount           NUMBER(19,2)     DEFAULT 0 NOT NULL,

    completed_by           NUMBER(19),
    completed_at           TIMESTAMP,

    canceled_by            NUMBER(19),
    canceled_at            TIMESTAMP,
    cancel_reason          VARCHAR2(1000),

    created_by             NUMBER(19)       NOT NULL,
    created_at             TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version                NUMBER(19)       DEFAULT 0 NOT NULL,

    CONSTRAINT PK_PURCHASE_RETURN
        PRIMARY KEY (purchase_return_id),

    CONSTRAINT CK_PURCHASE_RETURN_STATUS
        CHECK (
            status IN (
                'REGISTERED',
                'COMPLETED',
                'CANCELED'
            )
        ),

    CONSTRAINT CK_PURCHASE_RETURN_AMOUNT
        CHECK (total_amount >= 0),

    CONSTRAINT FK_PURCHASE_RETURN_RECEIPT
        FOREIGN KEY (receipt_id)
        REFERENCES RECEIPT(receipt_id),

    CONSTRAINT FK_PURCHASE_RETURN_COMPLETED
        FOREIGN KEY (completed_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_PURCHASE_RETURN_CANCELED
        FOREIGN KEY (canceled_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_PURCHASE_RETURN_CREATED
        FOREIGN KEY (created_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   8. PURCHASE_RETURN_ITEM
   매입 반품 품목
   ========================================================= */

CREATE TABLE PURCHASE_RETURN_ITEM (
    purchase_return_item_id    NUMBER(19)      NOT NULL,
    purchase_return_id         NUMBER(19)      NOT NULL,
    receipt_lot_id             NUMBER(19)      NOT NULL,
    inventory_lot_id           NUMBER(19)      NOT NULL,
    item_id                    NUMBER(19)      NOT NULL,
    return_quantity            NUMBER(19,3)    NOT NULL,
    unit_price                 NUMBER(19,2)    NOT NULL,
    line_amount                NUMBER(19,2)    NOT NULL,
    created_at                 TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at                 TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT PK_PURCHASE_RETURN_ITEM
        PRIMARY KEY (purchase_return_item_id),

    CONSTRAINT UK_PURCHASE_RETURN_LOT
        UNIQUE (purchase_return_id, receipt_lot_id),

    CONSTRAINT CK_PURCHASE_RETURN_QTY
        CHECK (return_quantity > 0),

    CONSTRAINT CK_PURCHASE_RETURN_PRICE
        CHECK (unit_price >= 0),

    CONSTRAINT CK_PURCHASE_RETURN_LINE_AMOUNT
        CHECK (
            line_amount = ROUND(return_quantity * unit_price, 2)
        ),

    CONSTRAINT FK_PURCHASE_RETURN_ITEM_RETURN
        FOREIGN KEY (purchase_return_id)
        REFERENCES PURCHASE_RETURN(purchase_return_id),

    CONSTRAINT FK_PURCHASE_RETURN_ITEM_RECEIPT_LOT
        FOREIGN KEY (receipt_lot_id)
        REFERENCES RECEIPT_LOT(receipt_lot_id),

    CONSTRAINT FK_PURCHASE_RETURN_ITEM_ITEM
        FOREIGN KEY (item_id)
        REFERENCES ITEM(item_id)

    /* inventory_lot_id FK는 INVENTORY_LOT 생성 후 추가 */
);