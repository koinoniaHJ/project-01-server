package com.erp.server.common.test;

//import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.response.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/**
 * 공통 응답 및 예외 처리를 확인하기 위한 임시 테스트 Controller 테스트 완료 후 삭제한다.
 * 기본 URL: /api/v1/test
 */
@RestController
@RequestMapping("/api/v1/test")
public class CommonTestController {

    // 성공 응답 테스트 GET /api/v1/test/success
    @GetMapping("/success")
    public ApiResponse<String> success() {

        return ApiResponse.success("success");
    }


    /**
     * BusinessException 테스트 GET /api/v1/test/business-error
     *
     * RESOURCE_NOT_FOUND를 직접 발생시켜 GlobalExceptionHandler가 처리하는지 확인한다.
     */
    @GetMapping("/business-error")
    public ApiResponse<Void> businessError() {

        throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "테스트 자원을 찾을 수 없습니다.");
    }


    /**
     * RequestBody DTO Validation 테스트 POST /api/v1/test/validation
     *
     * {@code @Valid}에 의해 TestValidationRequest의 Validation annotation을 검사한다.
     */
    @PostMapping("/validation")
    public ApiResponse<TestValidationRequest> validation(@Valid @RequestBody TestValidationRequest request) {

        return ApiResponse.success(request);
    }


    /**
     * 메서드 파라미터 Validation 테스트 GET /api/v1/test/parameter-validation?page=-1
     * page는 0 이상이어야 한다.
     * 
     * {@code @RequestParam("page")}: HTTP 요청에서 사용할 이름을 직접 명시
     */
    @GetMapping("/parameter-validation")
    public ApiResponse<Integer> parameterValidation(@RequestParam("page") @Min(0) int page) {

        return ApiResponse.success(page);
    }


    /**
     * 필수 RequestParam 누락 테스트 GET /api/v1/test/required-param
     *
     * keyword를 보내지 않으면 MissingServletRequestParameterException이 발생한다.
     */
    @GetMapping("/required-param")
    public ApiResponse<String> requiredParam(@RequestParam("keyword") String keyword) {

        return ApiResponse.success(keyword);
    }


    /**
     * RequestParam 타입 불일치 테스트 GET /api/v1/test/type-mismatch?number=abc
     *
     * int가 필요한데 문자열 abc를 보내면 MethodArgumentTypeMismatchException이 발생한다.
     */
    @GetMapping("/type-mismatch")
    public ApiResponse<Integer> typeMismatch(@RequestParam("number") int number) {

        return ApiResponse.success(number);
    }


    /**
     * 예상하지 못한 서버 오류 테스트 GET /api/v1/test/server-error
     *
     * BusinessException이 아닌 일반 RuntimeException을 발생시켜 최종 ExceptionHandler가 500으로 처리하는지 확인한다.
     */
    @GetMapping("/server-error")
    public ApiResponse<Void> serverError() {

        throw new RuntimeException("테스트용 서버 오류");
    }
    
    // GET /csrf를 요청하면 현재 세선의 csrf 토큰을 받을 수 있다.
//    @GetMapping("/csrf")
//    public CsrfToken csrf(CsrfToken csrfToken) {
//        return csrfToken;
//    }
}