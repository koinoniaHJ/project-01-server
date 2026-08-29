package com.erp.server.master.item.dto;

import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.item.domain.SupplierItem;
import com.erp.server.master.supplier.domain.Supplier;

// ********** 품목 상세 화면의 취급 공급업체 목록에 필요한 공급업체 식별자·코드·명칭·사용 상태를 반환하기 위한 응답 DTO record **********
// 관계 등록·해제는 itemId와 supplierId를 사용하므로 SUPPLIER_ITEM 내부 식별자와 등록 감사 정보는 응답에서 제외한다.
public record SupplierItemResponse(Long supplierId, String supplierCode, String supplierName, MasterStatus status) {

	// ========== SupplierItem Entity의 공급업체 정보를 SupplierItemResponse로 변환하는 정적 팩토리 메서드 ==========
	public static SupplierItemResponse from(SupplierItem supplierItem) {

		Supplier supplier = supplierItem.getSupplier();

		return new SupplierItemResponse(supplier.getSupplierId(), supplier.getSupplierCode(),
				supplier.getSupplierName(), supplier.getStatus());
	}
}
