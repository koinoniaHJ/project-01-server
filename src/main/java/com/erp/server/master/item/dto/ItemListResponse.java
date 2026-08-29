package com.erp.server.master.item.dto;

import java.math.BigDecimal;

import com.erp.server.common.user.domain.UserRole;
import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.item.domain.Item;
import com.erp.server.master.item.domain.ItemUnit;

// ********** 품목 목록 화면에 필요한 품목 코드·명칭·단위·기본 판매가격·사용 상태를 반환하고 WAREHOUSE 역할에는 기본 판매가격을 숨기기 위한 응답 DTO record **********
// 목록에 필요하지 않은 기타 단위명·메모·감사 정보는 제외하고, version은 품목 선택 시 상세 API에서 최신 값으로 조회한다.
public record ItemListResponse(Long itemId, String itemCode, String itemName, ItemUnit unit,
		BigDecimal defaultSalesPrice, MasterStatus status) {

	// ========== Item Entity를 역할별 조회 범위가 적용된 ItemListResponse로 변환하는 정적 팩토리 메서드 ==========
	public static ItemListResponse from(Item item, UserRole userRole) {

		BigDecimal defaultSalesPrice = userRole == UserRole.WAREHOUSE ? null : item.getDefaultSalesPrice();

		return new ItemListResponse(item.getItemId(), item.getItemCode(), item.getItemName(), item.getUnit(),
				defaultSalesPrice, item.getStatus());
	}
}
