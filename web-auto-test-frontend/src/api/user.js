import service from "@/utils/request";

/**
 * 用户登录接口
 * - 功能：发送用户名和密码到后端进行认证，后端返回 token 及用户信息
 * - 请求：POST /user/login（不带/api，避免与 baseURL=/api 重复）
 * @param {Object} data - 登录参数
 * @param {string} data.username - 用户名
 * @param {string} data.password - 密码
 * @returns {Promise<Object>} 后端返回的完整响应，包含code/msg/data
 */
export const login = (data) => {
  return service.post("/user/login", data).then((res) => {
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
 * - 请求：GET /user/info（不带/api，避免重复）
 * @returns {Promise<Object>} 后端返回的 data 字段
 */
export const getUserInfo = () => {
  return service.get("/user/info");
};

/**
 * 退出登录接口
 * - 功能：通知后端退出登录并清理服务端会话，同时前端清除本地 token
 * - 请求：POST /user/logout（不带/api，避免重复）
 * @returns {Promise<Object>} 后端返回的结果信息
 */
export const logout = () => {
  return service.post("/user/logout").then((res) => {
    // 退出后清除本地存储
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    localStorage.removeItem("role");
    return res;
  });
};
