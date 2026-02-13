import service from "@/utils/request";

/**
 * 用户登录接口
 * - 功能：发送用户名和密码到后端进行认证，后端返回 token 及用户信息
 * - 请求：POST /api/user/login（baseURL 不带 /api）
 * @param {Object} data - 登录参数
 * @param {string} data.username - 用户名
 * @param {string} data.password - 密码
 * @returns {Promise<Object>} 后端返回的完整响应，包含code/msg/data
 */
export const login = (data) => {
  return service.post("/api/user/login", data).then((res) => {
    // 登录成功后，提前存储token到localStorage（供请求拦截器使用）
    if (res.code === 200 && res.data?.token) {
      localStorage.setItem("token", res.data.token);
      localStorage.setItem("username", res.data.username);
      localStorage.setItem("role", res.data.role);
    }
    return res;
  });
};

/**
 * 获取当前登录用户信息
 * - 功能：请求后端获取当前用户信息，依赖请求拦截器自动携带 Authorization Token
 * - 请求：GET /api/user/info
 * @returns {Promise<Object>} 后端返回的 data 字段
 */
export const getUserInfo = () => {
  return service.get("/api/user/info");
};

/**
 * 退出登录接口
 * - 功能：通知后端退出登录并清理服务端会话，同时前端清除本地 token
 * - 请求：POST /api/user/logout
 * @returns {Promise<Object>} 后端返回的结果信息
 */
export const logout = () => {
  return service.post("/api/user/logout").then((res) => {
    // 退出后清除本地存储
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    localStorage.removeItem("role");
    return res;
  });
};

// ========== 新增：用户管理相关接口 ==========
/**
 * 查询用户列表（分页）
 * @param {Object} params - 分页/筛选参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 页大小
 */
export const getUserList = (params) => {
  // 携带X-Role请求头（admin权限）
  return service.get("/api/system/user/list", {
    params,
    headers: {
      "X-Role": localStorage.getItem("role") || "admin",
    },
  });
};

/**
 * 新增用户
 * @param {Object} data - 用户信息
 */
export const addUser = (data) => {
  return service.post("/api/system/user/add", data, {
    headers: {
      "X-Role": localStorage.getItem("role") || "admin",
    },
  });
};

/**
 * 编辑用户
 * @param {number} id - 用户ID
 * @param {Object} data - 用户信息
 */
export const updateUser = (id, data) => {
  return service.put(`/api/system/user/${id}`, data, {
    headers: {
      "X-Role": localStorage.getItem("role") || "admin",
    },
  });
};

/**
 * 删除用户
 * @param {number} id - 用户ID
 */
export const deleteUser = (id) => {
  return service.delete(`/api/system/user/${id}`, {
    headers: {
      "X-Role": localStorage.getItem("role") || "admin",
    },
  });
};
/**
 * 修改用户状态（启用/禁用）
 * @param {number} id 用户ID
 * @param {number} status 新状态：1=启用，0=禁用
 */
export const updateUserStatus = (id, status) => {
  return service.put(`/api/system/user/status/${id}`, null, {
    params: { status },
    headers: {
      "X-Role": localStorage.getItem("role") || "admin",
    },
  });
};
