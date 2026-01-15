import service from "@/utils/request";

/**
 * 获取任务列表（分页）
 * GET /api/task/list
 * @param {Object} params { pageNum, pageSize, taskName, mode, status }
 */
export function getTaskList(params) {
  return service.get("/api/task/list", { params });
}

/**
 * 获取已完成任务列表（供测试报告下拉框使用）
 * GET /api/task/finished-list
 */
export function getFinishedTasks() {
  return service.get("/api/task/finished-list");
}

/**
 * 创建任务
 * POST /api/task/create
 * @param {Object} data { name, caseIds, mode, date, time }
 */
export function createTask(data) {
  return service.post("/api/task/create", data);
}

/**
 * 运行任务
 * POST /api/task/run/{id}
 * @param {number|string} id 任务ID
 */
export function runTask(id) {
  return service.post(`/api/task/run/${id}`);
}

/**
 * 停止任务
 * POST /api/task/stop/{id}
 * @param {number|string} id 任务ID
 */
export function stopTask(id) {
  return service.post(`/api/task/stop/${id}`);
}

/**
 * 获取任务日志
 * GET /api/task/log/{id}
 * @param {number|string} id 任务ID
 * @returns Promise<{ logs: [] }>
 */
export function getTaskLog(id) {
  return service.get(`/api/task/log/${id}`);
}

// 导出默认接口
export default getTaskList;
