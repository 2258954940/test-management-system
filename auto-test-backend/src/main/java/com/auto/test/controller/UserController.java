package com.auto.test.controller;

import com.auto.test.common.ApiResponse;
import com.auto.test.entity.User;
import com.auto.test.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户登录与管理接口。
 */
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 登录，校验用户名与密码。
     */
    @PostMapping("/user/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
        try {
            System.out.println("前端传的用户名：" + payload.get("username"));
            System.out.println("前端传的密码：" + payload.get("password"));

            String username = payload.get("username");
            String password = payload.get("password");
            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                return ApiResponse.badRequest("用户名或密码不能为空");
            }
            User user = userService.getByUsername(username);
            if (user == null) {
                return ApiResponse.badRequest("用户名不存在");
            }
            System.out.println("数据库里的明文密码：" + user.getPassword()); // 修正日志备注
            // 明文校验密码（已修改checkPassword逻辑）
            if (!userService.checkPassword(password, user.getPassword())) {
                return ApiResponse.badRequest("密码错误");
            }
            String token = "admin-token-" + System.currentTimeMillis();
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("username", user.getUsername());
            data.put("role", user.getRole());
            System.out.println("后端返回的响应：code=200, data=" + data); // 修正日志打印
            return ApiResponse.success("登录成功", data);
        } catch (Exception ex) {
            return ApiResponse.error("服务器错误: " + ex.getMessage(), null);
        }
    }

    /**
     * 新增：获取当前用户信息（前端登录后会调用此接口）
     */
    @GetMapping("/user/info")
    public ApiResponse<Map<String, Object>> getUserInfo(@RequestHeader("Authorization") String token) {
        try {
            // 毕设演示：简化逻辑，从token中提取用户名（实际生产需解析token）
            if (!StringUtils.hasText(token) || !token.startsWith("admin-token-")) {
                return ApiResponse.unauthorized("token无效");
            }
            // 假设token中包含用户名（演示用，实际需用JWT解析）
            String username = "admin"; // 可改为从token解析，或从数据库查
            User user = userService.getByUsername(username);
            Map<String, Object> data = new HashMap<>();
            data.put("username", user.getUsername());
            data.put("role", user.getRole());
            data.put("token", token);
            return ApiResponse.success(data);
        } catch (Exception ex) {
            return ApiResponse.error("获取用户信息失败: " + ex.getMessage(), null);
        }
    }

    /**
     * 新增：退出登录（前端调用）
     */
    @PostMapping("/user/logout")
    public ApiResponse<String> logout() {
        try {
            // 毕设演示：仅返回成功，生产环境需清理服务端token
            return ApiResponse.success("退出成功", "ok");
        } catch (Exception ex) {
            return ApiResponse.error("退出失败: " + ex.getMessage(), null);
        }
    }

    /**
     * 用户列表（仅 admin 允许）。
     */
    @GetMapping("/system/user/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestHeader(value = "X-Role", required = false) String roleHeader
    ) {
        try {
            if (!isAdmin(roleHeader)) {
                return ApiResponse.forbidden("无权限");
            }
            Page<User> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(User::getCreateTime);
            Page<User> resultPage = userService.page(page, wrapper);
            Map<String, Object> data = new HashMap<>();
            data.put("list", resultPage.getRecords());
            data.put("total", resultPage.getTotal());
            return ApiResponse.success(data);
        } catch (Exception ex) {
            return ApiResponse.error("服务器错误: " + ex.getMessage(), null);
        }
    }

    /**
     * 新增用户（仅 admin 允许）。
     */
    @PostMapping("/system/user/add")
    public ApiResponse<String> addUser(@RequestBody User payload,
                                       @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        try {
            if (!isAdmin(roleHeader)) {
                return ApiResponse.forbidden("无权限");
            }
            if (!StringUtils.hasText(payload.getUsername()) || !StringUtils.hasText(payload.getPassword())) {
                return ApiResponse.badRequest("用户名或密码不能为空");
            }
            User existing = userService.getByUsername(payload.getUsername());
            if (existing != null) {
                return ApiResponse.badRequest("用户名已存在");
            }
            // 毕设演示环境：暂存明文密码，生产环境需恢复加密逻辑
            payload.setPassword(payload.getPassword());
            if (!StringUtils.hasText(payload.getRole())) {
                payload.setRole("user");
            }
            if (payload.getStatus() == null) {
                payload.setStatus(1);
            }
            boolean saved = userService.save(payload);
            return saved ? ApiResponse.success("新增成功", "ok") : ApiResponse.error("保存失败", null);
        } catch (Exception ex) {
            return ApiResponse.error("服务器错误: " + ex.getMessage(), null);
        }
    }

    /**
     * 编辑用户（仅 admin 允许）。
     */
    @PutMapping("/system/user/{id}")
    public ApiResponse<String> updateUser(@PathVariable Long id,
                                          @RequestBody User payload,
                                          @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        try {
            if (!isAdmin(roleHeader)) {
                return ApiResponse.forbidden("无权限");
            }
            User existing = userService.getById(id);
            if (existing == null) {
                return ApiResponse.badRequest("用户不存在");
            }
            if (StringUtils.hasText(payload.getUsername())) {
                existing.setUsername(payload.getUsername());
            }
            if (StringUtils.hasText(payload.getRole())) {
                existing.setRole(payload.getRole());
            }
            if (payload.getStatus() != null) {
                existing.setStatus(payload.getStatus());
            }
            if (StringUtils.hasText(payload.getPassword())) {
                // 毕设演示环境：暂存明文密码
                existing.setPassword(payload.getPassword());
            }
            boolean ok = userService.updateById(existing);
            return ok ? ApiResponse.success("更新成功", "ok") : ApiResponse.error("更新失败", null);
        } catch (Exception ex) {
            return ApiResponse.error("服务器错误: " + ex.getMessage(), null);
        }
    }

    /**
     * 删除用户（仅 admin 允许）。
     */
    @DeleteMapping("/system/user/{id}")
    public ApiResponse<String> deleteUser(@PathVariable Long id,
                                          @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        try {
            if (!isAdmin(roleHeader)) {
                return ApiResponse.forbidden("无权限");
            }
            boolean ok = userService.removeById(id);
            return ok ? ApiResponse.success("删除成功", "ok") : ApiResponse.error("删除失败", null);
        } catch (Exception ex) {
            return ApiResponse.error("服务器错误: " + ex.getMessage(), null);
        }
    }

    private boolean isAdmin(String roleHeader) {
        return "admin".equalsIgnoreCase(roleHeader);
    }
}