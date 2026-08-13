package com.erp.server.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 목적: API 오류 발생 시 클라이언트에 전달할 HTTP 상태, 오류 코드, 기본 메시지를 동일한 기준으로 정의하기 위한 공통 Enum
 * 오류 코드는 ErrorCode의 Enum 상수명을 그대로 사용한다.
 */
public enum ErrorCode {

    /********** 미리 정의해 두는 ErrorCode Enum 상수 **********
     * ErrorCode의 각 Enum 상수는 ErrorCode 타입의 객체이다.
     */

    // 400 Bad Request: 요청값 검증 실패 또는 요청 형식 오류
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

    // 401 Unauthorized: 미인증 또는 세션 만료
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않았거나 세션이 만료되었습니다."),

    // 403 Forbidden: 역할 권한 부족
    FORBIDDEN(HttpStatus.FORBIDDEN, "요청을 처리할 권한이 없습니다."),

    // 404 Not Found: 대상 자원을 찾을 수 없음
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "대상 자원을 찾을 수 없습니다."),

    // 409 Conflict: version 불일치 또는 현재 상태·업무 조건과 요청이 충돌하는 경우
    CONFLICT(HttpStatus.CONFLICT, "현재 상태에서는 요청을 처리할 수 없습니다."),

    // 423 Locked: 다른 업무로 대상 자원이 제한됨
    RESOURCE_LOCKED(HttpStatus.LOCKED, "다른 업무로 인해 대상 자원의 처리가 제한되어 있습니다."),

    // 500 Internal Server Error: 예상하지 못한 서버 또는 DB 오류
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 처리 중 예상하지 못한 오류가 발생했습니다."),

    // 502 Bad Gateway: 이메일 전송이 주 처리인 API의 SMTP 전송 실패
    MAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "이메일 전송에 실패했습니다.");

    // 각 ErrorCode가 가지고 있는 HTTP 상태
    private final HttpStatus httpStatus;

    // 오류 발생 시 사용할 기본 메시지
    private final String message;

    /**
     * ErrorCode Enum 상수를 생성할 때 호출되는 생성자
     * 각 Enum 상수의 괄호 안에 지정한 값은 ErrorCode 생성자의 매개변수로 전달되고,
     * 생성자를 통해 해당 Enum 객체의 httpStatus, message 필드에 저장된다.
     */
    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    // 다른 클래스에서 ErrorCode Enum 상수가 가지고 있는 httpStatus 값을 읽어오기 위해 사용하는 메서드.
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    // Enum 상수명을 클라이언트에 전달할 오류 코드로 사용한다.
    public String getCode() {
        return name();
    }

    // 다른 클래스에서 ErrorCode Enum 상수가 가지고 있는 message 값을 읽어오기 위해 사용하는 메서드.
    public String getMessage() {
        return message;
    }
}