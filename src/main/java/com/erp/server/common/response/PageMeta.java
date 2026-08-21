package com.erp.server.common.response;

import java.util.List;

import org.springframework.data.domain.Page;

// ********** 목록 조회 결과에 페이지 번호, 페이지 크기, 전체 데이터 수, 전체 페이지 수와 정렬 조건을 같은 형식으로 포함하여 반환하기 위한 페이징 응답 record **********

public record PageMeta(int page,	// 현재 페이지 번호이며 0부터 시작
		int size, 					// 한 페이지에 포함되는 데이터 개수
		long totalElements,			// 조건에 맞는 전체 데이터 개수
		int totalPages,				// 전체 페이지 개수
		List<String> sort			// "필드명,정렬방향" 형식의 정렬 조건 목록
) {

	// ========== Page 객체를 PageMeta로 변환하는 정적 팩토리 메서드 ==========
	// Page 안의 실제 데이터는 사용하지 않고 페이징 정보만 사용하므로 Page<?>로 받는다.
	//
	// 예시: Page<User>, Page<Order> 등 어떤 목록 결과든 받을 수 있다.
	public static PageMeta from(Page<?> page) {

		// page.getSort()는 Page에 적용된 정렬 조건이 담긴 Sort 객체를 반환한다.
		//
		// 예시
		// Sort
		// ├─ Sort.Order: status, ASC
		// └─ Sort.Order: createdAt, DESC
		//
		// Sort.stream()은 Sort.Order를 하나씩 처리할 수 있는 Stream<Sort.Order>를 반환한다.
		// Stream.map()은 각 Sort.Order를 "필드명,정렬방향" 문자열로 변환한다.
		// Stream.toList()는 변환된 문자열을 List<String>으로 모은다.
		List<String> sort = page.getSort().stream()
				.map(order -> order.getProperty() + "," + (order.isAscending() ? "asc" : "desc")).toList();

		return new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), sort);
	}
}