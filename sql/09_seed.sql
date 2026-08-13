/* =========================================================
   09_seed.sql
   개발용 초기 데이터 등록
   ========================================================= */


/* =========================================================
   1. 최초 ADMIN 사용자
   최초 관리자이므로 created_by, updated_by는 NULL
   ========================================================= */

INSERT INTO APP_USER (
    user_id,
    username,
    password_hash,
    user_name,
    role,
    status,
    created_by,
    updated_by
) VALUES (
    SEQ_APP_USER.NEXTVAL,
    'admin',
    '$2y$10$d2jteULeX0DabILcAanWteTTX.6r5RUs7oNwU5Sr7WKHSqidmlIbK',
    '관리자',
    'ADMIN',
    'ACTIVE',
    NULL,
    NULL
);


/* =========================================================
   2. 개발용 OFFICE 사용자
   ========================================================= */

INSERT INTO APP_USER (
    user_id,
    username,
    password_hash,
    user_name,
    role,
    status,
    created_by,
    updated_by
) VALUES (
    SEQ_APP_USER.NEXTVAL,
    'office',
    '$2y$10$eLGLtidDpo5z9gL/lYroXuqilI8t9oP.lbOjs1ZWvFM3rOzxXMde.',
    '사무 직원',
    'OFFICE',
    'ACTIVE',
    (SELECT user_id FROM APP_USER WHERE username = 'admin'),
    (SELECT user_id FROM APP_USER WHERE username = 'admin')
);


/* =========================================================
   3. 개발용 WAREHOUSE 사용자
   ========================================================= */

INSERT INTO APP_USER (
    user_id,
    username,
    password_hash,
    user_name,
    role,
    status,
    created_by,
    updated_by
) VALUES (
    SEQ_APP_USER.NEXTVAL,
    'warehouse',
    '$2y$10$d1KDclpT7dfJTd8kripQcOIwe3tWmbVyCcL3BA.qpnDbEOIzo2niK',
    '물류 직원',
    'WAREHOUSE',
    'ACTIVE',
    (SELECT user_id FROM APP_USER WHERE username = 'admin'),
    (SELECT user_id FROM APP_USER WHERE username = 'admin')
);


/* =========================================================
   4. 테스트 거래처
   CUS + 6자리 Sequence
   ========================================================= */

INSERT INTO CUSTOMER (
    customer_id,
    customer_code,
    customer_name,
    status,
    trade_status,
    total_receivable_amount,
    created_by,
    updated_by
) VALUES (
    SEQ_CUSTOMER.NEXTVAL,
    'CUS' || LPAD(SEQ_CUSTOMER_CODE.NEXTVAL, 6, '0'),
    '테스트 거래처',
    'ACTIVE',
    'NORMAL',
    0,
    (SELECT user_id FROM APP_USER WHERE username = 'admin'),
    (SELECT user_id FROM APP_USER WHERE username = 'admin')
);


/* =========================================================
   5. 테스트 공급업체
   SUP + 6자리 Sequence
   ========================================================= */

INSERT INTO SUPPLIER (
    supplier_id,
    supplier_code,
    supplier_name,
    email,
    status,
    created_by,
    updated_by
) VALUES (
    SEQ_SUPPLIER.NEXTVAL,
    'SUP' || LPAD(SEQ_SUPPLIER_CODE.NEXTVAL, 6, '0'),
    '테스트 공급업체',
    'supplier@example.com',
    'ACTIVE',
    (SELECT user_id FROM APP_USER WHERE username = 'admin'),
    (SELECT user_id FROM APP_USER WHERE username = 'admin')
);


/* =========================================================
   6. 테스트 품목
   ITM + 6자리 Sequence
   ========================================================= */

INSERT INTO ITEM (
    item_id,
    item_code,
    item_name,
    unit,
    default_sales_price,
    status,
    created_by,
    updated_by
) VALUES (
    SEQ_ITEM.NEXTVAL,
    'ITM' || LPAD(SEQ_ITEM_CODE.NEXTVAL, 6, '0'),
    '테스트 품목',
    'KG',
    10000,
    'ACTIVE',
    (SELECT user_id FROM APP_USER WHERE username = 'admin'),
    (SELECT user_id FROM APP_USER WHERE username = 'admin')
);


/* =========================================================
   7. 테스트 창고
   WH + 6자리 Sequence
   ========================================================= */

INSERT INTO WAREHOUSE (
    warehouse_id,
    warehouse_code,
    warehouse_name,
    status,
    created_by,
    updated_by
) VALUES (
    SEQ_WAREHOUSE.NEXTVAL,
    'WH' || LPAD(SEQ_WAREHOUSE_CODE.NEXTVAL, 6, '0'),
    '기본 창고',
    'ACTIVE',
    (SELECT user_id FROM APP_USER WHERE username = 'admin'),
    (SELECT user_id FROM APP_USER WHERE username = 'admin')
);


/* =========================================================
   8. 공급업체 - 품목 관계
   ========================================================= */

INSERT INTO SUPPLIER_ITEM (
    supplier_item_id,
    supplier_id,
    item_id,
    created_by
) VALUES (
    SEQ_SUPPLIER_ITEM.NEXTVAL,
    SEQ_SUPPLIER.CURRVAL,
    SEQ_ITEM.CURRVAL,
    (SELECT user_id FROM APP_USER WHERE username = 'admin')
);


/* =========================================================
   9. 창고 - 품목 관계
   안전재고 5 KG
   ========================================================= */

INSERT INTO WAREHOUSE_ITEM (
    warehouse_item_id,
    warehouse_id,
    item_id,
    safety_stock_quantity,
    created_by,
    updated_by
) VALUES (
    SEQ_WAREHOUSE_ITEM.NEXTVAL,
    SEQ_WAREHOUSE.CURRVAL,
    SEQ_ITEM.CURRVAL,
    5,
    (SELECT user_id FROM APP_USER WHERE username = 'admin'),
    (SELECT user_id FROM APP_USER WHERE username = 'admin')
);


COMMIT;