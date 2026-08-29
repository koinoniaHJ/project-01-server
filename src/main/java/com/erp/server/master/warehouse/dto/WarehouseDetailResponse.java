package com.erp.server.master.warehouse.dto;

import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.warehouse.domain.Warehouse;

// ********** 창고 상세정보와 수정·상태 변경에 사용할 최신 version을 반환하기 위한 응답 DTO record **********
// 창고 상세 조회와 이후 등록·수정·상태 변경 응답에 공통으로 사용한다.
public record WarehouseDetailResponse(
		Long warehouseId,
		String warehouseCode, // warehouseCode는 조회만 가능하며 수정 요청에는 포함되지 않는다.
		String warehouseName,
		String postalCode,
		String address,
		String addressDetail,
		MasterStatus status,
		String memo,
		Long version // version은 수정과 상태 변경 요청에 사용한다.
) {

	// ========== Warehouse Entity를 WarehouseDetailResponse로 변환하는 정적 팩토리 메서드 ==========
	public static WarehouseDetailResponse from(Warehouse warehouse) {

		return new WarehouseDetailResponse(
				warehouse.getWarehouseId(),
				warehouse.getWarehouseCode(),
				warehouse.getWarehouseName(),
				warehouse.getPostalCode(),
				warehouse.getAddress(),
				warehouse.getAddressDetail(),
				warehouse.getStatus(),
				warehouse.getMemo(),
				warehouse.getVersion()
		);
	}
}
