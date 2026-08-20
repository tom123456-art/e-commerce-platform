/**
 * ============================================================================
 * 文件：frontend/src/main.js
 * 作用：应用入口文件，初始化 Vue 应用并挂载到 DOM
 * ============================================================================
 *
 * 【教学知识点：应用入口文件】
 *
 * main.js 是前端应用的入口文件：
 * 1. 导入 Vue 核心库和根组件
 * 2. 创建 Vue 应用实例
 * 3. 注册插件（如路由、状态管理、UI 框架）
 * 4. 将应用挂载到 DOM 元素
 *
 * 【教学知识点：Vite 构建工具】
 *
 * Vite 是下一代前端构建工具：
 * - 开发环境：使用原生 ES Module，无需打包，启动极快
 * - 生产环境：使用 Rollup 打包，生成优化的静态资源
 *
 * 【教学知识点：import.meta.env 环境变量】
 *
 * import.meta.env 是 Vite 提供的环境变量访问方式：
 * - VITE_APP_VARIANT: 应用变体（'admin' 或其他）
 *
 * 环境变量定义在 .env 文件中：
 * VITE_APP_VARIANT=admin
 *
 * 只有以 VITE_ 开头的变量才会暴露给客户端代码
 */

// ============================================================================
// 导入依赖
// ============================================================================

/**
 * 【教学知识点：Vue 3 createApp】
 *
 * createApp(rootComponent) 创建一个新的 Vue 应用实例：
 * - rootComponent: 根组件，通常是 App.vue
 * - 返回应用实例，可以链式调用 .use()、.mount() 等方法
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'

// 根组件与路由改为按变体动态导入，避免 Shop 变体加载 Admin/Merchant 代码（修复 ERR_ABORTED）
import './style.css'

// ============================================================================
// 应用变体配置
// ============================================================================

/**
 * 【教学知识点：应用变体（Application Variant）】
 *
 * 本项目使用"单代码库，多入口"的架构：
 * - 同一套源代码
 * - 根据环境变量选择不同的根组件和路由
 * - 构建出两个独立的前端应用
 *
 * 根据 VITE_APP_VARIANT 环境变量决定使用哪个组件和路由：
 * - 'admin': 使用 AdminApp + adminRouter
 * - 其他值: 使用 App + router
 */
const variant = import.meta.env.VITE_APP_VARIANT
const isAdminApp = variant === 'admin'
// const isMerchantApp = variant === 'merchant'  // 商户端暂未实现，后续补充时取消注释

// ============================================================================
// 页面标题设置
// ============================================================================
// document.title = isMerchantApp ? '优选商城商家中心' : (isAdminApp ? '优选商城管理后台' : '优选商城')  // 商户端标题暂未实现
document.title = isAdminApp ? '优选商城管理后台' : '优选商城'

// ============================================================================
// 应用初始化（按变体动态加载根组件与路由）
// ============================================================================

/**
 * 【教学知识点：动态导入与代码分割】
 *
 * 根据应用变体动态 import 对应的根组件与路由：
 * - Shop 变体只加载 App.vue + router，不会请求 MerchantApp/AdminApp
 * - Admin/Merchant 变体按需加载各自的组件、路由与 Element Plus
 * - 动态 import() 返回 Promise，支持代码分割，减小首屏体积
 */
async function bootstrap() {
  let rootComponent
  let activeRouter

  // ===== 商户端（Merchant）代码暂未实现，后续补充时取消下面注释 =====
  if (isMerchantApp) {
    const [{ default: MerchantApp }, { default: merchantRouter }] = await Promise.all([
      import('./MerchantApp.vue'),
      import('./router/merchant')
    ])
    rootComponent = MerchantApp
    activeRouter = merchantRouter
  } else if (isAdminApp) {
  // if (isAdminApp) {
    const [{ default: AdminApp }, { default: adminRouter }] = await Promise.all([
      import('./AdminApp.vue'),
      import('./router/admin')
    ])
    rootComponent = AdminApp
    activeRouter = adminRouter
  } else {
    const [{ default: App }, { default: router }] = await Promise.all([
      import('./App.vue'),
      import('./router')
    ])
    rootComponent = App
    activeRouter = router
  }

  const app = createApp(rootComponent)
    .use(createPinia())
    .use(activeRouter)

  // 管理后台/商家中心需先加载 Element Plus 再挂载；用户端直接挂载
  if (isAdminApp) {
    import('element-plus/dist/index.css')
    const { default: ElementPlus } = await import('element-plus')
    app.use(ElementPlus)
    app.mount('#app')
  } else {
    app.mount('#app')
  }
}

bootstrap()