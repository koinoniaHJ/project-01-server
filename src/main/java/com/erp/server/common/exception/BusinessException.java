package com.erp.server.common.exception;

// ********** Service에서 발생한 업무 규칙 위반을 ErrorCode와 함께 전역 예외 처리기까지 전달하기 위한 공통 업무 예외 클래스 **********
// RuntimeException을 상속하므로 throws 선언을 강제하지 않는 Unchecked Exception으로 사용할 수 있다.
public class BusinessException extends RuntimeException {

    // 발생한 업무 오류의 HTTP 상태와 기본 메시지를 찾을 수 있도록 ErrorCode 객체를 저장한다.
    private final ErrorCode errorCode;

    // ========== ErrorCode의 기본 메시지로 업무 예외를 생성하는 생성자 ==========
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // ========== 별도 메시지로 업무 예외를 생성하는 생성자 ==========
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    // ========== 예외가 가진 ErrorCode를 반환하는 메서드 ==========
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}