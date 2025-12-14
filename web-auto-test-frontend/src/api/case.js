// src/api/case.js 最终修正版
import request from "@/utils/request";

// 1. 查询所有用例
export function getCaseList() {
  return request({
    url: "/cases",
    method: "get",
  });
}

// 2. 新增用例
export function addCase(data) {
  return request({
    url: "/addCase",
    method: "post",
    data: data,
  });
}

// 3. 编辑用例
export function updateCase(data) {
  return request({
    url: `/case/${data.id}`,
    method: "put",
    data: data,
  });
}

// 4. 删除用例
export function deleteCase(id) {
  return request({
    url: `/case/${id}`,
    method: "delete",
  });
}

// 5. 运行用例（适配后端 /runCase 接口，传 caseId 参数）
export function runCase(data) {
  return request({
    url: "/runCase", // 保持和后端一致的路径
    method: "post",
    data: data, // 传 { caseId: 123 } 格式
  });
}
