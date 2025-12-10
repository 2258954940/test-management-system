const { defineConfig } = require("@vue/cli-service");

// 按需引入 Element Plus（使用 unplugin 插件），并配置 devServer 代理
// 说明：项目 devDependencies 中已有 `unplugin-auto-import` 与 `unplugin-vue-components`，
// 下面在 webpack 配置中启用对应的 webpack 版本插件，配合 ElementPlusResolver。
// const AutoImport = require("unplugin-auto-import/webpack");
// const Components = require("unplugin-vue-components/webpack");
// const { ElementPlusResolver } = require("unplugin-vue-components/resolvers");

module.exports = defineConfig({
  transpileDependencies: true,
  // configureWebpack: {
  //   plugins: [
  //     // 自动按需导入 Vue / Pinia / Router API 以及 Element Plus 组件的样式与函数
  //     AutoImport({
  //       // 自动导入的库
  //       imports: ['vue', 'vue-router', 'pinia'],
  //       // 为 Element Plus 启用解析器，自动导入组件和样式
  //       resolvers: [ElementPlusResolver()],
  //       // 生成类型声明（可选）
  //       dts: 'src/auto-imports.d.ts',
  //     }),
  //     // 自动注册在模板/JS 中使用到的组件（Element Plus）
  //     Components({
  //       resolvers: [ElementPlusResolver()],
  //       // 生成组件类型声明（可选）
  //       dts: 'src/components.d.ts',
  //     }),
  //   ],
  // },

  // 开发服务器代理配置：将以 /api 开头的请求代理到后端 http://localhost:8080
  // 注意：若前端 devServer 也占用 8080 端口，开发服务器会尝试使用下一个可用端口。
  // 如果你希望前端固定使用其他端口（例如 8081），可以在这里添加 `port: 8081`。
  devServer: {
    proxy: {
      "^/api": {
        target: "http://localhost:8000",
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
