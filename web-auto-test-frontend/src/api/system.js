import service from "@/utils/request";

/**
 * 获取用户列表
 * GET /api/system/user/list
 * @param {Object} params { pageNum, pageSize, username, role, status }
 */
export function getUserList(params) {
  return service.get("/api/system/user/list", { params });
}

/**
 * 新增用户
 * POST /api/system/user/add
 * @param {Object} data { username, password, role, enabled }
 */
export function addUser(data) {
  return service.post("/api/system/user/add", data);
}

/**
 * 编辑用户
 * PUT /api/system/user/{id}
 * @param {number|string} id 用户ID
 * @param {Object} data 同 addUser（password 可选）
 */
export function editUser(id, data) {
  return service.put(`/api/system/user/${id}`, data);
}

/**
 * 删除用户
 * DELETE /api/system/user/{id}
 * @param {number|string} id 用户ID
 */
export function deleteUser(id) {
  return service.delete(`/api/system/user/${id}`);
}

/**
 * 重置用户密码
 * POST /api/system/user/resetPwd/{id}
 * @param {number|string} id 用户ID
 * @returns Promise<{ msg: string }>
 */
export function resetUserPassword(id) {
  return service.post(`/api/system/user/resetPwd/${id}`);
}

/**
 * 获取系统配置
 * GET /api/system/config
 * @returns Promise<{ platformName, timeout, autoCleanLogs }>
 */
export function getSystemConfig() {
  return service.get("/api/system/config");
}

/**
 * 保存系统配置
 * POST /api/system/config/save
 * @param {Object} data { platformName, timeout, autoCleanLogs }
 */
export function saveSystemConfig(data) {
  return service.post("/api/system/config/save", data);
}

/**
 * 获取操作日志
 * GET /api/system/log/list
 * @param {Object} params { pageNum, pageSize, username, opType, timeRange }
 */
export function getOperationLog(params) {
  const payload = { ...params };
  if (payload.timeRange && Array.isArray(payload.timeRange)) {
    payload.startTime = payload.timeRange[0];
    payload.endTime = payload.timeRange[1];
    delete payload.timeRange;
  }
  return service.get("/api/system/log/list", { params: payload });
}

export { getUserList as default };
