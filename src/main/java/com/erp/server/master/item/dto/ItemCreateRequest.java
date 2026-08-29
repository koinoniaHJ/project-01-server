package com.erp.server.master.item.dto;

import java.math.BigDecimal;

import com.erp.server.master.item.domain.ItemUnit;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// ********** 신규 품목 등록에 필요한 품목명·단위·기본 판매가격과 메모를 전달받기 위한 요청 DTO record **********
// 품목 코드는 서버에서 자동 생성하고 사용 상태·감사 정보·version은 클라이언트에서 받지 않는다.
public record ItemCreateRequest(

		@NotBlank(message = "품목명은 필수입니다.")
		@Size(max = 150, message = "품목명은 150자 이하여야 합니다.")
		String itemName,

		@NotNull(message = "품목 단위는 필수입니다.")
		ItemUnit unit,

		@Size(max = 50, message = "기타 단위명은 50자 이하여야 합니다.")
		String otherUnitName,

		@NotNull(message = "기본 판매가격은 필수입니다.")
		@DecimalMin(value = "0.00", message = "기본 판매가격은 0 이상이어야 합니다.")
		@Digits(integer = 17, fraction = 2, message = "기본 판매가격은 정수 17자리와 소수 2자리 이하여야 합니다.")
		BigDecimal defaultSalesPrice,

		@Size(max = 2000, message = "품목 메모는 2000자 이하여야 합니다.")
		String memo

) {

	// ========== OTHER 단위를 선택했을 때 기타 단위명이 입력되었는지 검증하는 메서드 ==========
	@AssertTrue(message = "OTHER 단위는 기타 단위명이 필수입니다.")
	public boolean isOtherUnitNameValid() {
		return unit != ItemUnit.OTHER || (otherUnitName != null && !otherUnitName.isBlank());
	}
}
