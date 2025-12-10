import service from "@/utils/request";

/**
 * 用户登录接口
 * - 功能：发送用户名和密码到后端进行认证，后端返回 token 及用户信息
 * - 请求：POST /user/login
 * @param {Object} data - 登录参数
 * @param {string} data.username - 用户名
 * @param {string} data.password - 密码
 * @param {string} [data.role] - 角色（可选，示例：admin/testor）
 * @returns {Promise<Object>} 后端返回的 data 字段，例如 { token, username, role }
 */
export const login = (data) => {
  return service.post("/user/login", data);
};

/**
 * 获取当前登录用户信息
 * - 功能：请求后端获取当前用户信息，依赖请求拦截器自动携带 Authorization Token
 * - 请求：GET /user/info
 * @returns {Promise<Object>} 后端返回的 data 字段，包含用户信息（例如 { username, role }）
 */
export const getUserInfo = () => {
  return service.get("/user/info");
};

/**
 * 退出登录接口
 * - 功能：通知后端退出登录并清理服务端会话（若有），同时前端应清除本地 token
 * - 请求：POST /user/logout
 * @returns {Promise<Object>} 后端返回的结果信息 { code, msg }
 */
export const logout = () => {
  return service.post("/user/logout");
};
