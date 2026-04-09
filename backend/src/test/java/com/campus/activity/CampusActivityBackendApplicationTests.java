package com.campus.activity;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CampusActivityBackendApplicationTests {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Test
    void contextLoadsAndMybatisIsReady() {
        assertThat(sqlSessionFactory).isNotNull();
        assertThat(sqlSessionFactory.getConfiguration().getMappedStatementNames()).isNotEmpty();
    }
}
