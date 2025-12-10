import service from "@/utils/request";

/**
 * 获取元素列表
 * GET /api/elements
 * @param {Object} params { pageNum, pageSize, elementName, page, controlType }
 */
export function getElementList(params) {
  return service.get("/api/elements", { params });
}

/**
 * 新增元素
 * POST /api/element/add
 * @param {Object} data { name, locatorType, locatorValue, page, controlType }
 */
export function addElement(data) {
  return service.post("/api/element/add", data);
}

/**
 * 编辑元素
 * PUT /api/element/{id}
 * @param {number|string} id 元素ID
 * @param {Object} data 同 addElement
 */
export function editElement(id, data) {
  return service.put(`/api/element/${id}`, data);
}

/**
 * 删除元素
 * DELETE /api/element/{id}
 * @param {number|string} id 元素ID
 */
export function deleteElement(id) {
  return service.delete(`/api/element/${id}`);
}

/**
 * DOM 解析元素
 * POST /api/element/parse
 * @param {Object} data { url }
 * @returns Promise<{ list: [{ name, locatorType, locatorValue, controlType }] }>
 */
export function parseElement(data) {
  return service.post("/api/element/parse", data);
}

/**
 * 批量导入元素
 * POST /api/element/batchImport
 * @param {Object} data { list: [] } 解析后的元素数组
 */
export function batchImportElement(data) {
  return service.post("/api/element/batchImport", data);
}

export { getElementList as default };
