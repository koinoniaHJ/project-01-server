package com.erp.server.master.supplier.dto;

import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.supplier.domain.Supplier;

// ********** 공급업체 목록 화면에 필요한 공급업체 코드·명칭·연락처·발주 이메일·사용 상태를 반환하기 위한 응답 DTO record **********
// 목록과 상세 응답을 분리하여 목록에 필요하지 않은 주소·메모·감사 정보를 전달하지 않는다.
// 목록에서 공급업체를 직접 수정하지 않으므로 version은 제외하고, 공급업체 선택 시 상세 API에서 최신 version을 조회한다.
public record SupplierListResponse(Long supplierId, String supplierCode, String supplierName, String phone,
		String email, MasterStatus status) {

	// ========== Supplier Entity를 SupplierListResponse로 변환하는 정적 팩토리 메서드 ==========
	public static SupplierListResponse from(Supplier supplier) {

		return new SupplierListResponse(supplier.getSupplierId(), supplier.getSupplierCode(),
				supplier.getSupplierName(), supplier.getPhone(), supplier.getEmail(), supplier.getStatus());
	}
}
