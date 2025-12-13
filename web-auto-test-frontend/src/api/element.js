import service from "@/utils/request";

/**
 * 获取元素列表（对齐后端：/api/element → 原/api/elements）
 * GET /api/element
 * @param {Object} params { pageNum, pageSize, elementName, page, controlType }
 */
export function getElementList(params) {
  // 后端暂时没做分页，先传空params，后续再扩展
  return service.get("/element", { params });
}

/**
 * 新增元素（对齐后端：/api/element → 原/api/element/add）
 * POST /api/element
 * @param {Object} data { elementName, locatorType, locatorValue, pageUrl, widgetType }
 */
export function addElement(data) {
  // 字段映射：前端name→后端elementName，page→pageUrl，controlType→widgetType
  const reqData = {
    elementName: data.name,
    locatorType: data.locatorType,
    locatorValue: data.locatorValue,
    pageUrl: data.page,
    widgetType: data.controlType,
    createBy: "admin", // 固定创建人
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
  // 字段映射和新增一致
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

/**
 * DOM 解析元素（保留，后续对接后端）
 * POST /api/element/parse
 * @param {Object} data { url }
 * @returns Promise<{ list: [{ name, locatorType, locatorValue, controlType }] }>
 */
export function parseElement(data) {
  return service.post("/element/parse", data);
}

/**
 * 批量导入元素（保留，后续对接后端）
 * POST /api/element/batchImport
 * @param {Object} data { list: [] } 解析后的元素数组
 */
export function batchImportElement(data) {
  // 字段映射后传给后端
  const reqList = data.list.map((item) => ({
    elementName: item.name,
    locatorType: item.locatorType,
    locatorValue: item.locatorValue,
    pageUrl: "", // 批量导入暂时默认空页面
    widgetType: item.controlType,
    createBy: "admin",
  }));
  return service.post("/element/batchImport", { list: reqList });
}

// 保留默认导出
export { getElementList as default };
