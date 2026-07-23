import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  define: {
    //定义全局变量VITE_APP_VARIANT,值为shop，用来区分当前是商城前台项目
    'import.meta.env.VITE_APP_VARIANT': JSON.stringify('shop')
  },
  //plugins配置vue需要加载的插件列表
  plugins: [vue()],
  //server服务器的相关配置
  server: {
    //port指定前端开发服务启动端口号为3000，也就是访问地址http://localhost:3000
    port: 3000,
    //匹配所有以api开头的请求路径
    proxy: {
      '/api': {
      //target:代理的目标地址,也是后端SpringBoot的服务地址，所有api请求都会转发到8080后端
      target: 'http://localhost:8080',
      //解决跨域校验
      changeOrigin: true,
      //secure是否校验HTTPS证书
      secure: true
      }
    }
  }
})
