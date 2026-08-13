package com.erp.server.common.response;
import java.util.List;

/**
 * Controller가 클라이언트에 전달하는 성공 응답을 success, data, meta, warnings 의 동일한 형식으로 반환하기 위한 객체
 * 
 * @param <T>		응답 데이터의 타입
 * @param success	요청 성공 여부
 * @param data		실제 응답 데이터
 * @param meta		페이지 조회 시 사용하는 부가 정보
 * @param warnings	정상 처리되었지만 사용자에게 전달할 경고 코드 목록
 */

public record ApiResponse<T>(boolean success, T data, PageMeta meta, List<String> warnings) {
	/**
	 * 성공 응답을 생성하여 return 하는 메서드이므로 success 자리에 무조건 true 를 지정한다.
	 * static 메서드로 ApiResponse.success(매개변수) 형태로 사용할 수 있도록 한다.
	 */
	
	/********** 일반 성공 응답 static 메서드 **********
	 * 페이지 정보가 없으므로 meta 는 null, 경고가 없으므로 warnings 는 빈 List 로 생성(List.of(): [])
	 */
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data, null, List.of()); 
	}
	/********** "경고가 포함된" 일반 성공 응답을 생성하는 static 메서드 **********
	 * 페이지 정보는 없으므로 meta 는 null 
	 */
    public static <T> ApiResponse<T> success(T data, List<String> warnings) {
        return new ApiResponse<>(true, data, null, warnings == null ? List.of() : warnings);
    }
    /********** "페이지 정보가 포함된" 성공 응답을 생성하는 static 메서드 **********
     * 경고가 없으므로 warnings 는 빈 List 로 생성(List.of(): [])
     */
    public static <T> ApiResponse<T> success(T data, PageMeta meta) {
        return new ApiResponse<>(true, data, meta, List.of());
    }
    /********** "페이지 정보와 경고가 모두 포함된" 성공 응답을 생성하는 static 메서드 **********/
    public static <T> ApiResponse<T> success(T data, PageMeta meta, List<String> warnings) {
        return new ApiResponse<>(true, data, meta, warnings == null ? List.of() : warnings);
    }
}

/**
 * <T>: success() 메서드에서 사용할 Generic 타입 T를 선언
 * ApiResponse<T>: success() 메서드의 반환 타입
 * success(T data): T 타입의 data 를 매개변수로 받는다.
 *
 * return new ApiResponse<>(매개변수): 새로운 ApiResponse 객체를 생성하여 반환한다.
 * Controller에 {@code @RestController}가 적용되어 있으면 Spring MVC가 메서드의 반환 객체를 HTTP 응답 본문으로 처리하고, 
 * Jackson을 통해 JSON으로 직렬화하여 클라이언트에 전달한다. (Spring Web 의존성이 제공하는 Spring MVC 기능)
 */