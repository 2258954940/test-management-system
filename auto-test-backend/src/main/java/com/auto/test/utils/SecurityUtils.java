package com.auto.test.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 安全工具类：获取当前登录用户信息
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户名（优先从SecurityContext获取，兜底返回admin）
     */
    public static String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserDetails) {
                    return ((UserDetails) principal).getUsername();
                }
                if (principal instanceof String && !"anonymousUser".equals(principal)) {
                    return (String) principal;
                }
            }
        } catch (Exception e) {
            // 异常时兜底返回admin
            return "admin";
        }
        return "admin";
    }

    /**
     * 从Token解析用户名（兼容你的Token格式：auto-test-token-用户名-xxx）
     */
    public static String getUsernameFromToken(String token) {
        if (token == null || !token.startsWith("auto-test-token-")) {
            return "admin";
        }
        try {
            String[] parts = token.split("-");
            if (parts.length >= 4) {
                return parts[3];
            }
        } catch (Exception e) {
            return "admin";
        }
        return "admin";
    }
}