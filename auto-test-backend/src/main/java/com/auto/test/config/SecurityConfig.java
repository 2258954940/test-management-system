package com.auto.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 安全相关配置（包含BCrypt加密、Security拦截规则、CORS跨域）。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. 先配置CORS（必须在csrf之前，否则跨域会被拦截）
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 2. 禁用CSRF（前后端分离场景必做）
            .csrf(csrf -> csrf.disable())
            // 3. 禁用表单登录/HTTP Basic（前后端分离用JWT）
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            // 4. 权限规则：精准放行登录接口，其他接口需认证
            .authorizeHttpRequests(auth -> auth
                // 核心：放行登录接口（重点检查路径是否和前端一致）
                // 若后端配置了server.servlet.context-path=/api → 这里写 /user/login
                // 若后端无context-path → 这里写 /api/user/login
                .requestMatchers("/api/user/login").permitAll()
                // 放行用例相关接口（开发环境临时用，生产需收窄）
                .requestMatchers("/api/cases", "/api/addCase", "/api/case/**", "/api/runCase").permitAll()
                // 新增：放行元素管理所有接口（关键！解决403）
                .requestMatchers("/api/element/**").permitAll()
                // 系统用户接口仅admin可访问
                .requestMatchers("/api/system/user/**").hasAuthority("admin")
                // 其他所有接口需认证
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 关键：Spring6.x推荐用addAllowedOriginPattern，兼容浏览器CORS限制
    // 开发环境：允许所有前端域名（生产环境替换为具体域名）
        config.addAllowedOriginPattern("*");        
         // 允许带凭证（Cookie/Token）
        config.setAllowCredentials(true);
        // 允许所有请求方法
        config.addAllowedMethod("*");
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 暴露Authorization头（前端可获取）
        config.addExposedHeader("Authorization");
        // 缓存CORS配置1小时
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径应用CORS配置
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}