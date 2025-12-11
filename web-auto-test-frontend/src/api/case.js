// src/api/case.js（还原后，能正常请求的版本）
import request from "@/utils/request";

// 1. 查询所有用例（改回原来能通的URL：/cases，不是/case/list）
export function getCaseList() {
  return request({
    url: "/cases", // 改回没报错前的URL！！！
    method: "get",
    // 去掉params：改后加的params可能导致后端接收参数异常
  });
}

// 2. 新增用例（改回原来能通的URL：/addCase，不是/case/add）
export function addCase(data) {
  return request({
    url: "/addCase", // 改回没报错前的URL！！！
    method: "post",
    data: data,
  });
}

// 3. 编辑用例（改回原来能通的URL：/case/${id}，不是/case/update）
export function updateCase(data) {
  return request({
    url: `/case/${data.id}`, // 改回没报错前的URL！！！
    method: "put",
    data: data,
  });
}

// 4. 删除用例（改回原来能通的逻辑，保持不变即可）
export function deleteCase(id) {
  return request({
    url: `/case/${id}`, // 改回没报错前的URL！！！
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
