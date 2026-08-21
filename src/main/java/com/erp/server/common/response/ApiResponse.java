package com.erp.server.common.response;

import java.util.List;

// ********** Controller의 성공 결과를 success, data, meta, warnings 형식으로 통일하여 반환하기 위한 공통 성공 응답 record **********
public record ApiResponse<T>(boolean success,	// 요청 성공 여부
		T data, 								// API마다 달라지는 실제 응답 데이터
		PageMeta meta,							// 목록 조회에 사용하는 페이지 정보
		List<String> warnings					// 처리는 성공했지만 사용자에게 알려야 하는 경고 코드 목록
) {

	// ========== 일반 성공 응답을 생성하는 정적 팩토리 메서드 ==========
	// 페이지 정보와 경고가 없는 응답을 만든다.
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data, null, List.of());
	}

	// ========== 경고가 포함된 성공 응답을 생성하는 정적 팩토리 메서드 ==========
	public static <T> ApiResponse<T> success(T data, List<String> warnings) {
		return new ApiResponse<>(true, data, null, warnings == null ? List.of() : warnings);
	}

	// ========== 페이지 정보가 포함된 성공 응답을 생성하는 정적 팩토리 메서드 ==========
	public static <T> ApiResponse<T> success(T data, PageMeta meta) {
		return new ApiResponse<>(true, data, meta, List.of());
	}

	// ========== 페이지 정보와 경고가 포함된 성공 응답을 생성하는 정적 팩토리 메서드 ==========
	public static <T> ApiResponse<T> success(T data, PageMeta meta, List<String> warnings) {
		return new ApiResponse<>(true, data, meta, warnings == null ? List.of() : warnings);
	}
}