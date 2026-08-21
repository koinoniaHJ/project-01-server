package com.erp.server.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import com.erp.server.common.filter.TraceIdFilter;

// ********** GlobalExceptionHandler와 TraceIdFilter를 연결하여 공통 오류 응답 규칙을 확인하기 위한 Spring MVC 테스트 클래스 **********
class GlobalExceptionHandlerTest {

    // 실제 HTTP 서버를 실행하지 않고 Controller 요청과 응답을 검사하는 객체
    private MockMvc mockMvc;

    // ========== 각 테스트 전에 MockMvc를 구성하는 설정 메서드 ==========
    @BeforeEach
    void setUp() {
        // 테스트용 Controller, 전역 예외 처리기와 traceId Filter만 연결하여 MockMvc를 구성한다.
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new TraceIdFilter())
                .build();
    }

    // ========== BusinessException 응답을 검증하는 테스트 메서드 ==========
    @Test
    @DisplayName("업무 예외는 ErrorCode의 상태와 오류 응답으로 반환한다")
    void handlesBusinessException() throws Exception {

        mockMvc.perform(get("/test/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.fieldErrors").isEmpty())
                .andExpect(jsonPath("$.error.traceId").isNotEmpty());
    }

    // ========== RequestBody DTO Validation 응답을 검증하는 테스트 메서드 ==========
    @Test
    @DisplayName("RequestBody 검증 실패는 필드 오류가 포함된 400 응답을 반환한다")
    void handlesRequestBodyValidation() throws Exception {

        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.error.fieldErrors[0].code").value("NotBlank"))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty());
    }

    // ========== Controller 반환값 Validation 응답을 검증하는 테스트 메서드 ==========
    @Test
    @DisplayName("Controller 반환값 검증 실패는 500 응답을 반환한다")
    void handlesReturnValueValidation() throws Exception {

        mockMvc.perform(get("/test/return-value"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty());
    }

    // ========== 예상하지 못한 예외 응답을 검증하는 테스트 메서드 ==========
    @Test
    @DisplayName("처리되지 않은 예외는 내부 정보 없이 500 응답을 반환한다")
    void handlesUnexpectedException() throws Exception {

        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.error.message")
                        .value("서버 처리 중 예상하지 못한 오류가 발생했습니다."))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty());
    }

    // ********** GlobalExceptionHandler의 각 처리 경로를 실행하기 위해 테스트 코드 안에서만 사용하는 Controller 클래스 **********
    @RestController
    @RequestMapping("/test")
    static class TestController {

        // ========== BusinessException을 발생시키는 테스트용 API 메서드 ==========
        @GetMapping("/business")
        void business() {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // ========== RequestBody DTO Validation을 실행하는 테스트용 API 메서드 ==========
        @PostMapping("/validation")
        void validation(@Valid @RequestBody TestRequest request) {
        }

        // ========== Controller 반환값 Validation을 실행하는 테스트용 API 메서드 ==========
        @GetMapping("/return-value")
        @Min(1)
        int returnValue() {
            return 0; // @Min(1)을 만족하지 않는 서버 반환값
        }

        // ========== 예상하지 못한 예외를 발생시키는 테스트용 API 메서드 ==========
        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("테스트용 내부 예외");
        }
    }

    // ********** RequestBody Validation 실패를 확인하기 위한 테스트용 요청 record **********
    record TestRequest(
            @NotBlank String name
    ) {}
}