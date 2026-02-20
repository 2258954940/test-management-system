package com.auto.test.controller;

import com.auto.test.common.ApiResponse;
import com.auto.test.entity.User;
import com.auto.test.service.UserService;
import com.auto.test.service.SysLogService;
import com.auto.test.utils.SecurityUtils; // 新增：导入工具类
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户登录与管理接口（保证日志记录真实操作人）
 */
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final SysLogService sysLogService;

    // 构造器注入
    public UserController(UserService userService, SysLogService sysLogService) {
        this.userService = userService;
        this.sysLogService = sysLogService;
    }

    /**
     * 登录，生成包含真实用户名的Token
     */
    @PostMapping("/user/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
        try {
            String username = payload.get("username");
            String password = payload.get("password");

            // 基础校验
            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                return ApiResponse.badRequest("用户名或密码不能为空");
            }
            User user = userService.getByUsername(username);
            if (user == null) {
                return ApiResponse.badRequest("用户名不存在");
            }
            if (user.getStatus() == null || user.getStatus() != 1) {
                return ApiResponse.badRequest("用户已被禁用，无法登录");
            }
            if (!userService.checkPassword(password, user.getPassword())) {
                return ApiResponse.badRequest("密码错误");
            }

            // 存入SecurityContext（登录日志用）
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                user.getPassword(),
                java.util.Collections.emptyList()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 生成Token：直接用用户名，不再做Base64
            String token = "auto-test-token-" + user.getUsername() + "-" + System.currentTimeMillis();
            
            // 返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("username", user.getUsername());
            data.put("role", user.getRole());

            // 关键修改：第一个参数传真实用户名（user.getUsername()）
            sysLogService.saveLog(user.getUsername(), "用户登录", "用户" + username + "登录系统");
            return ApiResponse.success("登录成功", data);
        } catch (Exception ex) {
            return ApiResponse.error("登录失败：" + ex.getMessage(), null);
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user/info")
    public ApiResponse<Map<String, Object>> getUserInfo(@RequestHeader("Authorization") String token) {
        try {
            String realUsername = getRealUsernameFromToken(token);
            
            User user = userService.getByUsername(realUsername);
            if (user == null) {
                return ApiResponse.unauthorized("用户不存在");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("username", user.getUsername());
            data.put("role", user.getRole());
            data.put("token", token);
            return ApiResponse.success(data);
        } catch (Exception ex) {
            return ApiResponse.error("获取用户信息失败：" + ex.getMessage(), null);
        }
    }

    /**
     * 退出登录
     */
    @PostMapping("/user/logout")
    public ApiResponse<String> logout(@RequestHeader("Authorization") String token) {
        try {
            String realUsername = getRealUsernameFromToken(token);
            setCurrentUserToSecurityContext(realUsername);
            
            // 关键修改：第一个参数传realUsername
            sysLogService.saveLog(realUsername, "用户退出", "用户" + realUsername + "退出系统");
            return ApiResponse.success("退出成功", "ok");
        } catch (Exception ex) {
            return ApiResponse.error("退出失败：" + ex.getMessage(), null);
        }
    }

    /**
     * 用户列表（无需记录日志）
     */
    @GetMapping("/system/user/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @RequestHeader("Authorization") String token
    ) {
        try {
            String realUsername = getRealUsernameFromToken(token);
            if (!isAdmin(roleHeader)) {
                return ApiResponse.forbidden("仅管理员可查看用户列表");
            }
            
            setCurrentUserToSecurityContext(realUsername);

            Page<User> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(User::getCreateTime);
            Page<User> resultPage = userService.page(page, wrapper);
            
            Map<String, Object> data = new HashMap<>();
            data.put("list", resultPage.getRecords());
            data.put("total", resultPage.getTotal());
            return ApiResponse.success(data);
        } catch (Exception ex) {
            return ApiResponse.error("查询用户列表失败：" + ex.getMessage(), null);
        }
    }

    /**
     * 新增用户
     */
    @PostMapping("/system/user/add")
    public ApiResponse<String> addUser(
            @RequestBody @jakarta.validation.Valid User payload,
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @RequestHeader("Authorization") String token
    ) {
        try {
            String realUsername = getRealUsernameFromToken(token);
            if (!isAdmin(roleHeader)) {
                return ApiResponse.forbidden("仅管理员可新增用户");
            }

            if (!StringUtils.hasText(payload.getUsername()) || !StringUtils.hasText(payload.getPassword())) {
                return ApiResponse.badRequest("用户名或密码不能为空");
            }
            if (payload.getUsername().trim().length() < 2) {
                return ApiResponse.badRequest("用户名长度不能小于2");
            }
            if (userService.getByUsername(payload.getUsername()) != null) {
                return ApiResponse.badRequest("用户名已存在");
            }

            if (!StringUtils.hasText(payload.getRole())) {
                payload.setRole("user");
            }
            if (payload.getStatus() == null) {
                payload.setStatus(1);
            }

            setCurrentUserToSecurityContext(realUsername);
            boolean saved = userService.save(payload);
            
            if (saved) {
                // 关键修改：第一个参数传realUsername
                sysLogService.saveLog(realUsername, "新增用户", 
                    realUsername + "新增用户-" + payload.getUsername() + "（角色：" + payload.getRole() + "）");
            }

            return saved ? ApiResponse.success("新增成功", "ok") : ApiResponse.error("新增失败", null);
        } catch (Exception ex) {
            return ApiResponse.error("新增用户失败：" + ex.getMessage(), null);
        }
    }

    /**
     * 编辑用户/修改状态
     */
    @PutMapping("/system/user/{id}")
    public ApiResponse<String> updateUser(
            @PathVariable Long id,
            @RequestBody User payload,
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @RequestHeader("Authorization") String token
    ) {
        try {
            String realUsername = getRealUsernameFromToken(token);
            if (!isAdmin(roleHeader)) {
                return ApiResponse.forbidden("仅管理员可编辑用户");
            }

            User existing = userService.getById(id);
            if (existing == null) {
                return ApiResponse.badRequest("用户不存在");
            }

            String oldUsername = existing.getUsername();
            String oldRole = existing.getRole();
            Integer oldStatus = existing.getStatus();

            if (StringUtils.hasText(payload.getUsername()) && payload.getUsername().trim().length() >= 2) {
                existing.setUsername(payload.getUsername());
            }
            if (StringUtils.hasText(payload.getRole())) {
                existing.setRole(payload.getRole());
            }
            if (payload.getStatus() != null) {
                existing.setStatus(payload.getStatus());
            }
            if (StringUtils.hasText(payload.getPassword())) {
                existing.setPassword(payload.getPassword());
            }

            setCurrentUserToSecurityContext(realUsername);
            boolean ok = userService.updateById(existing);
            
            if (ok) {
                String operationContent = "";
                if (payload.getStatus() != null && !payload.getStatus().equals(oldStatus)) {
                    operationContent = realUsername + "修改用户" + oldUsername + "状态为：" + 
                        (payload.getStatus() == 1 ? "启用" : "禁用");
                    // 关键修改：第一个参数传realUsername
                    sysLogService.saveLog(realUsername, "修改用户状态", operationContent);
                } else {
                    operationContent = realUsername + "编辑用户-" + oldUsername + 
                        "（原角色：" + oldRole + "，新角色：" + existing.getRole() + "）";
                    // 关键修改：第一个参数传realUsername
                    sysLogService.saveLog(realUsername, "编辑用户", operationContent);
                }
            }

            return ok ? ApiResponse.success("更新成功", "ok") : ApiResponse.error("更新失败", null);
        } catch (Exception ex) {
            return ApiResponse.error("修改用户失败：" + ex.getMessage(), null);
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/system/user/{id}")
    public ApiResponse<String> deleteUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @RequestHeader("Authorization") String token
    ) {
        try {
            String realUsername = getRealUsernameFromToken(token);
            if (!isAdmin(roleHeader)) {
                return ApiResponse.forbidden("仅管理员可删除用户");
            }

            User user = userService.getById(id);
            if (user == null) {
                return ApiResponse.badRequest("用户不存在");
            }

            setCurrentUserToSecurityContext(realUsername);
            boolean ok = userService.removeById(id);
            
            if (ok) {
                // 关键修改：第一个参数传realUsername
                sysLogService.saveLog(realUsername, "删除用户", 
                    realUsername + "删除用户-" + user.getUsername() + "（ID：" + id + "）");
            }

            return ok ? ApiResponse.success("删除成功", "ok") : ApiResponse.error("删除失败", null);
        } catch (Exception ex) {
            return ApiResponse.error("删除用户失败：" + ex.getMessage(), null);
        }
    }

    // ===================== 工具方法（核心）=====================
   /**
 * 从Token解析真实用户名（修复分隔符截取逻辑）
 */
private String getRealUsernameFromToken(String token) {
    try {
        // 第一步：先解码前端传的URL编码Token
        token = java.net.URLDecoder.decode(token, "UTF-8");
    } catch (Exception e) {
        throw new RuntimeException("Token解码失败，请重新登录");
    }

    // 校验Token前缀
    if (!StringUtils.hasText(token) || !token.startsWith("auto-test-token-")) {
        throw new RuntimeException("Token无效，请重新登录");
    }

    // 第二步：拆分Token（格式：auto-test-token-admin123-1771481838993）
    String[] tokenParts = token.split("-");
    // 拆分后数组长度必须是4（索引0:auto-test-token, 1:admin123, 2:时间戳）
    // 注意：split("-") 会把 "auto-test-token" 拆成 ["auto","test","token"]，所以数组长度是 5！
    if (tokenParts.length < 5) {
        throw new RuntimeException("Token格式错误，请重新登录");
    }

    // 正确截取用户名：第4个元素（索引3）
    String realUsername = tokenParts[3];
    // 打印日志（方便你调试，可选）
    System.out.println("解析出的用户名：" + realUsername);

    // 第三步：校验用户名是否存在
    User user = userService.getByUsername(realUsername);
    if (user == null) {
        throw new RuntimeException("操作人不存在，请重新登录：" + realUsername);
    }
    return realUsername;
}

    /**
     * 将真实用户名存入SecurityContext
     */
    private void setCurrentUserToSecurityContext(String username) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            username,
            null,
            java.util.Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 校验是否为管理员
     */
    private boolean isAdmin(String roleHeader) {
        return "admin".equalsIgnoreCase(roleHeader);
    }
}