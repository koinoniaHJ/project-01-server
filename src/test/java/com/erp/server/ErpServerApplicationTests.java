package com.erp.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 
 * {@code @ActiveProfiles("test")}: 테스트 실행 시 다음 파일을 적용하라는 의미
 * src/test/resources/application-test.properties
 */
@SpringBootTest
@ActiveProfiles("test")
class ErpServerApplicationTests {

    @Test
    void contextLoads() {
    }

}