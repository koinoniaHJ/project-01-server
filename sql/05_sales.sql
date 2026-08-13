/* =========================================================
   05_sales.sql
   판매 업무 테이블 생성
   ========================================================= */


/* =========================================================
   1. SALES_ORDER
   주문
   ========================================================= */

CREATE TABLE SALES_ORDER (
    sales_order_id                      NUMBER(19)       NOT NULL,
    customer_id                         NUMBER(19)       NOT NULL,
    channel                             VARCHAR2(20)     NOT NULL,
    status                              VARCHAR2(30)     DEFAULT 'DRAFT' NOT NULL,

    customer_code_snapshot              VARCHAR2(20),
    customer_name_snapshot              VARCHAR2(150),
    delivery_postal_code_snapshot       VARCHAR2(10),
    delivery_address_snapshot           VARCHAR2(500),
    delivery_address_detail_snapshot    VARCHAR2(300),
    recipient_name_snapshot             VARCHAR2(100),
    recipient_phone_snapshot            VARCHAR2(30),

    total_amount                        NUMBER(19,2)     DEFAULT 0 NOT NULL,
    memo                                VARCHAR2(2000),

    registered_by                       NUMBER(19),
    registered_at                       TIMESTAMP,

    canceled_by                         NUMBER(19),
    canceled_at                         TIMESTAMP,
    cancel_reason                       VARCHAR2(1000),

    created_by                          NUMBER(19)       NOT NULL,
    created_at                          TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at                          TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version                             NUMBER(19)       DEFAULT 0 NOT NULL,

    CONSTRAINT PK_SALES_ORDER
        PRIMARY KEY (sales_order_id),

    CONSTRAINT CK_SALES_ORDER_CHANNEL
        CHECK (
            channel IN ('VISIT', 'PHONE', 'MESSAGE')
        ),

    CONSTRAINT CK_SALES_ORDER_STATUS
        CHECK (
            status IN (
                'DRAFT',
                'REGISTERED',
                'COMPLETED',
                'CANCELED'
            )
        ),

    CONSTRAINT CK_SALES_ORDER_TOTAL_AMOUNT
        CHECK (total_amount >= 0),

    CONSTRAINT FK_SALES_ORDER_CUSTOMER
        FOREIGN KEY (customer_id)
        REFERENCES CUSTOMER(customer_id),

    CONSTRAINT FK_SALES_ORDER_REGISTERED_BY
        FOREIGN KEY (registered_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_SALES_ORDER_CANCELED_BY
        FOREIGN KEY (canceled_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_SALES_ORDER_CREATED_BY
        FOREIGN KEY (created_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   2. SALES_ORDER_ITEM
   주문 품목
   ========================================================= */

CREATE TABLE SALES_ORDER_ITEM (
    sales_order_item_id     NUMBER(19)       NOT NULL,
    sales_order_id          NUMBER(19)       NOT NULL,
    line_no                 NUMBER(5)        NOT NULL,
    item_id                 NUMBER(19)       NOT NULL,

    item_code_snapshot      VARCHAR2(20),
    item_name_snapshot      VARCHAR2(150),
    unit_snapshot           VARCHAR2(50),

    order_quantity          NUMBER(19,3)     NOT NULL,
    unit_price              NUMBER(19,2)     NOT NULL,
    line_amount             NUMBER(19,2)     NOT NULL,

    created_at              TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT PK_SALES_ORDER_ITEM
        PRIMARY KEY (sales_order_item_id),

    CONSTRAINT UK_SALES_ORDER_ITEM_LINE
        UNIQUE (sales_order_id, line_no),

    CONSTRAINT UK_SALES_ORDER_ITEM_ITEM
        UNIQUE (sales_order_id, item_id),

    CONSTRAINT CK_SALES_ORDER_ITEM_QTY
        CHECK (order_quantity > 0),

    CONSTRAINT CK_SALES_ORDER_ITEM_PRICE
        CHECK (unit_price >= 0),

    CONSTRAINT CK_SALES_ORDER_ITEM_AMOUNT
        CHECK (
            line_amount = ROUND(order_quantity * unit_price, 2)
        ),

    CONSTRAINT FK_SALES_ORDER_ITEM_ORDER
        FOREIGN KEY (sales_order_id)
        REFERENCES SALES_ORDER(sales_order_id),

    CONSTRAINT FK_SALES_ORDER_ITEM_ITEM
        FOREIGN KEY (item_id)
        REFERENCES ITEM(item_id)
);


/* =========================================================
   3. SHIPMENT
   출고
   ========================================================= */

CREATE TABLE SHIPMENT (
    shipment_id             NUMBER(19)      NOT NULL,
    sales_order_id          NUMBER(19)      NOT NULL,
    warehouse_id            NUMBER(19),
    status                  VARCHAR2(30)    DEFAULT 'PENDING' NOT NULL,
    packing_sequence        NUMBER(5)       DEFAULT 0 NOT NULL,

    packed_by               NUMBER(19),
    packed_at               TIMESTAMP,

    completed_by            NUMBER(19),
    completed_at            TIMESTAMP,

    canceled_by             NUMBER(19),
    canceled_at             TIMESTAMP,

    created_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version                 NUMBER(19)      DEFAULT 0 NOT NULL,

    CONSTRAINT PK_SHIPMENT
        PRIMARY KEY (shipment_id),

    CONSTRAINT UK_SHIPMENT_SALES_ORDER
        UNIQUE (sales_order_id),

    CONSTRAINT CK_SHIPMENT_STATUS
        CHECK (
            status IN (
                'PENDING',
                'PACKED',
                'COMPLETED',
                'CANCELED'
            )
        ),

    CONSTRAINT CK_SHIPMENT_PACKING_SEQUENCE
        CHECK (packing_sequence >= 0),

    CONSTRAINT FK_SHIPMENT_SALES_ORDER
        FOREIGN KEY (sales_order_id)
        REFERENCES SALES_ORDER(sales_order_id),

    CONSTRAINT FK_SHIPMENT_WAREHOUSE
        FOREIGN KEY (warehouse_id)
        REFERENCES WAREHOUSE(warehouse_id),

    CONSTRAINT FK_SHIPMENT_PACKED_BY
        FOREIGN KEY (packed_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_SHIPMENT_COMPLETED_BY
        FOREIGN KEY (completed_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_SHIPMENT_CANCELED_BY
        FOREIGN KEY (canceled_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   4. SHIPMENT_LOT
   출고 LOT 배정
   ========================================================= */

CREATE TABLE SHIPMENT_LOT (
    shipment_lot_id           NUMBER(19)      NOT NULL,
    shipment_id               NUMBER(19)      NOT NULL,
    sales_order_item_id       NUMBER(19)      NOT NULL,
    inventory_lot_id          NUMBER(19)      NOT NULL,
    packed_quantity           NUMBER(19,3)    NOT NULL,
    reserved_yn               CHAR(1)         DEFAULT 'N' NOT NULL,
    reserved_at               TIMESTAMP,
    created_at                TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at                TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT PK_SHIPMENT_LOT
        PRIMARY KEY (shipment_lot_id),

    CONSTRAINT UK_SHIPMENT_LOT
        UNIQUE (
            shipment_id,
            sales_order_item_id,
            inventory_lot_id
        ),

    CONSTRAINT CK_SHIPMENT_LOT_RESERVED
        CHECK (reserved_yn IN ('Y', 'N')),

    CONSTRAINT CK_SHIPMENT_LOT_QTY
        CHECK (packed_quantity > 0),

    CONSTRAINT FK_SHIPMENT_LOT_SHIPMENT
        FOREIGN KEY (shipment_id)
        REFERENCES SHIPMENT(shipment_id),

    CONSTRAINT FK_SHIPMENT_LOT_ORDER_ITEM
        FOREIGN KEY (sales_order_item_id)
        REFERENCES SALES_ORDER_ITEM(sales_order_item_id)

    /* inventory_lot_id FK는 INVENTORY_LOT 생성 후 추가 */
);


/* =========================================================
   5. DELIVERY_NOTE
   납품서
   ========================================================= */

CREATE TABLE DELIVERY_NOTE (
    delivery_note_id       NUMBER(19)       NOT NULL,
    shipment_id            NUMBER(19)       NOT NULL,
    issue_sequence         NUMBER(5)        NOT NULL,
    status                 VARCHAR2(20)     DEFAULT 'ACTIVE' NOT NULL,

    issued_by              NUMBER(19)       NOT NULL,
    issued_at              TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,

    voided_by              NUMBER(19),
    voided_at              TIMESTAMP,
    void_reason            VARCHAR2(1000),

    CONSTRAINT PK_DELIVERY_NOTE
        PRIMARY KEY (delivery_note_id),

    CONSTRAINT UK_DELIVERY_NOTE_SEQUENCE
        UNIQUE (shipment_id, issue_sequence),

    CONSTRAINT CK_DELIVERY_NOTE_STATUS
        CHECK (
            status IN ('ACTIVE', 'VOID')
        ),

    CONSTRAINT CK_DELIVERY_NOTE_SEQUENCE
        CHECK (issue_sequence > 0),

    CONSTRAINT FK_DELIVERY_NOTE_SHIPMENT
        FOREIGN KEY (shipment_id)
        REFERENCES SHIPMENT(shipment_id),

    CONSTRAINT FK_DELIVERY_NOTE_ISSUED_BY
        FOREIGN KEY (issued_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_DELIVERY_NOTE_VOIDED_BY
        FOREIGN KEY (voided_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   6. CUSTOMER_RETURN
   거래처 반품
   ========================================================= */

CREATE TABLE CUSTOMER_RETURN (
    customer_return_id       NUMBER(19)       NOT NULL,
    shipment_id              NUMBER(19)       NOT NULL,
    warehouse_id             NUMBER(19)       NOT NULL,
    status                   VARCHAR2(30)     DEFAULT 'REGISTERED' NOT NULL,
    total_return_amount      NUMBER(19,2)     DEFAULT 0 NOT NULL,
    memo                     VARCHAR2(2000),

    completed_by             NUMBER(19),
    completed_at             TIMESTAMP,

    not_allowed_by           NUMBER(19),
    not_allowed_at           TIMESTAMP,
    not_allowed_reason       VARCHAR2(1000),

    created_by               NUMBER(19)       NOT NULL,
    created_at               TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at               TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version                  NUMBER(19)       DEFAULT 0 NOT NULL,

    CONSTRAINT PK_CUSTOMER_RETURN
        PRIMARY KEY (customer_return_id),

    CONSTRAINT CK_CUSTOMER_RETURN_STATUS
        CHECK (
            status IN (
                'REGISTERED',
                'COMPLETED',
                'NOT_ALLOWED'
            )
        ),

    CONSTRAINT CK_CUSTOMER_RETURN_AMOUNT
        CHECK (total_return_amount >= 0),

    CONSTRAINT FK_CUSTOMER_RETURN_SHIPMENT
        FOREIGN KEY (shipment_id)
        REFERENCES SHIPMENT(shipment_id),

    CONSTRAINT FK_CUSTOMER_RETURN_WAREHOUSE
        FOREIGN KEY (warehouse_id)
        REFERENCES WAREHOUSE(warehouse_id),

    CONSTRAINT FK_CUSTOMER_RETURN_COMPLETED_BY
        FOREIGN KEY (completed_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_CUSTOMER_RETURN_NOT_ALLOWED
        FOREIGN KEY (not_allowed_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_CUSTOMER_RETURN_CREATED_BY
        FOREIGN KEY (created_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   7. CUSTOMER_RETURN_ITEM
   거래처 반품 품목
   ========================================================= */

CREATE TABLE CUSTOMER_RETURN_ITEM (
    customer_return_item_id       NUMBER(19)       NOT NULL,
    customer_return_id            NUMBER(19)       NOT NULL,
    shipment_lot_id               NUMBER(19)       NOT NULL,
    sales_order_item_id           NUMBER(19)       NOT NULL,
    inventory_lot_id              NUMBER(19)       NOT NULL,
    item_id                       NUMBER(19)       NOT NULL,

    return_type                   VARCHAR2(30)     NOT NULL,
    unopened_yn                   CHAR(1),

    actual_return_quantity        NUMBER(19,3)     DEFAULT 0 NOT NULL,
    resellable_quantity           NUMBER(19,3)     DEFAULT 0 NOT NULL,
    non_resellable_quantity       NUMBER(19,3)     DEFAULT 0 NOT NULL,

    reason                        VARCHAR2(1000)   NOT NULL,
    unit_price                    NUMBER(19,2)     NOT NULL,
    line_amount                   NUMBER(19,2)     NOT NULL,

    created_at                    TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at                    TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT PK_CUSTOMER_RETURN_ITEM
        PRIMARY KEY (customer_return_item_id),

    CONSTRAINT UK_CUSTOMER_RETURN_SHIPMENT_LOT
        UNIQUE (customer_return_id, shipment_lot_id),

    CONSTRAINT CK_CUSTOMER_RETURN_ITEM_TYPE
        CHECK (
            return_type IN (
                'DEFECT',
                'WRONG_DELIVERY',
                'CHANGE_OF_MIND'
            )
        ),

    CONSTRAINT CK_CUSTOMER_RETURN_UNOPENED
        CHECK (unopened_yn IN ('Y', 'N')),

    CONSTRAINT CK_CUSTOMER_RETURN_ACTUAL_QTY
        CHECK (actual_return_quantity >= 0),

    CONSTRAINT CK_CUSTOMER_RETURN_RESELL_QTY
        CHECK (resellable_quantity >= 0),

    CONSTRAINT CK_CUSTOMER_RETURN_NON_RESELL_QTY
        CHECK (non_resellable_quantity >= 0),

    CONSTRAINT CK_CUSTOMER_RETURN_QTY_SUM
        CHECK (
            resellable_quantity
            + non_resellable_quantity
            = actual_return_quantity
        ),

    CONSTRAINT CK_CUSTOMER_RETURN_UNIT_PRICE
        CHECK (unit_price >= 0),

    CONSTRAINT CK_CUSTOMER_RETURN_LINE_AMOUNT
        CHECK (
            line_amount = ROUND(actual_return_quantity * unit_price, 2)
        ),

    CONSTRAINT FK_CUSTOMER_RETURN_ITEM_RETURN
        FOREIGN KEY (customer_return_id)
        REFERENCES CUSTOMER_RETURN(customer_return_id),

    CONSTRAINT FK_CUSTOMER_RETURN_ITEM_SHIP_LOT
        FOREIGN KEY (shipment_lot_id)
        REFERENCES SHIPMENT_LOT(shipment_lot_id),

    CONSTRAINT FK_CUSTOMER_RETURN_ITEM_ORDER_ITEM
        FOREIGN KEY (sales_order_item_id)
        REFERENCES SALES_ORDER_ITEM(sales_order_item_id),

    CONSTRAINT FK_CUSTOMER_RETURN_ITEM_ITEM
        FOREIGN KEY (item_id)
        REFERENCES ITEM(item_id)

    /* inventory_lot_id FK는 INVENTORY_LOT 생성 후 추가 */
);