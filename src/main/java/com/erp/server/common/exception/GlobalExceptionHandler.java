package com.erp.server.common.exception;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.erp.server.common.filter.TraceIdFilter;
import com.erp.server.common.response.ErrorResponse;

/**
 * Controller 까지 전달된 예외를 한곳에서 처리하여, 정해진 HTTP 상태와 ErrorResponse 형식으로 클라이언트에 반환하기 위한 전역 예외 처리 클래스
 * {@code @RestControllerAdvice}: 여러 Controller 에서 발생한 예외를 한곳에서 공통으로 처리할 수 있도록 Spring 에 등록하는 annotation
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	// 현재 요청의 MDC에 저장되어 있는 traceId를 가져온다. MDC.get("traceId")
	private String getTraceId() {
	    return MDC.get(TraceIdFilter.TRACE_ID_KEY);
	}
	
    /**
     * ========================= BusinessException 처리 =========================
     * Service 등에서 throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND) 같은 예외가 발생하면 
     * 해당 예외가 가지고 있는 ErrorCode를 이용하여 HTTP 상태와 오류 응답을 생성한다.
     * 
     * {@code @ExceptionHandler(BusinessException.class)}: BusinessException 클래스의 예외가 발생했을 때 바로 아래 메서드가 해당 예외를 처리하도록 지정하는 annotation
     * {@code ResponseEntity<ErrorResponse>}: ErrorResponse 객체와 HTTP 상태 코드를 함께 담아 클라이언트에 반환하기 위한 Spring의 응답 객체
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {

        // BusinessException 안에 저장되어 있는 ErrorCode 객체를 가져온다.
        ErrorCode errorCode = exception.getErrorCode();
        /*
         * ErrorResponse 생성
         * code: ErrorCode에 정의된 오류 코드
         * message: BusinessException의 실제 예외 메시지. 사용자 지정 메시지가 있다면 사용자 지정 메시지가 사용된다.
         * traceId: 현재 요청의 MDC에 저장된 traceId
         */
        ErrorResponse response = ErrorResponse.of(errorCode.getCode(), exception.getMessage(), getTraceId());
        /*
         * ResponseEntity는 ErrorCode에 정의된 HTTP 상태 코드와 응답 객체(ErrorResponse)를 함께 담아 반환하고,
         * Spring MVC가 ErrorResponse 객체를 Jackson을 통해 JSON으로 직렬화하여 클라이언트에 전달한다.
         */
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }
    
    // ========================= RequestBody DTO Validation 오류 처리 =========================
    /**
     * Controller 메서드에서 {@code @Valid}가 적용된 {@code @RequestBody} DTO의 Validation 조건이 실패했을 때
     * 필드별 검증 정보를 포함한 공통 오류 응답으로 처리하기 위함
     *
     * {@code @Valid}가 적용된 RequestBody DTO의 검증에 실패하면 MethodArgumentNotValidException이 발생한다.
     * 발생한 필드 오류를 ErrorResponse의 fieldErrors 형식으로 변환하여 400 Bad Request로 반환한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {

    	// Validation 오류는 INVALID_INPUT으로 처리한다.
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        
        /*
         * @Valid 검증에 실패하면 Spring은 어떤 필드에서 어떤 검증 오류가 발생했는지 필드별 오류 정보를 내부적으로 가지고 있다.
         * 예:
	     * field = "customerId"
	     * code = "NotNull"
	     * defaultMessage = "must not be null"
		 * 
		 * Spring의 검증 오류 정보에서 field와 code를 가져와 ErrorResponse.FieldError 형식으로 변환한다.
		 * {
		 *   "field": "customerId",
		 *   "code": "NotNull"
		 * }
         */
        List<ErrorResponse.FieldError> fieldErrors = 
        		exception.getBindingResult()
        				.getFieldErrors()
        				.stream() // Validation 예외에서 검증 결과를 가져온 뒤, 그중 필드별 오류 목록을 꺼내 Stream 형태로 변환한다.  
                        .map(fieldError ->
                                new ErrorResponse.FieldError(
                                        fieldError.getField(),
                                        fieldError.getCode()
                                ))
                        .toList();
        
        // fieldErrors가 포함된 ErrorResponse 생성.
        ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), fieldErrors, getTraceId());

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }
    
    // ========================= 메서드 파라미터 Validation 오류 처리 =========================
    /**
     * {@code @RequestParam}, {@code @PathVariable} 같은 Controller 메서드 파라미터에 직접 선언한 Validation 조건이 실패했을 때
     * 공통 오류 응답으로 처리하기 위함
     *
     * HandlerMethodValidationException 발생
     * 
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidationException(HandlerMethodValidationException exception) {

        // Controller 반환값 자체의 Validation 이 실패한 경우는 클라이언트 요청 오류가 아니라 서버 처리 오류이므로 500으로 처리한다.
        if (exception.isForReturnValue()) {

            ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

            ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), getTraceId());

            return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
        }

        // 요청 파라미터 Validation 실패는 INVALID_INPUT으로 처리한다.
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;

        ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), getTraceId());

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }
    
    // ========================= 기타 잘못된 요청 형식 처리 =========================
    /**
     * RequestBody JSON을 정상적으로 읽을 수 없는 경우 처리
     *
     * HttpMessageNotReadableException 발생
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {

        ErrorCode errorCode = ErrorCode.INVALID_INPUT;

        ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), getTraceId());

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }


    /**
     * 필수 RequestParam이 누락된 경우 처리
     *
     * MissingServletRequestParameterException 발생
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameterException(MissingServletRequestParameterException exception) {

        ErrorCode errorCode = ErrorCode.INVALID_INPUT;

        ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), getTraceId());

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }


    /**
     * RequestParam, PathVariable 등의 타입 변환에 실패한 경우 처리
     *
     * MethodArgumentTypeMismatchException 발생
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception) {

        ErrorCode errorCode = ErrorCode.INVALID_INPUT;

        ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), getTraceId());

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }
    
    // ========================= 존재하지 않는 URL 또는 정적 리소스 요청 처리 =========================
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException exception) {

        ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;

        ErrorResponse response = ErrorResponse.of(
                errorCode.getCode(),
                errorCode.getMessage(),
                getTraceId()
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }
    
    
    // GlobalExceptionHandler에서 발생한 오류를 서버 로그에 기록하기 위한 Logger 객체
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * ========================= 예상하지 못한 예외 처리 =========================
     * 특정 예외를 처리하도록 별도로 정의한 {@code @ExceptionHandler}가 없는 경우,
     * 해당 예외를 최종적으로 처리하여 500 Internal Server Error로 응답한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        
        // 실제 예외가 발생했을 때 ERROR 레벨로 메시지와 예외 정보를 서버 로그에 기록
        log.error("예상하지 못한 예외가 발생했습니다.", exception);

        ErrorResponse response = ErrorResponse.of(errorCode.getCode(),errorCode.getMessage(), getTraceId());

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }
}