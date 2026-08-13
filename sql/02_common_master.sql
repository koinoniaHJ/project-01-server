/* =========================================================
   02_common_master.sql
   공통·기준정보 기본 테이블 생성
   ========================================================= */


/* =========================================================
   1. APP_USER
   사용자
   ========================================================= */

CREATE TABLE APP_USER (
    user_id         NUMBER(19)      NOT NULL,
    username        VARCHAR2(50)    NOT NULL,
    password_hash   VARCHAR2(255)   NOT NULL,
    user_name       VARCHAR2(100)   NOT NULL,
    role            VARCHAR2(20)    NOT NULL,
    status          VARCHAR2(20)    DEFAULT 'ACTIVE' NOT NULL,
    last_login_at   TIMESTAMP,
    created_by      NUMBER(19),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by      NUMBER(19),
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version         NUMBER(19)      DEFAULT 0 NOT NULL,

    CONSTRAINT PK_APP_USER
        PRIMARY KEY (user_id),

    CONSTRAINT UK_APP_USER_USERNAME
        UNIQUE (username),

    CONSTRAINT CK_APP_USER_ROLE
        CHECK (role IN ('ADMIN', 'OFFICE', 'WAREHOUSE')),

    CONSTRAINT CK_APP_USER_STATUS
        CHECK (status IN ('ACTIVE', 'INACTIVE')),

    CONSTRAINT FK_APP_USER_CREATED_BY
        FOREIGN KEY (created_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_APP_USER_UPDATED_BY
        FOREIGN KEY (updated_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   2. CUSTOMER
   거래처
   ========================================================= */

CREATE TABLE CUSTOMER (
    customer_id                 NUMBER(19)       NOT NULL,
    customer_code               VARCHAR2(20)     NOT NULL,
    customer_name               VARCHAR2(150)    NOT NULL,
    phone                       VARCHAR2(30),
    email                       VARCHAR2(255),
    postal_code                 VARCHAR2(10),
    address                     VARCHAR2(500),
    address_detail              VARCHAR2(300),
    delivery_postal_code        VARCHAR2(10),
    delivery_address            VARCHAR2(500),
    delivery_address_detail     VARCHAR2(300),
    recipient_name              VARCHAR2(100),
    recipient_phone             VARCHAR2(30),
    memo                        VARCHAR2(2000),
    status                      VARCHAR2(20)      DEFAULT 'ACTIVE' NOT NULL,
    trade_status                VARCHAR2(20)      DEFAULT 'NORMAL' NOT NULL,
    total_receivable_amount     NUMBER(19,2)      DEFAULT 0 NOT NULL,
    created_by                  NUMBER(19)        NOT NULL,
    created_at                  TIMESTAMP         DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by                  NUMBER(19)        NOT NULL,
    updated_at                  TIMESTAMP         DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version                     NUMBER(19)        DEFAULT 0 NOT NULL,

    CONSTRAINT PK_CUSTOMER
        PRIMARY KEY (customer_id),

    CONSTRAINT UK_CUSTOMER_CODE
        UNIQUE (customer_code),

    CONSTRAINT CK_CUSTOMER_STATUS
        CHECK (status IN ('ACTIVE', 'INACTIVE')),

    CONSTRAINT CK_CUSTOMER_TRADE_STATUS
        CHECK (trade_status IN ('NORMAL', 'HOLD')),

    CONSTRAINT CK_CUSTOMER_RECEIVABLE
        CHECK (total_receivable_amount >= 0),

    CONSTRAINT FK_CUSTOMER_CREATED_BY
        FOREIGN KEY (created_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_CUSTOMER_UPDATED_BY
        FOREIGN KEY (updated_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   3. SUPPLIER
   공급업체
   ========================================================= */

CREATE TABLE SUPPLIER (
    supplier_id       NUMBER(19)       NOT NULL,
    supplier_code     VARCHAR2(20)     NOT NULL,
    supplier_name     VARCHAR2(150)    NOT NULL,
    phone             VARCHAR2(30),
    email             VARCHAR2(255)    NOT NULL,
    postal_code       VARCHAR2(10),
    address           VARCHAR2(500),
    address_detail    VARCHAR2(300),
    memo              VARCHAR2(2000),
    status            VARCHAR2(20)     DEFAULT 'ACTIVE' NOT NULL,
    created_by        NUMBER(19)       NOT NULL,
    created_at        TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by        NUMBER(19)       NOT NULL,
    updated_at        TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version           NUMBER(19)       DEFAULT 0 NOT NULL,

    CONSTRAINT PK_SUPPLIER
        PRIMARY KEY (supplier_id),

    CONSTRAINT UK_SUPPLIER_CODE
        UNIQUE (supplier_code),

    CONSTRAINT CK_SUPPLIER_STATUS
        CHECK (status IN ('ACTIVE', 'INACTIVE')),

    CONSTRAINT FK_SUPPLIER_CREATED_BY
        FOREIGN KEY (created_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_SUPPLIER_UPDATED_BY
        FOREIGN KEY (updated_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   4. ITEM
   품목
   ========================================================= */

CREATE TABLE ITEM (
    item_id                 NUMBER(19)       NOT NULL,
    item_code               VARCHAR2(20)     NOT NULL,
    item_name               VARCHAR2(150)    NOT NULL,
    unit                    VARCHAR2(20)     NOT NULL,
    other_unit_name         VARCHAR2(50),
    default_sales_price     NUMBER(19,2)     DEFAULT 0 NOT NULL,
    status                  VARCHAR2(20)     DEFAULT 'ACTIVE' NOT NULL,
    memo                    VARCHAR2(2000),
    created_by              NUMBER(19)       NOT NULL,
    created_at              TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by              NUMBER(19)       NOT NULL,
    updated_at              TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version                 NUMBER(19)       DEFAULT 0 NOT NULL,

    CONSTRAINT PK_ITEM
        PRIMARY KEY (item_id),

    CONSTRAINT UK_ITEM_CODE
        UNIQUE (item_code),

    CONSTRAINT CK_ITEM_UNIT
        CHECK (unit IN ('G', 'KG', 'EA', 'PACK', 'BOX', 'OTHER')),
        
    CONSTRAINT CK_ITEM_OTHER_UNIT
	    CHECK (
	        unit <> 'OTHER'
	        OR other_unit_name IS NOT NULL
	    ),

    CONSTRAINT CK_ITEM_STATUS
        CHECK (status IN ('ACTIVE', 'INACTIVE')),

    CONSTRAINT CK_ITEM_SALES_PRICE
        CHECK (default_sales_price >= 0),

    CONSTRAINT FK_ITEM_CREATED_BY
        FOREIGN KEY (created_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_ITEM_UPDATED_BY
        FOREIGN KEY (updated_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   5. WAREHOUSE
   창고
   ========================================================= */

CREATE TABLE WAREHOUSE (
    warehouse_id       NUMBER(19)       NOT NULL,
    warehouse_code     VARCHAR2(20)     NOT NULL,
    warehouse_name     VARCHAR2(150)    NOT NULL,
    postal_code        VARCHAR2(10),
    address            VARCHAR2(500),
    address_detail     VARCHAR2(300),
    status             VARCHAR2(20)     DEFAULT 'ACTIVE' NOT NULL,
    memo               VARCHAR2(2000),
    created_by         NUMBER(19)       NOT NULL,
    created_at         TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by         NUMBER(19)       NOT NULL,
    updated_at         TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version            NUMBER(19)       DEFAULT 0 NOT NULL,

    CONSTRAINT PK_WAREHOUSE
        PRIMARY KEY (warehouse_id),

    CONSTRAINT UK_WAREHOUSE_CODE
        UNIQUE (warehouse_code),

    CONSTRAINT CK_WAREHOUSE_STATUS
        CHECK (status IN ('ACTIVE', 'INACTIVE')),

    CONSTRAINT FK_WH_CREATED_BY
        FOREIGN KEY (created_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_WH_UPDATED_BY
        FOREIGN KEY (updated_by)
        REFERENCES APP_USER(user_id)
);