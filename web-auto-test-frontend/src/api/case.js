// src/api/case.js 最终修正版
import request from "@/utils/request";

// 1. 查询所有用例（已正确，无需改）
export function getCaseList() {
  return request({
    url: "/cases", // 拼接后：/api/cases ✅
    method: "get",
  });
}

// 2. 新增用例（核心修改：补充/cases前缀）
export function addCase(data) {
  return request({
    url: "/cases/addCase", // 原：/addCase → 改后：/cases/addCase（拼接后/api/cases/addCase ✅）
    method: "post",
    data: data,
  });
}

// 3. 编辑用例（核心修改：补充/cases前缀）
export function updateCase(data) {
  return request({
    url: `/cases/case/${data.id}`, // 原：/case/${data.id} → 改后：/cases/case/${id}（拼接后/api/cases/case/${id} ✅）
    method: "put",
    data: data,
  });
}

// 4. 删除用例（核心修改：补充/cases前缀）
export function deleteCase(id) {
  return request({
    url: `/cases/case/${id}`, // 原：/case/${id} → 改后：/cases/case/${id}（拼接后/api/cases/case/${id} ✅）
    method: "delete",
  });
}

// 5. 运行用例（核心修改：补充/cases前缀）
export function runCase(data) {
  return request({
    url: "/cases/runCase", // 原：/runCase → 改后：/cases/runCase（拼接后/api/cases/runCase ✅）
    method: "post",
    data: data, // 传 { caseId: 123 } 格式 ✅
  });
}
