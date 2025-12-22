/* eslint-disable prettier/prettier */
// src/api/siteConfig.js
import request from "@/utils/request";

/**
 * 查询所有网站测试配置（给前端下拉框提供数据）
 * 最终拼接后：baseURL(/api) + url(/cases/siteConfigs) = /api/cases/siteConfigs
 * 和后端 TestCaseController 中的 @GetMapping("/siteConfigs") 完全匹配
 */
export function getSiteConfigs() {
  return request({
    url: "/cases/siteConfigs", // ✅ 正确路径，无需加/api
    method: "get",
  });
}
