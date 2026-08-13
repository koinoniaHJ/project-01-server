/* =========================================================
   07_settlement.sql
   정산 업무 테이블 생성
   ========================================================= */


/* =========================================================
   1. VOUCHER
   전표
   ========================================================= */

CREATE TABLE VOUCHER (
    voucher_id                    NUMBER(19)      NOT NULL,
    type                          VARCHAR2(30)    NOT NULL,
    voucher_date                  DATE            NOT NULL,

    customer_id                   NUMBER(19),
    supplier_id                   NUMBER(19),

    original_voucher_id           NUMBER(19),

    shipment_id                   NUMBER(19),
    customer_return_id            NUMBER(19),
    receipt_id                    NUMBER(19),
    purchase_return_id            NUMBER(19),

    total_amount                  NUMBER(19,2)    NOT NULL,
    settlement_target_amount      NUMBER(19,2),
    allocated_amount              NUMBER(19,2)    DEFAULT 0 NOT NULL,
    outstanding_amount            NUMBER(19,2),
    settlement_status             VARCHAR2(30),

    created_at                    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version                       NUMBER(19)      DEFAULT 0 NOT NULL,

    CONSTRAINT PK_VOUCHER
        PRIMARY KEY (voucher_id),

    CONSTRAINT UK_VOUCHER_SHIPMENT
        UNIQUE (shipment_id),

    CONSTRAINT UK_VOUCHER_CUSTOMER_RETURN
        UNIQUE (customer_return_id),

    CONSTRAINT UK_VOUCHER_RECEIPT
        UNIQUE (receipt_id),

    CONSTRAINT UK_VOUCHER_PURCHASE_RETURN
        UNIQUE (purchase_return_id),

    CONSTRAINT CK_VOUCHER_TYPE
        CHECK (
            type IN (
                'SALES',
                'SALES_RETURN',
                'PURCHASE',
                'PURCHASE_RETURN'
            )
        ),

    CONSTRAINT CK_VOUCHER_SETTLEMENT_STATUS
        CHECK (
            settlement_status IN (
                'UNPAID',
                'PARTIALLY_PAID',
                'PAID'
            )
        ),

    CONSTRAINT CK_VOUCHER_ALLOCATED_AMOUNT
        CHECK (
            allocated_amount >= 0
        ),

    CONSTRAINT CK_VOUCHER_OUTSTANDING_AMOUNT
        CHECK (
            outstanding_amount >= 0
        ),

    CONSTRAINT CK_VOUCHER_PARTY
        CHECK (
            (
                type IN ('SALES', 'SALES_RETURN')
                AND customer_id IS NOT NULL
                AND supplier_id IS NULL
            )
            OR
            (
                type IN ('PURCHASE', 'PURCHASE_RETURN')
                AND customer_id IS NULL
                AND supplier_id IS NOT NULL
            )
        ),

    CONSTRAINT CK_VOUCHER_ORIGIN
        CHECK (
            (
                type = 'SALES'
                AND shipment_id IS NOT NULL
                AND customer_return_id IS NULL
                AND receipt_id IS NULL
                AND purchase_return_id IS NULL
            )
            OR
            (
                type = 'SALES_RETURN'
                AND shipment_id IS NULL
                AND customer_return_id IS NOT NULL
                AND receipt_id IS NULL
                AND purchase_return_id IS NULL
            )
            OR
            (
                type = 'PURCHASE'
                AND shipment_id IS NULL
                AND customer_return_id IS NULL
                AND receipt_id IS NOT NULL
                AND purchase_return_id IS NULL
            )
            OR
            (
                type = 'PURCHASE_RETURN'
                AND shipment_id IS NULL
                AND customer_return_id IS NULL
                AND receipt_id IS NULL
                AND purchase_return_id IS NOT NULL
            )
        ),

    CONSTRAINT CK_VOUCHER_ORIGINAL
        CHECK (
            (
                type IN ('SALES', 'PURCHASE')
                AND original_voucher_id IS NULL
            )
            OR
            (
                type IN ('SALES_RETURN', 'PURCHASE_RETURN')
                AND original_voucher_id IS NOT NULL
            )
        ),

    CONSTRAINT FK_VOUCHER_CUSTOMER
        FOREIGN KEY (customer_id)
        REFERENCES CUSTOMER(customer_id),

    CONSTRAINT FK_VOUCHER_SUPPLIER
        FOREIGN KEY (supplier_id)
        REFERENCES SUPPLIER(supplier_id),

    CONSTRAINT FK_VOUCHER_ORIGINAL
        FOREIGN KEY (original_voucher_id)
        REFERENCES VOUCHER(voucher_id),

    CONSTRAINT FK_VOUCHER_SHIPMENT
        FOREIGN KEY (shipment_id)
        REFERENCES SHIPMENT(shipment_id),

    CONSTRAINT FK_VOUCHER_CUSTOMER_RETURN
        FOREIGN KEY (customer_return_id)
        REFERENCES CUSTOMER_RETURN(customer_return_id),

    CONSTRAINT FK_VOUCHER_RECEIPT
        FOREIGN KEY (receipt_id)
        REFERENCES RECEIPT(receipt_id),

    CONSTRAINT FK_VOUCHER_PURCHASE_RETURN
        FOREIGN KEY (purchase_return_id)
        REFERENCES PURCHASE_RETURN(purchase_return_id)
);


/* =========================================================
   2. VOUCHER_ITEM
   전표 품목
   ========================================================= */

CREATE TABLE VOUCHER_ITEM (
    voucher_item_id        NUMBER(19)       NOT NULL,
    voucher_id             NUMBER(19)       NOT NULL,
    line_no                NUMBER(5)        NOT NULL,
    item_id                NUMBER(19)       NOT NULL,
    item_name_snapshot     VARCHAR2(150)    NOT NULL,
    unit_snapshot          VARCHAR2(50)     NOT NULL,
    quantity               NUMBER(19,3)     NOT NULL,
    unit_price             NUMBER(19,2)     NOT NULL,
    line_amount            NUMBER(19,2)     NOT NULL,
    created_at             TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT PK_VOUCHER_ITEM
        PRIMARY KEY (voucher_item_id),

    CONSTRAINT UK_VOUCHER_ITEM_LINE
        UNIQUE (voucher_id, line_no),

    CONSTRAINT CK_VOUCHER_ITEM_QUANTITY
        CHECK (
            quantity > 0
        ),

    CONSTRAINT CK_VOUCHER_ITEM_UNIT_PRICE
        CHECK (
            unit_price >= 0
        ),

    CONSTRAINT FK_VOUCHER_ITEM_VOUCHER
        FOREIGN KEY (voucher_id)
        REFERENCES VOUCHER(voucher_id),

    CONSTRAINT FK_VOUCHER_ITEM_ITEM
        FOREIGN KEY (item_id)
        REFERENCES ITEM(item_id)
);


/* =========================================================
   3. PAYMENT
   입금
   ========================================================= */

CREATE TABLE PAYMENT (
    payment_id              NUMBER(19)       NOT NULL,
    customer_id             NUMBER(19)       NOT NULL,
    payment_date            DATE             NOT NULL,
    amount                  NUMBER(19,2)     NOT NULL,
    method                  VARCHAR2(30)     NOT NULL,
    memo                    VARCHAR2(2000),
    status                  VARCHAR2(20)     DEFAULT 'ACTIVE' NOT NULL,
    unallocated_amount      NUMBER(19,2)     DEFAULT 0 NOT NULL,

    created_by              NUMBER(19)       NOT NULL,
    created_at              TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,

    canceled_by             NUMBER(19),
    canceled_at             TIMESTAMP,
    cancel_reason           VARCHAR2(1000),

    version                 NUMBER(19)       DEFAULT 0 NOT NULL,

    CONSTRAINT PK_PAYMENT
        PRIMARY KEY (payment_id),

    CONSTRAINT CK_PAYMENT_METHOD
        CHECK (
            method IN (
                'BANK_TRANSFER',
                'CASH',
                'CARD',
                'OTHER'
            )
        ),

    CONSTRAINT CK_PAYMENT_STATUS
        CHECK (
            status IN ('ACTIVE', 'CANCELED')
        ),

    CONSTRAINT CK_PAYMENT_AMOUNT
        CHECK (
            amount > 0
        ),

    CONSTRAINT CK_PAYMENT_UNALLOCATED
        CHECK (
            unallocated_amount >= 0
            AND unallocated_amount <= amount
        ),

    CONSTRAINT CK_PAYMENT_CANCEL
        CHECK (
            (
                status = 'ACTIVE'
                AND canceled_by IS NULL
                AND canceled_at IS NULL
                AND cancel_reason IS NULL
            )
            OR
            (
                status = 'CANCELED'
                AND canceled_by IS NOT NULL
                AND canceled_at IS NOT NULL
                AND cancel_reason IS NOT NULL
            )
        ),

    CONSTRAINT FK_PAYMENT_CUSTOMER
        FOREIGN KEY (customer_id)
        REFERENCES CUSTOMER(customer_id),

    CONSTRAINT FK_PAYMENT_CREATED_BY
        FOREIGN KEY (created_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_PAYMENT_CANCELED_BY
        FOREIGN KEY (canceled_by)
        REFERENCES APP_USER(user_id)
);


/* =========================================================
   4. PAYMENT_ALLOCATION
   입금 배분
   ========================================================= */

CREATE TABLE PAYMENT_ALLOCATION (
    payment_allocation_id     NUMBER(19)       NOT NULL,
    payment_id                NUMBER(19)       NOT NULL,
    voucher_id                NUMBER(19)       NOT NULL,
    allocated_amount          NUMBER(19,2)     NOT NULL,

    allocated_by              NUMBER(19),
    allocated_at              TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,

    released_by               NUMBER(19),
    released_at               TIMESTAMP,
    release_reason            VARCHAR2(1000),

    CONSTRAINT PK_PAYMENT_ALLOCATION
        PRIMARY KEY (payment_allocation_id),

    CONSTRAINT CK_PAYMENT_ALLOC_AMOUNT
        CHECK (
            allocated_amount > 0
        ),

    CONSTRAINT CK_PAYMENT_ALLOC_RELEASE
        CHECK (
            (
                released_at IS NULL
                AND released_by IS NULL
                AND release_reason IS NULL
            )
            OR
            (
                released_at IS NOT NULL
                AND released_by IS NOT NULL
                AND release_reason IS NOT NULL
            )
        ),

    CONSTRAINT FK_PAYMENT_ALLOC_PAYMENT
        FOREIGN KEY (payment_id)
        REFERENCES PAYMENT(payment_id),

    CONSTRAINT FK_PAYMENT_ALLOC_VOUCHER
        FOREIGN KEY (voucher_id)
        REFERENCES VOUCHER(voucher_id),

    CONSTRAINT FK_PAYMENT_ALLOC_ALLOCATED_BY
        FOREIGN KEY (allocated_by)
        REFERENCES APP_USER(user_id),

    CONSTRAINT FK_PAYMENT_ALLOC_RELEASED_BY
        FOREIGN KEY (released_by)
        REFERENCES APP_USER(user_id)
);