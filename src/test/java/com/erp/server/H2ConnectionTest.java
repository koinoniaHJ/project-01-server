package com.erp.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// ********** test 프로필의 H2 DB 연결과 SQL 실행 여부를 검증하는 클래스 **********
@SpringBootTest
@ActiveProfiles("test")
class H2ConnectionTest {

    // application-test.properties를 바탕으로 생성된 H2 DataSource를 사용한다.
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ========== H2 DB에 SQL을 실행하고 조회 결과를 검증 ==========
    @Test
    void connectsToH2() {

        Integer result = jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class
        );

        assertThat(result).isEqualTo(1);
    }
}