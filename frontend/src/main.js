import { createApp } from 'vue'
// Vue3 UI组件库
import ElementPlus from 'element-plus'
// Pinia状态管理库的工厂函数
import { createPinia } from 'pinia'
// 引入Element Plus的样式文件
import 'element-plus/dist/index.css'
import App from './App.vue'
// 引入路由实例
import router from './router'
// 全局样式
import './style.css'

//createApp(App).mount('#app')

const app=createApp(App)
app.use(createPinia())
app.use(ElementPlus)
app.use(router)
app.mount('#app')