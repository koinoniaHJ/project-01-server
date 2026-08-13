package com.erp.server.common.response;

import java.util.List;

/**
 * Controller가 클라이언트에 전달하는 오류 응답의 형식을 success, error, code, message, fieldErrors, traceId로 통일하기 위한 공통 응답 객체
 *
 * 오류 응답 형식 success / error
 * error 내부 code / message / fieldErrors / traceId
 * @param success 요청 성공 여부
 * @param error   오류 상세 정보
 * 
 * ErrorDetail error: 같은 Record 내부의 멤버 타입은 선언 순서와 관계없이 사용할 수 있으므로, 
 * 아래에 선언된 ErrorDetail을 record component의 타입으로 사용할 수 있다.
 */
public record ErrorResponse(boolean success, ErrorDetail error) {
	
	// ********** fieldErrors가 없는 일반 오류 응답을 생성하는 static 메서드 **********
    public static ErrorResponse of(String code, String message, String traceId) {
        return new ErrorResponse(false, new ErrorDetail(code, message, List.of(), traceId));
    }

    // ********** fieldErrors가 포함된 오류 응답을 생성하는 static 메서드 **********
    public static ErrorResponse of(String code, String message, List<FieldError> fieldErrors, String traceId) {
        return new ErrorResponse(false, new ErrorDetail(code, message, fieldErrors == null ? List.of() : fieldErrors, traceId));
    }

    /**
     * ****************************** 중첩 record ******************************
     * 오류 응답에서만 사용하는 중첩 JSON 구조를 Java에서도 같은 구조로 표현하고, 
     * ErrorDetail과 FieldError는 ErrorResponse에서만 사용하는 관련 응답 타입이므로 ErrorResponse 안에서 함께 관리하기 위해 중첩 record를 사용한다.
     *
     * @param code        오류 코드
     * @param message     오류 메시지
     * @param fieldErrors 필드별 검증 오류 목록(검증 오류는 하나의 요청에서 여러 필드에 동시에 발생할 수 있기 때문에 List)
     * @param traceId     오류 추적용 식별자
     * 
     ********** error 객체 내부의 상세 오류 정보를 담는 객체 **********
     */
    public record ErrorDetail(String code, String message, List<FieldError> fieldErrors, String traceId) {}

    /**
     * @param field  	오류가 발생한 필드
     * @param code		Spring Validation에서 제공하는 검증 오류 코드
     * 
     ********** 요청 데이터의 특정 필드에서 발생한 검증 오류 하나를 담는 객체 **********
     */
    public record FieldError(String field, String code) {}
}