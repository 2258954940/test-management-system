import service from "@/utils/request";

/**
 * 获取用例列表
 * GET /api/cases
 * @param {Object} params { pageNum, pageSize, caseName, module, status }
 * @returns Promise<{ list: [], total: number }>
 */
export function getCaseList(params) {
  return service.get("/cases", { params });
}

/**
 * 新增用例
 * POST /api/addTestCase
 * @param {Object} data { name, module, url, locatorType, locatorValue, actionType, inputData, expectedResult }
 */
export function addCase(data) {
  return service.post("/api/addTestCase", data);
}

/**
 * 编辑用例
 * PUT /api/case/{id}
 * @param {number|string} id 用例ID
 * @param {Object} data 同 addCase
 */
export function editCase(id, data) {
  return service.put(`/api/case/${id}`, data);
}

/**
 * 删除用例
 * DELETE /api/case/{id}
 * @param {number|string} id 用例ID
 */
export function deleteCase(id) {
  return service.delete(`/api/case/${id}`);
}

/**
 * 运行单个用例
 * POST /api/runCase
 * @param {number|string} caseId 用例ID
 * @returns Promise<{ status, message, durationMs, screenshotPath }>
 */
export function runCase(caseId) {
  return service.post("/api/runCase", { caseId });
}

export { getCaseList as default };
