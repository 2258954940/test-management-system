// src/utils/request.js
import axios from "axios";
import { ElMessage } from "element-plus";
import router from "@/router";
import { useUserStore } from "@/store";

// ========== 注释掉重复请求拦截（临时） ==========
// const pendingRequestMap = new Map();
// function getRequestKey(config) { ... }
// function addPending(config) { ... }
// function removePending(config) { ... }
// ===============================================

// 创建Axios实例
// src/utils/request.js
const service = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL || "/api", // 加回/api前缀，和Postman一致
  timeout: 30000,
  headers: {
    "Content-Type": "application/json;charset=utf-8",
  },
});

// 请求拦截器：只保留Token逻辑，注释重复请求
// src/utils/request.js —— 仅修改「请求拦截器」这一段，其余代码不动！
// src/utils/request.js —— 请求拦截器中添加日志
service.interceptors.request.use(
  (config) => {
    console.log("请求URL:", config.url);
    const isLoginRequest = config.url.includes("/user/login");
    console.log("是否登录请求:", isLoginRequest);

    if (!isLoginRequest) {
      // 从localStorage取Token（确认有值）
      const token = localStorage.getItem("token") || "";
      const role = localStorage.getItem("role") || "";

      // 🔥 核心修复：用普通对象覆盖headers，彻底绕过AxiosHeaders不可变限制
      config.headers = {
        "Content-Type": "application/json;charset=utf-8", // 保留默认Content-Type
        ...(token ? { Authorization: `Bearer ${token}` } : {}), // 加Token
        ...(role ? { "X-Role": role } : {}), // 加角色
      };
    }

    // 打印最终请求头（此时应该能看到Token）
    console.log("最终请求头:", config.headers);
    return config;
  },
  (error) => {
    ElMessage.error("请求参数异常，请重试");
    return Promise.reject(error);
  }
);
// 响应拦截器：注释重复请求移除逻辑
service.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== 200) {
      if (res.code === 401) {
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
    return res;
  },
  (error) => {
    // if (error?.config) { removePending(error.config); } // 注释掉这行

    if (error?.name === "CanceledError" || error?.code === "ERR_CANCELED") {
      return Promise.reject(error);
    }

    const status = error?.response?.status;
    const resMsg = error?.response?.data?.msg;

    // 重点：区分真实403和其他错误，避免误提示
    if (status === 401) {
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
      ElMessage.error(resMsg || "无权限访问该资源");
    } else if (status === 404) {
      // 404单独提示：接口路径错误（不是无权限）
      ElMessage.error("接口路径错误，请检查后端接口是否存在");
    } else if (status === 500) {
      ElMessage.error(resMsg || "服务器内部错误，请稍后重试");
    } else {
      ElMessage.error(resMsg || "网络错误，请检查网络后重试");
    }

    return Promise.reject(error);
  }
);

export default service;
