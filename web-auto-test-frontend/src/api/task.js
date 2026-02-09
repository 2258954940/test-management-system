// web-auto-test-frontend\src\api\task.js
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
 * 创建任务（核心修改：适配后端字段）
 * POST /api/task/create
 * @param {Object} data { taskName, caseIds, executeType, cronExpression }
 */
export function createTask(data) {
  return service.post("/api/task/create", {
    taskName: data.taskName, // 对应前端taskForm.taskName、后端TaskCreateDTO的taskName
    caseIds: data.caseIds, // 逗号分隔的用例ID字符串（前端传过来的）
    executeType: data.executeType, // 执行方式：immediate/timing（替换原mode）
    cronExpression: data.cronExpression, // 新增：Cron表达式（vue3-cron选择的）
    // 删除原date/time字段（后端已不用）
  });
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

// ========== 新增：删除任务接口（修改为POST方法，匹配后端） ==========
/**
 * 删除任务
 * POST /api/task/delete/{id}
 * @param {number|string} id 任务ID
 */
export function deleteTask(id) {
  return service.post(`/api/task/delete/${id}`);
}

// 导出默认接口（保持原有逻辑不变）
export default getTaskList;
