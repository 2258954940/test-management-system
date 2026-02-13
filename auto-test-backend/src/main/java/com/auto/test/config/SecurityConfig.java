package com.auto.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; 
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 安全配置（适配Spring Security 6.x+，放行登录相关接口）。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. CORS配置（跨域）
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 2. 禁用CSRF（前后端分离必做）
            .csrf(AbstractHttpConfigurer::disable)
            // 3. 禁用表单登录/HTTP Basic（JWT场景）
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            // 4. 无状态会话（JWT不需要session）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 5. 权限规则（核心修改：去掉hasAuthority限制）
            .authorizeHttpRequests(auth -> auth
                // 放行登录/用户信息/退出接口（全方法）
                .requestMatchers("/api/user/**").permitAll()
                // 放行OPTIONS预检请求（跨域必加）
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 开发环境临时放行用例/元素接口
                .requestMatchers("/api/cases/**").permitAll()
                .requestMatchers("/api/element/**").permitAll()
                // 🔥 核心修改：放行/system/user/**（交给Controller自己校验X-Role）
                .requestMatchers("/api/system/user/**").permitAll()
                // 临时放行所有请求（毕设演示，避免其他接口拦截）
                .anyRequest().permitAll() 
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有前端域名访问（毕设演示）
        config.setAllowedOriginPatterns(java.util.Collections.singletonList("*"));
        config.setAllowCredentials(true);
        // 允许所有请求方法
        config.addAllowedMethod(CorsConfiguration.ALL);
        // 允许所有请求头（包括X-Role）
        config.addAllowedHeader(CorsConfiguration.ALL);
        // 暴露Authorization头（前端可获取）
        config.addExposedHeader("Authorization");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}