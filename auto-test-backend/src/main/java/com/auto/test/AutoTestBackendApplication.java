package com.auto.test;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.auto.test.mapper") // 扫描MyBatis的Mapper接口
public class AutoTestBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutoTestBackendApplication.class, args);
    }
}