package com.erp.server.settlement.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.server.settlement.domain.VoucherItem;

// ********** VOUCHER_ITEM의 전표 품목 스냅샷 기본 CRUD를 처리하기 위한 Repository interface **********
public interface VoucherItemRepository extends JpaRepository<VoucherItem, Long> {
}
