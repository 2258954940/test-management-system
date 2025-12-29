// src/utils/request.js
import axios from "axios";
import { ElMessage } from "element-plus";
import router from "@/router";
import { useUserStore } from "@/store";

// 创建Axios实例
const service = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL || "http://localhost:8000", // 修复baseURL：直接指向后端/api
  timeout: 30000,
  headers: {
    "Content-Type": "application/json;charset=utf-8",
  },
});

// 请求拦截器：携带Token + 日志
service.interceptors.request.use(
  (config) => {
    console.log("【请求URL】:", config.url);
    const isLoginRequest = config.url.includes("/user/login");
    console.log("【是否登录请求】:", isLoginRequest);

    // 非登录请求，携带Token和Role
    if (!isLoginRequest) {
      const token = localStorage.getItem("token") || "";
      const role = localStorage.getItem("role") || "";

      // 修复请求头：简单赋值，避免AxiosHeaders不可变问题
      if (token) config.headers["Authorization"] = token;
      if (role) config.headers["X-Role"] = role;
    }

    console.log("【最终请求头】:", config.headers);
    return config;
  },
  (error) => {
    ElMessage.error("请求参数异常，请重试");
    return Promise.reject(error);
  }
);

// 响应拦截器：统一处理响应和错误
service.interceptors.response.use(
  (response) => {
    // ########### 新增：优先判断blob类型，直接返回，不做JSON解析 ###########
    if (response.data instanceof Blob) {
      return response.data;
    }
    const res = response.data;
    // 非200状态码统一处理
    if (res.code !== 200) {
      if (res.code === 401) {
        // 登录过期，清理用户信息
        try {
          const userStore = useUserStore();
          if (userStore && typeof userStore.logout === "function") {
            userStore.logout();
          }
        } catch (err) {
          localStorage.removeItem("token");
          localStorage.removeItem("role");
          localStorage.removeItem("username");
        }
        ElMessage.error(res.msg || "登录已过期，请重新登录");
        router.push("/login");
      } else {
        ElMessage.error(res.msg || "请求失败");
      }
      return Promise.reject(res);
    }
    // 200状态码，返回完整响应（包含code/data/msg）
    return res;
  },
  (error) => {
    const status = error?.response?.status;
    const resMsg = error?.response?.data?.msg;

    // 按状态码精准提示
    if (status === 401) {
      try {
        const userStore = useUserStore();
        if (userStore) userStore.logout();
      } catch (err) {
        localStorage.clear();
      }
      ElMessage.error("登录已过期，请重新登录");
      router.push("/login");
    } else if (status === 403) {
      ElMessage.error(resMsg || "无权限访问该资源");
    } else if (status === 404) {
      ElMessage.error("接口不存在，请检查后端接口路径");
    } else if (status === 500) {
      ElMessage.error(resMsg || "服务器内部错误，请稍后重试");
    } else {
      ElMessage.error(resMsg || "网络错误，请检查网络连接");
    }

    return Promise.reject(error);
  }
);

export default service;
