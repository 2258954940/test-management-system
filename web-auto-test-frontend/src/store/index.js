import { defineStore } from "pinia";

// 用户状态管理：username, role, token
// token 从 localStorage 持久化，login/logout 方法供全局调用
export const useUserStore = defineStore("user", {
  state: () => ({
    username: "",
    role: "",
    token: localStorage.getItem("token") || "",
  }),
  actions: {
    login(userInfo) {
      this.username = userInfo.username || "";
      this.role = userInfo.role || "";
      this.token = userInfo.token || "";
      if (this.token) {
        localStorage.setItem("token", this.token);
      }
      if (this.role) {
        localStorage.setItem("role", this.role);
      }
      if (this.username) {
        localStorage.setItem("username", this.username);
      }
    },
    loadFromLocal() {
      this.token = localStorage.getItem("token") || this.token || "";
      this.role = localStorage.getItem("role") || this.role || "";
      this.username = localStorage.getItem("username") || this.username || "";
    },
    logout() {
      this.username = "";
      this.role = "";
      this.token = "";
      // 清除本地与会话存储的用户信息，避免下次携带旧数据
      ["token", "role", "username", "userInfo"].forEach((k) => {
        try {
          localStorage.removeItem(k);
        } catch (e) {
          // eslint-disable-next-line no-unused-vars
          // 忽略localStorage清理异常（如浏览器禁用存储）
        }
        try {
          sessionStorage.removeItem(k);
        } catch (e) {
          // eslint-disable-next-line no-unused-vars
          // 忽略sessionStorage清理异常（如浏览器禁用存储）
        }
      });
    },
  },
});

export const useLoadingStore = defineStore("loading", {
  state: () => ({
    visible: false,
    text: "加载中...",
  }),
  actions: {
    show(text = "加载中...") {
      this.text = text || "加载中...";
      this.visible = true;
    },
    hide() {
      this.visible = false;
      this.text = "加载中...";
    },
  },
});
