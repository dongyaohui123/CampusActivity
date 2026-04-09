package com.campus.activity;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.campus.activity.mapper")
public class CampusActivityBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusActivityBackendApplication.class, args);
    }
}
