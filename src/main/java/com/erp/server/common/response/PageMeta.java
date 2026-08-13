package com.erp.server.common.response;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * 목록 조회 시 페이지 번호, 페이지 크기, 전체 데이터 수, 전체 페이지 수, 정렬 조건 등 페이징 부가 정보를 동일한 형식으로 반환하기 위한 객체
 * @param page          현재 페이지 번호
 * @param size          한 페이지당 데이터 개수
 * @param totalElements 조건에 맞는 전체 데이터 개수
 * @param totalPages    전체 페이지 개수
 * @param sort          적용된 정렬 조건 목록
 */
public record PageMeta(int page, int size, long totalElements, int totalPages, List<String> sort) {
	/**
	 * 
     * Spring Data 가 제공하는 Page 객체에서 페이지 관련 정보를 꺼내 PageMeta 객체로 변환하는 static 메서드
     * PageMeta 메서드는 Page 안의 실제 데이터 타입을 사용하지 않고, Page 자체의 페이징 정보만 사용하므로 Page<?>로 받는다.
     * 
     */
	public static PageMeta from(Page<?> page) {
        /*
         * page.getSort(): Page 객체가 가지고 있는 정렬 정보를 가져온다.
         * stream(): Sort 안에 있는 여러 정렬 조건을 하나씩 처리할 수 있는 Stream 으로 만든다.
         * map(order -> ...): Stream 안의 Sort.Order 객체를 하나씩 받아 "필드명,정렬방향" 형식의 String 으로 변환한다.
         * order: stream() 안에 들어있는 데이터가 하나씩 자동으로 order 에 들어온다. 두 개라면 map()은 두 번 실행된다고 생각하면 된다.
         * order.getProperty(): 정렬 대상 필드명을 가져온다. 예: createdAt
         * order.isAscending(): 해당 정렬 조건이 오름차순인지 확인한다.
         * toList(): map()으로 변환된 여러 정렬 문자열을 하나의 List<String>으로 만든다.
         * 예: status ASC, createdAt DESC -> ["status,asc", "createdAt,desc"]
         * 정렬 조건이 하나도 없으면 별도로 null 처리할 필요 없이 "sort": []가 된다.
         */
        List<String> sort = page.getSort().stream()
        		.map(order -> order.getProperty() + "," + (order.isAscending() ? "asc" : "desc")).toList();
        
        /*
         * Page 객체에서 필요한 페이징 정보를 꺼내 새로운 PageMeta 객체를 생성하여 return
         * page.getNumber(): 현재 페이지 번호
         * page.getSize(): 한 페이지당 데이터 개수
         * page.getTotalElements(): 조건에 맞는 전체 데이터 개수
         * page.getTotalPages(): 전체 페이지 개수
         * sort: 적용된 정렬 조건 목록 -> 예: ["status,asc", "createdAt,desc"]
         */
        return new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), sort);
    }
}