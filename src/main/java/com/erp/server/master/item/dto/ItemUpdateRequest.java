package com.erp.server.master.item.dto;

import java.math.BigDecimal;

import com.erp.server.master.item.domain.ItemUnit;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// ********** 품목 기본정보·기본 판매가격 변경값과 동시 수정 확인용 version을 전달받기 위한 요청 DTO record **********
// 품목 코드는 변경할 수 없고 사용 상태는 별도 API로 변경하므로 수정 요청에 포함하지 않는다.
public record ItemUpdateRequest(

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
		String memo,

		@NotNull(message = "version은 필수입니다.")
		@PositiveOrZero(message = "version은 0 이상이어야 합니다.")
		Long version

) {

	// ========== OTHER 단위를 선택했을 때 기타 단위명이 입력되었는지 검증하는 메서드 ==========
	@AssertTrue(message = "OTHER 단위는 기타 단위명이 필수입니다.")
	public boolean isOtherUnitNameValid() {
		return unit != ItemUnit.OTHER || (otherUnitName != null && !otherUnitName.isBlank());
	}
}
