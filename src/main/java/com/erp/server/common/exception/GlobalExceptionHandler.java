package com.erp.server.common.exception;

import java.util.List;

import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.web.bind.MethodArgumentNotValidException;

import org.springframework.web.bind.MissingServletRequestParameterException;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.erp.server.common.filter.TraceIdFilter;
import com.erp.server.common.response.ErrorResponse;

// ********** Controller까지 전달된 예외를 종류별로 처리하고 정해진 HTTP 상태와 ErrorResponse로 변환하여 반환하기 위한 전역 예외 처리 클래스 **********
// @RestControllerAdvice 덕분에 Spring MVC가 이 클래스를 전역 예외 처리기로 찾아 사용한다.
// Spring Security Filter에서 발생하는 인증·인가 오류는 Security 전용 처리기에서 별도로 처리한다.
@RestControllerAdvice
public class GlobalExceptionHandler {

	// 예상하지 못한 예외의 메시지와 Stack Trace를 서버 로그에 기록하기 위한 Logger 객체
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// ========== 현재 요청의 traceId를 조회하는 메서드 ==========
	private String getTraceId() {
		return MDC.get(TraceIdFilter.TRACE_ID_KEY);
	}

	// ========== Service의 업무 예외를 공통 오류 응답으로 변환하는 메서드 ==========
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {

		// 업무 예외에 저장된 ErrorCode와 메시지로 프로젝트의 공통 오류 응답을 만든다.
		ErrorCode errorCode = exception.getErrorCode();

		ErrorResponse response = ErrorResponse.of(errorCode.getCode(), exception.getMessage(), getTraceId());

		return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
	}

	// ========== @Valid가 적용된 @RequestBody DTO의 검증 실패를 공통 오류 응답으로 변환하는 메서드 ==========
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {

		ErrorCode errorCode = ErrorCode.INVALID_INPUT;

		// Spring의 필드 오류에서 필드명과 검증 코드만 꺼내 프로젝트의 FieldError로 변환한다.
		List<ErrorResponse.FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> new ErrorResponse.FieldError(fieldError.getField(), fieldError.getCode())).toList();

		ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), fieldErrors,
				getTraceId());

		return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
	}

	// ========== Controller 메서드의 파라미터·반환값 검증 실패를 공통 오류 응답으로 변환하는 메서드 ==========
	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ErrorResponse> handleMethodValidationException(HandlerMethodValidationException exception) {

		if (exception.isForReturnValue()) {
			// 반환값은 서버가 만든 값이므로 검증 실패를 클라이언트 입력 오류가 아닌 서버 오류로 처리한다.
			ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

			ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), getTraceId());

			return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
		}

		// @RequestParam이나 @PathVariable 검증 실패는 잘못된 요청값으로 처리한다.
		ErrorCode errorCode = ErrorCode.INVALID_INPUT;

		ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), getTraceId());

		return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
	}

	// ========== @RequestBody JSON 해석 오류를 처리하는 메서드 ==========
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
			HttpMessageNotReadableException exception) {

		ErrorCode errorCode = ErrorCode.INVALID_INPUT;

		ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), getTraceId());

		return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
	}

	// ========== 필수 @RequestParam 누락 오류를 처리하는 메서드 ==========
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingRequestParameterException(
			MissingServletRequestParameterException exception) {

		ErrorCode errorCode = ErrorCode.INVALID_INPUT;

		ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), getTraceId());

		return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
	}

	// ========== @RequestParam·@PathVariable 타입 변환 실패를 처리하는 메서드 ==========
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
			MethodArgumentTypeMismatchException exception) {

		ErrorCode errorCode = ErrorCode.INVALID_INPUT;

		ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), getTraceId());

		return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
	}

	// ========== 존재하지 않는 URL 또는 정적 리소스 요청을 처리하는 메서드 ==========
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException exception) {

		ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;

		ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), getTraceId());

		return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
	}

	// ========== 예상하지 못한 예외를 최종 처리하는 메서드 ==========
	// 위의 구체적인 예외 처리 메서드에서 처리하지 못한 모든 Exception을 마지막에 처리한다.
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception exception) {

		ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

		// 클라이언트에는 내부 예외 정보를 노출하지 않고 공통 메시지만 반환한다.
		log.error("예상하지 못한 예외가 발생했습니다.", exception);

		ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), getTraceId());

		return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
	}
}