import axios from "axios";
import { ElMessage } from "element-plus";
import router from "@/router";
import { useUserStore } from "@/store";

// 存储待处理的请求，用于取消重复请求
const pendingRequestMap = new Map();

/**
 * 生成请求唯一标识
 * @param {axios.RequestConfig} config - 请求配置
 * @returns {string} 请求key
 */
function getRequestKey(config) {
  const { method, url, params, data } = config;
  return [
    method?.toLowerCase() || "get",
    url,
    JSON.stringify(params || {}),
    JSON.stringify(data || {}),
  ].join("&");
}

/**
 * 添加待处理请求（重复则取消旧请求）
 * @param {axios.RequestConfig} config - 请求配置
 */
function addPending(config) {
  const key = getRequestKey(config);
  if (pendingRequestMap.has(key)) {
    const controller = pendingRequestMap.get(key);
    controller.abort(); // 取消旧请求
  }
  const controller = new AbortController();
  config.signal = controller.signal;
  config.__pendingKey = key;
  pendingRequestMap.set(key, controller);
}

/**
 * 移除已完成/取消的请求
 * @param {axios.RequestConfig} config - 请求配置
 */
function removePending(config) {
  const key = config?.__pendingKey || getRequestKey(config || {});
  if (pendingRequestMap.has(key)) {
    pendingRequestMap.delete(key);
  }
}

// 创建Axios实例
const service = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL || "/api", // 前端代理前缀
  timeout: 5000, // 请求超时时间
  headers: {
    "Content-Type": "application/json;charset=utf-8", // 默认请求头
  },
});

// 请求拦截器：添加Token、X-Role请求头，取消重复请求
service.interceptors.request.use(
  (config) => {
    // 1. 处理重复请求
    addPending(config);

    // 2. 获取Token和Role（优先Pinia，回退localStorage）
    let token = "";
    let role = "";
    try {
      const userStore = useUserStore();
      if (userStore) {
        token = userStore.token || "";
        role = userStore.role || "";
      }
    } catch (err) {
      // Pinia未初始化时，从localStorage获取
      token = localStorage.getItem("token") || "";
      role = localStorage.getItem("role") || "";
    }

    // 3. 添加Token到Authorization头
    if (token) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
    }

    // 4. 添加X-Role头（供后端校验admin权限）
    if (role) {
      config.headers = config.headers || {};
      config.headers["X-Role"] = role;
    }

    return config;
  },
  (error) => {
    // 请求拦截器异常处理
    ElMessage.error("请求参数异常，请重试");
    return Promise.reject(error);
  }
);

// 响应拦截器：处理后端返回格式，统一错误提示，清理重复请求
service.interceptors.response.use(
  (response) => {
    // 1. 移除已完成的请求
    removePending(response.config);

    // 2. 获取后端返回数据（适配ApiResponse格式）
    const res = response.data;

    // 3. 空数据处理
    if (res === null || res === undefined) {
      ElMessage.error("服务器返回数据格式错误");
      return Promise.reject(new Error("No response data"));
    }

    // 4. 业务错误处理（非200状态码）
    if (res.code !== 200) {
      // 401：Token失效/未登录，跳登录页
      if (res.code === 401) {
        try {
          const userStore = useUserStore();
          if (userStore && typeof userStore.logout === "function") {
            userStore.logout(); // 清空Pinia
          }
        } catch (err) {
          // 清空localStorage
          localStorage.removeItem("token");
          localStorage.removeItem("role");
          localStorage.removeItem("username");
        }
        ElMessage.error(res.msg || "登录已过期，请重新登录");
        router.push("/login");
      } else {
        // 其他业务错误，提示后端返回的msg
        ElMessage.error(res.msg || "请求失败");
      }
      return Promise.reject(res);
    }

    // 5. 成功：返回后端的data字段（简化前端调用）
    return res.data;
  },
  (error) => {
    // 1. 移除已取消/失败的请求
    if (error?.config) {
      removePending(error.config);
    }

    // 2. 处理取消请求的异常（不提示）
    if (error?.name === "CanceledError" || error?.code === "ERR_CANCELED") {
      return Promise.reject(error);
    }

    // 3. 统一网络错误提示
    const status = error?.response?.status;
    const resMsg = error?.response?.data?.msg;

    if (status === 401) {
      // 401：未登录/Token失效
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
      ElMessage.error("登录已过期，请重新登录");
      router.push("/login");
    } else if (status === 403) {
      // 403：无权限
      ElMessage.error(resMsg || "无权限访问该资源");
    } else if (status === 404) {
      // 404：接口不存在
      ElMessage.error("请求的接口不存在");
    } else if (status === 500) {
      // 500：服务器错误
      ElMessage.error(resMsg || "服务器内部错误，请稍后重试");
    } else {
      // 其他网络错误（如跨域、超时）
      ElMessage.error(resMsg || "网络错误，请检查网络后重试");
    }

    return Promise.reject(error);
  }
);

export default service;
