package com.auto.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AutoTestBackendApplication {
    public static void main(String[] args) {
        // 启动 Spring Boot（驱动路径在 SeleniumUtil 中手动指定，不再依赖 WebDriverManager）。
        SpringApplication.run(AutoTestBackendApplication.class, args);
    }
}