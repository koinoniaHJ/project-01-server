package com.erp.server.master.customer.domain;

// ********** CUSTOMER.trade_status에서 사용할 거래처 거래 상태를 Java와 DB에서 같은 값으로 관리하기 위한 enum **********
public enum CustomerTradeStatus {

    NORMAL,	// 정상 거래
    HOLD	// 거래 중지
}