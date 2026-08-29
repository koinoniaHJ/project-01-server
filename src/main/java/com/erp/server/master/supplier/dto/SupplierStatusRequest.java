package com.erp.server.master.supplier.dto;

import com.erp.server.master.common.domain.MasterStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// ********** 공급업체 사용 상태 변경값과 동시 수정 확인용 version을 전달받기 위한 요청 DTO record **********
// 공급업체 사용 상태 변경 API가 ACTIVE 또는 INACTIVE 값과 동시 수정 확인용 version을 전달받는다.
public record SupplierStatusRequest(

        @NotNull(message = "공급업체 사용 상태는 필수입니다.")
        MasterStatus status,

        @NotNull(message = "version은 필수입니다.")
        @PositiveOrZero(message = "version은 0 이상이어야 합니다.")
        Long version

) {
}
