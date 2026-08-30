package com.erp.server.settlement.service;

import java.math.BigDecimal;

import com.erp.server.master.item.domain.Item;

// ********** 입고·반품·출고 업무가 전표 품목 스냅샷 생성에 전달할 품목·수량·단가를 묶기 위한 내부 입력 record **********
public record VoucherItemInput(Item item, BigDecimal quantity, BigDecimal unitPrice) {
}
