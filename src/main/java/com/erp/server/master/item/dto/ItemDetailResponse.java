package com.erp.server.master.item.dto;

import java.math.BigDecimal;
import java.util.List;

import com.erp.server.common.user.domain.UserRole;
import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.item.domain.Item;
import com.erp.server.master.item.domain.ItemUnit;
import com.erp.server.master.item.domain.SupplierItem;

// ********** 품목 상세정보·취급 공급업체 목록과 최신 version을 반환하고 WAREHOUSE 역할에는 기본 판매가격을 숨기기 위한 응답 DTO record **********
// 품목 상세 조회와 등록·수정·상태 변경 응답에 사용하며 취급 공급업체는 공급업체 코드 오름차순으로 전달받는다.
public record ItemDetailResponse(
		Long itemId,
		String itemCode, // itemCode는 조회만 가능하며 수정 요청에는 포함되지 않는다.
		String itemName,
		ItemUnit unit,
		String otherUnitName,
		BigDecimal defaultSalesPrice,
		MasterStatus status,
		String memo,
		List<SupplierItemResponse> suppliers,
		Long version // version은 수정과 상태 변경 요청에 사용한다.
) {

	// ========== Item Entity와 취급 공급업체 관계를 역할별 조회 범위가 적용된 ItemDetailResponse로 변환하는 정적 팩토리 메서드 ==========
	public static ItemDetailResponse from(Item item, List<SupplierItem> supplierItems, UserRole userRole) {

		BigDecimal defaultSalesPrice = userRole == UserRole.WAREHOUSE ? null : item.getDefaultSalesPrice();
		List<SupplierItemResponse> suppliers = supplierItems.stream()
				.map(SupplierItemResponse::from)
				.toList();

		return new ItemDetailResponse(item.getItemId(), item.getItemCode(), item.getItemName(), item.getUnit(),
				item.getOtherUnitName(), defaultSalesPrice, item.getStatus(), item.getMemo(), suppliers,
				item.getVersion());
	}
}
