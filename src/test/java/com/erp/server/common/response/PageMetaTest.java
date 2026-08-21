package com.erp.server.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

// ********** Spring Data의 Page가 프로젝트의 PageMeta 형식으로 정확히 변환되는지 확인하기 위한 단위 테스트 클래스 **********
class PageMetaTest {

	// ========== 페이지 정보와 정렬 조건 변환을 검증하는 테스트 메서드 ==========
	@Test
	@DisplayName("Page의 페이징 정보와 정렬 조건을 PageMeta로 변환한다")
	void convertsPageToPageMeta() {

		PageRequest pageRequest = PageRequest.of(1, 2, Sort.by(Sort.Order.asc("status"), Sort.Order.desc("createdAt")));

		Page<String> page = new PageImpl<>(List.of("A", "B"), pageRequest, 5);

		PageMeta meta = PageMeta.from(page);

		assertThat(meta.page()).isEqualTo(1);
		assertThat(meta.size()).isEqualTo(2);
		assertThat(meta.totalElements()).isEqualTo(5);
		assertThat(meta.totalPages()).isEqualTo(3);
		assertThat(meta.sort()).containsExactly("status,asc", "createdAt,desc");
	}
}