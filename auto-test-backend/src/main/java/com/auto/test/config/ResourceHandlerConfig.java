package com.auto.test.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 静态资源映射：将 /screenshots/** 映射到项目根目录的 screenshots 文件夹。
 */
@Configuration
public class ResourceHandlerConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String baseDir = System.getProperty("user.dir");
        String location = Paths.get(baseDir, "screenshots").toUri().toString();
        registry.addResourceHandler("/screenshots/**")
            .addResourceLocations(location);
    }
}
