package com.auto.test.controller;

import com.auto.test.common.ApiResponse;
import com.auto.test.entity.User;
import com.auto.test.service.UserService;
import com.auto.test.service.SysLogService; // 新增：导入日志服务
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
    private final SysLogService sysLogService; // 新增：日志服务

    // 构造器注入（添加SysLogService）
    public UserController(UserService userService, SysLogService sysLogService) { // 新增：日志服务参数
        this.userService = userService;
        this.sysLogService = sysLogService; // 新增：赋值
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
            
            // ========== 新增：用户状态校验 ==========
            if (user.getStatus() == null || user.getStatus() != 1) {
                return ApiResponse.badRequest("用户已被禁用，无法登录");
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
            
            // 新增：记录登录日志（可选）
            sysLogService.saveLog(
                username, // 用登录用户名作为操作人
                "用户登录",
                "用户" + username + "登录系统"
            );
            
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
            
            // 新增：记录退出日志（可选）
            sysLogService.saveLog(
                "admin",
                "用户退出",
                "用户admin退出系统"
            );
            
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
     * 新增用户（仅 admin 允许）（添加日志）。
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
            
            // 新增：记录操作日志
            if (saved) {
                sysLogService.saveLog(
                    "admin",
                    "新增用户",
                    "新增用户-" + payload.getUsername() + "（角色：" + payload.getRole() + "）"
                );
            }
            
            return saved ? ApiResponse.success("新增成功", "ok") : ApiResponse.error("保存失败", null);
        } catch (Exception ex) {
            return ApiResponse.error("服务器错误: " + ex.getMessage(), null);
        }
    }

    /**
     * 编辑用户（仅 admin 允许）（添加日志）。
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
            
            // 记录修改前的状态（用于日志）
            String oldUsername = existing.getUsername();
            String oldRole = existing.getRole();
            Integer oldStatus = existing.getStatus();
            
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
            
            // 新增：记录操作日志
            if (ok) {
                // 区分是编辑基本信息还是修改状态
                String operationContent = "";
                if (payload.getStatus() != null && !payload.getStatus().equals(oldStatus)) {
                    operationContent = "修改用户" + oldUsername + "状态为：" + (payload.getStatus() == 1 ? "启用" : "禁用");
                    sysLogService.saveLog("admin", "修改用户状态", operationContent);
                } else {
                    operationContent = "编辑用户-" + oldUsername + "（原角色：" + oldRole + "，新角色：" + existing.getRole() + "）";
                    sysLogService.saveLog("admin", "编辑用户", operationContent);
                }
            }
            
            return ok ? ApiResponse.success("更新成功", "ok") : ApiResponse.error("更新失败", null);
        } catch (Exception ex) {
            return ApiResponse.error("服务器错误: " + ex.getMessage(), null);
        }
    }

    /**
     * 删除用户（仅 admin 允许）（添加日志）。
     */
    @DeleteMapping("/system/user/{id}")
    public ApiResponse<String> deleteUser(@PathVariable Long id,
                                          @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        try {
            if (!isAdmin(roleHeader)) {
                return ApiResponse.forbidden("无权限");
            }
            // 先查询用户信息（用于日志）
            User user = userService.getById(id);
            if (user == null) {
                return ApiResponse.badRequest("用户不存在");
            }
            
            boolean ok = userService.removeById(id);
            
            // 新增：记录操作日志
            if (ok) {
                sysLogService.saveLog(
                    "admin",
                    "删除用户",
                    "删除用户-" + user.getUsername() + "（ID：" + id + "）"
                );
            }
            
            return ok ? ApiResponse.success("删除成功", "ok") : ApiResponse.error("删除失败", null);
        } catch (Exception ex) {
            return ApiResponse.error("服务器错误: " + ex.getMessage(), null);
        }
    }

    private boolean isAdmin(String roleHeader) {
        return "admin".equalsIgnoreCase(roleHeader);
    }
}