package com.erp.server.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// ********** ApiResponse의 정적 팩토리 메서드가 공통 성공 응답 규칙에 맞는 객체를 생성하는지 확인하기 위한 단위 테스트 클래스 **********
class ApiResponseTest {

	// ========== 성공 응답의 값과 null 경고 목록의 기본 처리를 함께 검증하는 테스트 메서드 ==========
	@Test
	@DisplayName("성공 응답은 값을 유지하고 null 경고 목록을 빈 목록으로 변환한다")
	void successCreatesExpectedResponse() {

		// Type Casting
		// 현재 테스트에서는 null을 List<String> 타입으로 지정하여, 여러 success() 오버로딩 메서드 중
		// success(T data, List<String> warnings)가 선택되도록 사용
		ApiResponse<String> response = ApiResponse.success("result", (java.util.List<String>) null);

		assertThat(response.success()).isTrue();
		assertThat(response.data()).isEqualTo("result");
		assertThat(response.meta()).isNull();
		assertThat(response.warnings()).isEmpty();
	}
}