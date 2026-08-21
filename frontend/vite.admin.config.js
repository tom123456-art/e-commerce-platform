import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'


// 管理端构建配置：与用户端配置几乎一样，区别在于「变体标识、端口、输出目录」三处
export default defineConfig({
  define: {
    // 变体标识设为 'admin'，业务代码据此加载管理端的路由和布局
    'import.meta.env.VITE_APP_VARIANT': JSON.stringify('admin')
  },
  plugins: [vue()],
   resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 3001,   // 管理端运行在 3001 端口（与用户端 3000 错开，可同时启动）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: true
      }
    }
  },
  build: {
    // outDir：构建产物（npm run build:admin）的输出目录
    // 故意改成 dist-admin 而不是默认的 dist，避免和用户端的构建产物互相覆盖
    outDir: 'dist-admin'
  }
})