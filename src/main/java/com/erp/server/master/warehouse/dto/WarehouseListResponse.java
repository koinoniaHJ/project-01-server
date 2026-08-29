package com.erp.server.master.warehouse.dto;

import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.warehouse.domain.Warehouse;

// ********** 창고 목록 화면에 필요한 창고 코드·명칭·기본 주소·사용 상태를 반환하기 위한 응답 DTO record **********
// 목록에 필요하지 않은 우편번호·상세 주소·메모·감사 정보는 제외하고, version은 창고 선택 시 상세 API에서 최신 값으로 조회한다.
public record WarehouseListResponse(Long warehouseId, String warehouseCode, String warehouseName, String address,
		MasterStatus status) {

	// ========== Warehouse Entity를 WarehouseListResponse로 변환하는 정적 팩토리 메서드 ==========
	public static WarehouseListResponse from(Warehouse warehouse) {

		return new WarehouseListResponse(warehouse.getWarehouseId(), warehouse.getWarehouseCode(),
				warehouse.getWarehouseName(), warehouse.getAddress(), warehouse.getStatus());
	}
}
