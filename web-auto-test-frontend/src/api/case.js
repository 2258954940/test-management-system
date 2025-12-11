// src/api/case.js 修正后：
import request from "@/utils/request";

// 1. 查询所有用例（后端接口：/api/cases）
export function getCaseList() {
  return request({
    url: "/cases", // 最终请求：/api/cases（与后端一致）
    method: "get",
  });
}

// 2. 新增用例（后端接口：/api/addCase）
export function addCase(data) {
  return request({
    url: "/addCase", // 最终请求：/api/addCase（与后端一致）
    method: "post",
    data: data,
  });
}

// 3. 编辑用例（后端接口：/api/case/{id}）
export function updateCase(data) {
  return request({
    url: `/case/${data.id}`, // 最终请求：/api/case/{id}（与后端一致）
    method: "put",
    data: data,
  });
}

// 4. 删除用例（已正确）
export function deleteCase(id) {
  return request({
    url: `/case/${id}`,
    method: "delete",
  });
}

// 运行用例（保留）
export function runCase(data) {
  return request({
    url: "/runCase", // 改回没报错前的URL！！！
    method: "post",
    data: data,
  });
}
