import service from "@/utils/request";

/**
 * 获取元素列表（对齐后端：/api/element → 原/api/elements）
 * GET /api/element
 * @param {Object} params { pageNum, pageSize, elementName, page, controlType }
 */
export function getElementList() {
  return service.get("/element");
}

/**
 * 新增元素（对齐后端：/api/element → 原/api/element/add）
 * POST /api/element
 * @param {Object} data { elementName, locatorType, locatorValue, pageUrl, widgetType }
 */
export function addElement(data) {
  const reqData = {
    elementName: data.name,
    locatorType: data.locatorType,
    locatorValue: data.locatorValue,
    pageUrl: data.page,
    widgetType: data.controlType,
    createBy: "admin",
  };
  return service.post("/element", reqData);
}

/**
 * 编辑元素（对齐后端：方法名editElement→updateElement，路径不变）
 * PUT /api/element/{id}
 * @param {number|string} id 元素ID
 * @param {Object} data 同 addElement
 */
export function updateElement(id, data) {
  const reqData = {
    elementName: data.name,
    locatorType: data.locatorType,
    locatorValue: data.locatorValue,
    pageUrl: data.page,
    widgetType: data.controlType,
    createBy: "admin",
  };
  return service.put(`/element/${id}`, reqData);
}

/**
 * 删除元素（路径不变，保留）
 * DELETE /api/element/{id}
 * @param {number|string} id 元素ID
 */
export function deleteElement(id) {
  return service.delete(`/element/${id}`);
}

// ========== 1. 改：解析接口命名+字段适配（和前端调用对齐） ==========
/**
 * DOM 解析元素（对接后端真实接口）
 * POST /api/element/parse
 * @param {Object} data { url }
 */
export function parseUrlElements(data) {
  // 改名：parseElement → parseUrlElements（和前端调用一致）
  return service.post("/element/parse", data);
}

/**
 * 批量导入元素（保留，调整字段映射）
 * POST /api/element/batchImport
 * @param {Object} data { list: [] } 解析后的元素数组
 */
export function batchImportElement(data) {
  // 改：pageUrl赋值为解析的URL（而非空），适配后端
  const reqList = data.list.map((item) => ({
    elementName: item.name, // 前端parseResult的name → 后端elementName
    locatorType: item.locatorType,
    locatorValue: item.locatorValue,
    pageUrl: data.url || "", // 补充：把解析的URL传给pageUrl
    widgetType: item.controlType, // 前端controlType → 后端widgetType
    createBy: "admin",
  }));
  return service.post("/element/batchImport", { list: reqList });
}

export { getElementList as default };
