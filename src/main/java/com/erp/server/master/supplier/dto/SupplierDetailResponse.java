package com.erp.server.master.supplier.dto;

import com.erp.server.common.user.domain.UserRole;
import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.supplier.domain.Supplier;

// ********** 공급업체 상세정보와 최신 version을 반환하고 WAREHOUSE 역할에는 공급업체 메모를 숨기기 위한 응답 DTO record **********
// 공급업체 상세 조회와 등록·수정·상태 변경 응답에 사용한다.
// WAREHOUSE 역할에는 설계에 따라 공급업체 메모를 숨긴다.
public record SupplierDetailResponse(
        Long supplierId,
        String supplierCode, // supplierCode는 조회만 가능하며 수정 요청에는 포함되지 않는다.
        String supplierName,
        String phone,
        String email,
        String postalCode,
        String address,
        String addressDetail,
        String memo,
        MasterStatus status,
        Long version // version은 수정과 상태 변경 요청에 사용한다.
) {

    // ========== Supplier Entity를 역할별 조회 범위가 적용된 SupplierDetailResponse로 변환하는 정적 팩토리 메서드 ==========
    public static SupplierDetailResponse from(Supplier supplier, UserRole userRole) {

        String memo = userRole == UserRole.WAREHOUSE ? null : supplier.getMemo();

        return new SupplierDetailResponse(
                supplier.getSupplierId(),
                supplier.getSupplierCode(),
                supplier.getSupplierName(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.getPostalCode(),
                supplier.getAddress(),
                supplier.getAddressDetail(),
                memo,
                supplier.getStatus(),
                supplier.getVersion()
        );
    }
}
