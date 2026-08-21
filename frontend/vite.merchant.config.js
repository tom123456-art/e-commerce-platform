import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'


// 商家端构建配置：与用户端/管理端配置几乎一样，区别在「变体标识、端口、输出目录」三处
export default defineConfig({
  define: {
    // 变体标识设为 'merchant'，业务代码（main.js）据此加载商家端的路由和布局
    'import.meta.env.VITE_APP_VARIANT': JSON.stringify('merchant')
  },
  plugins: [vue()],
   resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 3002,   // 商家端运行在 3002 端口（与用户端 3000、管理端 3001 错开，可同时启动）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: true
      }
    }
  },
  build: {
    // outDir：构建产物（npm run build:merchant）的输出目录
    // 故意改成 dist-merchant，避免和用户端（dist）、管理端（dist-admin）的构建产物互相覆盖
    outDir: 'dist-merchant'
  }
})
