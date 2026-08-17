// defineConfig 是 Vite 提供的辅助函数，作用是让编辑器对配置项有「智能提示」
import { defineConfig } from 'vite'
// vue 插件：让 Vite 能识别和编译 .vue 单文件组件
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // define：在「编译时」把代码里的某个表达式替换成固定值
  // 这里把 import.meta.env.VITE_APP_VARIANT 替换成字符串 'shop'，
  // 业务代码就能靠它判断「当前是用户端还是管理端」，从而加载不同的路由/布局
  define: {
    'import.meta.env.VITE_APP_VARIANT': JSON.stringify('shop')
  },
  // plugins：注册 Vite 插件，vue() 是开发 Vue 项目必装的插件
  plugins: [vue()],
  // server：开发服务器（npm run dev 时生效）的配置
  server: {
    port: 3000,   // 用户端开发服务器运行在 3000 端口
    // proxy：开发环境的「反向代理」，是解决跨域问题的关键
    // 浏览器有「同源策略」：前端在 3000 端口，直接请求 8080 的后端会被拦截（跨域）。
    // 配置 proxy 后，前端请求 /api/xxx 会先发给 Vite 服务器（同源，不跨域），
    // 再由 Vite 转发给后端 8080，绕过了浏览器的跨域限制。
    proxy: {
      '/api': {                       // 所有以 /api 开头的请求都会被代理
        target: 'http://localhost:9090', // 转发的目标地址（后端服务）
        changeOrigin: true,           // 把请求头里的 Origin 改成目标地址，避免后端校验来源失败
        secure: true                  // 校验 HTTPS 证书（目标是 http 时此项影响不大）
      }
    }
  }
})