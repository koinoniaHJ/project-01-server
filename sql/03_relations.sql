/* =========================================================
   03_relations.sql
   관계정보 및 거래 상태 이력 테이블 생성
   ========================================================= */


/* =========================================================
   1. CUSTOMER_TRADE_STATUS_HISTORY
   거래처 거래 상태 이력
   ========================================================= */

CREATE TABLE CUSTOMER_TRADE_STATUS_HISTORY (
    trade_status_history_id    NUMBER(19)       NOT NULL,
    customer_id                NUMBER(19)       NOT NULL,
    previous_status            VARCHAR2(20)     NOT NULL,
    changed_status             VARCHAR2(20)     NOT NULL,
    reason                     VARCHAR2(1000)   NOT NULL,
    changed_by                 NUMBER(19)       NOT NULL,
    changed_at                 TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT PK_CUSTOMER_TRADE_STATUS_HISTORY
        PRIMARY KEY (trade_status_history_id),

    CONSTRAINT CK_CUST_TRADE_HIST_PREV_STATUS
        CHECK (previous_status IN ('NORMAL', 'HOLD')),

    CONSTRAINT CK_CUST_TRADE_HIST_CHANGED_STATUS
        CHECK (changed_status IN ('NORMAL', 'HOLD')),

    CONSTRAINT CK_CUST_TRADE_HIST_STATUS_CHANGE
        CHECK (previous_status <> changed_status),

    CONSTRAINT FK_CUST_TRADE_HIST_CUSTOMER
        FOREIGN KEY (customer_id)
        REFERENCES CUSTOMER(customer_id),

    CONSTRAINT FK_CUST_TRADE_HIST_CHANGED_BY
        FOREIGN KEY (changed_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   2. SUPPLIER_ITEM
   공급업체 취급 품목
   ========================================================= */

CREATE TABLE SUPPLIER_ITEM (
    supplier_item_id    NUMBER(19)    NOT NULL,
    supplier_id         NUMBER(19)    NOT NULL,
    item_id             NUMBER(19)    NOT NULL,
    created_by          NUMBER(19)    NOT NULL,
    created_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT PK_SUPPLIER_ITEM
        PRIMARY KEY (supplier_item_id),

    CONSTRAINT UK_SUPPLIER_ITEM
        UNIQUE (supplier_id, item_id),

    CONSTRAINT FK_SUPPLIER_ITEM_SUPPLIER
        FOREIGN KEY (supplier_id)
        REFERENCES SUPPLIER(supplier_id),

    CONSTRAINT FK_SUPPLIER_ITEM_ITEM
        FOREIGN KEY (item_id)
        REFERENCES ITEM(item_id),

    CONSTRAINT FK_SUPPLIER_ITEM_CREATED_BY
        FOREIGN KEY (created_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   3. WAREHOUSE_ITEM
   창고 품목 기준
   ========================================================= */

CREATE TABLE WAREHOUSE_ITEM (
    warehouse_item_id          NUMBER(19)      NOT NULL,
    warehouse_id               NUMBER(19)      NOT NULL,
    item_id                    NUMBER(19)      NOT NULL,
    safety_stock_quantity      NUMBER(19,3)    DEFAULT 0 NOT NULL,
    created_by                 NUMBER(19)      NOT NULL,
    created_at                 TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by                 NUMBER(19)      NOT NULL,
    updated_at                 TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version                    NUMBER(19)      DEFAULT 0 NOT NULL,

    CONSTRAINT PK_WAREHOUSE_ITEM
        PRIMARY KEY (warehouse_item_id),

    CONSTRAINT UK_WAREHOUSE_ITEM
        UNIQUE (warehouse_id, item_id),

    CONSTRAINT CK_WAREHOUSE_ITEM_SAFETY_STOCK
        CHECK (safety_stock_quantity >= 0),

    CONSTRAINT FK_WAREHOUSE_ITEM_WAREHOUSE
        FOREIGN KEY (warehouse_id)
        REFERENCES WAREHOUSE(warehouse_id),

    CONSTRAINT FK_WAREHOUSE_ITEM_ITEM
        FOREIGN KEY (item_id)
        REFERENCES ITEM(item_id),

    CONSTRAINT FK_WAREHOUSE_ITEM_CREATED_BY
        FOREIGN KEY (created_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_WAREHOUSE_ITEM_UPDATED_BY
        FOREIGN KEY (updated_by)
        REFERENCES APP_USER(user_id)
);