package com.auto.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // 必须导入HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 安全配置（适配Spring Security 6.x+，无启动报错）。
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
            // 1. CORS配置（跨域）
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 2. 禁用CSRF（前后端分离必做）
            .csrf(AbstractHttpConfigurer::disable)
            // 3. 禁用表单登录/HTTP Basic（JWT场景）
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            // 4. 无状态会话（JWT不需要session）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 5. 权限规则（核心：用HttpMethod指定方法，路径以/开头）
            .authorizeHttpRequests(auth -> auth
                // ✅ 正确写法：HttpMethod.POST + 完整路径（以/开头）
                .requestMatchers(HttpMethod.POST, "/api/user/login").permitAll()
                // ✅ 正确写法：HttpMethod.OPTIONS + 通配路径（以/开头）
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 开发环境临时放行用例/元素接口
                .requestMatchers("/api/case/**").permitAll()
                .requestMatchers("/api/element/**").permitAll()
                // 系统用户接口仅admin可访问
                .requestMatchers("/api/system/user/**").hasAuthority("admin")
                // 其他接口需认证
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 6.x+ 推荐写法：设置允许的源模式
        config.setAllowedOriginPatterns(java.util.Collections.singletonList("*"));
        config.setAllowCredentials(true);
        // 允许所有请求方法
        config.addAllowedMethod(CorsConfiguration.ALL);
        // 允许所有请求头
        config.addAllowedHeader(CorsConfiguration.ALL);
        // 暴露Authorization头（前端可获取）
        config.addExposedHeader("Authorization");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}