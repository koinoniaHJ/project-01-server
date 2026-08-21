package com.erp.server.common.exception;

import org.springframework.http.HttpStatus;

// ********** API 오류마다 사용할 HTTP 상태와 기본 메시지를 한곳에서 관리하고 상수명을 오류 코드로 사용하기 위한 오류 코드 enum **********
public enum ErrorCode {
	// 400 Bad Request
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
	// 401 Unauthorized
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않았거나 세션이 만료되었습니다."),
	// 403 Forbidden
	FORBIDDEN(HttpStatus.FORBIDDEN, "요청을 처리할 권한이 없습니다."),
	// 404 Not Found
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "대상 자원을 찾을 수 없습니다."),
	// 409 Conflict
	CONFLICT(HttpStatus.CONFLICT, "현재 상태에서는 요청을 처리할 수 없습니다."),
	// 423 Locked
	RESOURCE_LOCKED(HttpStatus.LOCKED, "다른 업무로 인해 대상 자원의 처리가 제한되어 있습니다."),
	// 500 Internal Server Error
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 처리 중 예상하지 못한 오류가 발생했습니다."),
	// 502 Bad Gateway
	MAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "이메일 전송에 실패했습니다.");

	// 각 ErrorCode 상수가 가지는 HTTP 상태
	private final HttpStatus httpStatus;

	// 각 ErrorCode 상수가 가지는 기본 오류 메시지
	private final String message;

	// ========== ErrorCode 상수를 초기화하는 생성자 ==========
	// Enum 상수의 괄호 안에 전달된 값을 각 상수 객체의 필드에 저장한다.
	ErrorCode(HttpStatus httpStatus, String message) {
		this.httpStatus = httpStatus;
		this.message = message;
	}

	// ========== HTTP 상태를 반환하는 메서드 ==========
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	// ========== 클라이언트용 오류 코드를 반환하는 메서드 ==========
	public String getCode() {
		return name();
	}

	// ========== 기본 오류 메시지를 반환하는 메서드 ==========
	public String getMessage() {
		return message;
	}
}