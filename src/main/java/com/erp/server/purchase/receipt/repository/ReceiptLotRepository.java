package com.erp.server.purchase.receipt.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.server.purchase.receipt.domain.ReceiptLot;

// ********** RECEIPT_LOT의 기본 CRUD와 검수 저장·완료 후 매입 반품에서 사용할 입고 LOT 데이터를 관리하기 위한 Repository interface **********
public interface ReceiptLotRepository extends JpaRepository<ReceiptLot, Long> {
}
