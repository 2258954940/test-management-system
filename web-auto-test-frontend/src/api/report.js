import service from "@/utils/request";

/**
 * 获取报告列表
 * GET /api/reports
 * @param {Object} params { pageNum, pageSize, taskId, taskName, timeRange }
 */
export function getReportList(params) {
  const payload = { ...params };
  if (payload.timeRange && Array.isArray(payload.timeRange)) {
    payload.startTime = payload.timeRange[0];
    payload.endTime = payload.timeRange[1];
    delete payload.timeRange;
  }
  return service.get("/api/reports", { params: payload });
}

/**
 * 获取报告统计
 * GET /api/report/stats/{taskId}
 * @param {number|string} taskId 任务ID
 * @returns Promise<{ total, success, failed, rate }>
 */
export function getReportStats(taskId) {
  return service.get(`/api/report/stats/${taskId}`);
}

/**
 * 根据任务ID获取报告详情与统计
 * GET /api/report/task/{taskId}
 * @param {number|string} taskId 任务ID
 * @returns Promise<{ list: [], stats: {} }>
 */
export function getReportByTaskId(taskId) {
  return service.get(`/api/report/task/${taskId}`);
}

export { getReportList as default };
