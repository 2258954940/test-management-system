const { defineConfig } = require("@vue/cli-service");
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 8080,
    // 强制转发/api开头的请求到后端8000
    proxy: {
      "/api": {
        target: "http://localhost:8000",
        changeOrigin: true, // 模拟后端请求的Origin，解决CORS
        // secure: false,
        // pathRewrite: { "^/api": "/api" }, // 完全保留/api前缀，和Postman一致
      },
    },
    client: {
      overlay: false, // 关闭webpack开发环境的错误覆盖层
    },
  },
});
