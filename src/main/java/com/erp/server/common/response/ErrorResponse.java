package com.erp.server.common.response;

import java.util.List;

// ********** 오류 결과를 success, error.code, error.message, error.fieldErrors, error.traceId 형식으로 통일하여 반환하기 위한 공통 오류 응답 record **********
public record ErrorResponse(boolean success, // 요청 성공 여부이며 오류 응답에서는 항상 false
		ErrorDetail error 					 // 오류 코드, 메시지, 필드 오류와 traceId를 담는 상세 객체
) {

	// ========== 필드 오류가 없는 응답을 생성하는 정적 팩토리 메서드 ==========
	public static ErrorResponse of(String code, String message, String traceId) {
		return new ErrorResponse(false, new ErrorDetail(code, message, List.of(), traceId));
	}

	// ========== 필드 오류가 포함된 응답을 생성하는 정적 팩토리 메서드 ==========
	public static ErrorResponse of(String code, String message, List<FieldError> fieldErrors, String traceId) {

		return new ErrorResponse(false,
				new ErrorDetail(code, message, fieldErrors == null ? List.of() : fieldErrors, traceId));
	}

	// ********** JSON의 error 객체에 들어갈 상세 오류 정보를 ErrorResponse 안에서 함께 관리하기 위한 중첩 **********
	public record ErrorDetail(String code, String message, List<FieldError> fieldErrors, String traceId) {
	}

	// ********** RequestBody DTO의 특정 필드에서 발생한 Validation 오류 하나를 표현하기 위한 중첩 record **********
	public record FieldError(String field, String code) {
	}
}