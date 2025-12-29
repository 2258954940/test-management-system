package com.auto.test.common;

import lombok.Data;

/**
 * 统一API响应格式
 */
@Data
public class ApiResponse<T> {
    // 响应码：200成功，400参数错误，401未授权，403禁止访问，500服务器错误
    private int code;
    // 响应信息
    private String msg;
    // 响应数据
    private T data;

    // 私有构造器
    private ApiResponse(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // 成功响应
    public static <T> ApiResponse<T> success(String msg, T data) {
        return new ApiResponse<>(200, msg, data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    // 参数错误（400）
    public static <T> ApiResponse<T> badRequest(String msg) {
        return new ApiResponse<>(400, msg, null);
    }

    // 未授权（401）—— 新增这个方法！
    public static <T> ApiResponse<T> unauthorized(String msg) {
        return new ApiResponse<>(401, msg, null);
    }

    // 禁止访问（403）
    public static <T> ApiResponse<T> forbidden(String msg) {
        return new ApiResponse<>(403, msg, null);
    }

    // 服务器错误（500）
    public static <T> ApiResponse<T> error(String msg, T data) {
        return new ApiResponse<>(500, msg, data);
    }

    public static <T> ApiResponse<T> error(String msg) {
        return new ApiResponse<>(500, msg, null);
    }
}