package com.erp.server.master.customer.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.master.customer.domain.CustomerTradeStatusHistory;

// ********** CUSTOMER_TRADE_STATUS_HISTORY의 이력 저장과 거래처별 거래 상태 변경 이력 조회를 처리하기 위한 Repository interface **********
public interface CustomerTradeStatusHistoryRepository extends JpaRepository<CustomerTradeStatusHistory, Long> {

    // ========== 지정한 거래처의 거래 상태 변경 이력을 최근 변경 순서로 페이지 조회하는 메서드 ==========
    @Query("""
            select h
            from CustomerTradeStatusHistory h
            where h.customer.customerId = :customerId
            order by h.changedAt desc, h.tradeStatusHistoryId desc
            """)
    Page<CustomerTradeStatusHistory> findAllByCustomerId(
            @Param("customerId") Long customerId,
            Pageable pageable);
}