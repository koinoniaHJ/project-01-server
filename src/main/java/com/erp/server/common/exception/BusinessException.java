package com.erp.server.common.exception;

/**
 * 업무 처리 중 규칙 위반이 발생했을 때 ErrorCode를 담아 예외를 발생시키기 위한 공통 예외 클래스
 */
public class BusinessException extends RuntimeException { 

    /* 발생한 업무 오류의 ErrorCode 상수 객체 자체를 저장하는 필드
	 * 예: throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
	 */
    private final ErrorCode errorCode;

    // ********** ErrorCode에 정의된 기본 메시지를 사용하는 생성자 **********
    public BusinessException(ErrorCode errorCode) {
    	/* super(...)는 부모 클래스의 생성자를 호출하는 문법이다.
    	 * 부모 클래스인 RuntimeException에 String 하나를 전달하면, 해당 문자열을 예외 메시지로 저장하는 생성자를 호출하는 것이다.
    	 * 저장된 예외 메시지는 BusinessException에서도 getMessage()로 조회할 수 있다.
    	 */
        super(errorCode.getMessage());	
        this.errorCode = errorCode; // ErrorCode 상수 자체를 BusinessException의 필드에 저장 

    }

    /**
     ********** ErrorCode는 그대로 사용하고, 기본 메시지 대신 별도의 메시지를 사용하고 싶을 때 사용하는 생성자 **********
     * 예: throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "주문 정보를 찾을 수 없습니다."));
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message); // RuntimeException이 관리하는 예외 message 자리에 저장
        this.errorCode = errorCode;
    }

    // 예외가 가지고 있는 ErrorCode 객체를 반환한다.
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}