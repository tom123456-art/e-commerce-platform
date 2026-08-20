/**
 * 商家后台路由配置
 *
 * 定义商家后台的所有页面路由，包括：
 * - 登录/注册页（仅访客可访问）
 * - Dashboard、商品管理、评论管理、店铺信息等（需登录）
 * - 路由守卫：未登录重定向到登录页，已登录访问登录页重定向到 Dashboard
 */

import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getCurrentUser, isMerchant, saveAuth } from '../utils/auth'

const routes = [
  // 登录页：仅未登录用户可访问，已登录用户重定向到 Dashboard
  {
    path: '/login',
    name: 'MerchantLogin',
    component: () => import('../views/merchant/MerchantLoginView.vue'),
    meta: { guestOnly: true }
  },
  // 注册页：仅未登录用户可访问
  {
    path: '/register',
    name: 'MerchantRegister',
    component: () => import('../views/merchant/MerchantRegisterView.vue'),
    meta: { guestOnly: true }
  },
  // 根路径重定向到 Dashboard
  {
    path: '/',
    redirect: '/dashboard'
  },
  // Dashboard 数据看板：需登录
  {
    path: '/dashboard',
    name: 'MerchantDashboard',
    component: () => import('../views/merchant/MerchantDashboardView.vue'),
    meta: { requiresAuth: true }
  },
  // 商品管理列表：需登录
  {
    path: '/products',
    name: 'MerchantProducts',
    component: () => import('../views/merchant/MerchantProductsView.vue'),
    meta: { requiresAuth: true }
  },
  // 新增商品：需登录
  {
    path: '/products/new',
    name: 'MerchantProductNew',
    component: () => import('../views/merchant/MerchantProductEditorView.vue'),
    meta: { requiresAuth: true }
  },
  // 编辑商品：需登录，通过 :id 参数动态加载商品信息
  {
    path: '/products/:id/edit',
    name: 'MerchantProductEdit',
    component: () => import('../views/merchant/MerchantProductEditorView.vue'),
    meta: { requiresAuth: true }
  },
  // 评论管理：需登录
  {
    path: '/reviews',
    name: 'MerchantReviews',
    component: () => import('../views/merchant/MerchantReviewsView.vue'),
    meta: { requiresAuth: true }
  },
  // 店铺信息管理：需登录
  {
    path: '/store',
    name: 'MerchantStore',
    component: () => import('../views/merchant/MerchantStoreView.vue'),
    meta: { requiresAuth: true }
  },
  // 通配符：未知路径重定向到 Dashboard
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

// 创建 Vue Router 实例，使用 HTML5 History 模式
const router = createRouter({
  history: createWebHistory(),
  routes
})

// ============================================================================
// 跨窗口 Token 传递（postMessage API）
// ============================================================================

/**
 * 监听来自用户端（Shop）的 AUTH_TOKEN 消息，实现跨窗口 Token 传递。
 *
 * 应用场景：用户在用户端商城（3000）点击「进入商家中心」时，
 * 用户端会打开商家中心的新窗口（3002），并通过 postMessage 把 Token 传过来，
 * 避免商家重复登录。
 *
 * 握手流程：
 * 1. 商家后台加载完成后，向父窗口（opener）发送 MERCHANT_READY 消息
 * 2. 用户端收到 MERCHANT_READY 后，发送 AUTH_TOKEN 消息
 * 3. 商家后台收到 Token 后，调用后端 API 验证并保存
 */
window.addEventListener('message', (event) => {
  // ★ 安全校验：只接受来自用户端域名的消息
  const shopOrigin = import.meta.env.VITE_SHOP_ORIGIN || 'http://localhost:3000'
  if (event.origin !== shopOrigin) return

  if (event.data?.type === 'AUTH_TOKEN') {
    const token = event.data.token
    if (!token) return

    // 动态导入 http 模块，避免循环依赖
    import('../utils/http').then(({ default: http }) => {
      http.get('/auth/me', { headers: { Authorization: `Bearer ${token}` } })
        .then(res => {
          const payload = res.data
          const user = payload?.data || payload

          // ★ 验证用户是否是商家（防止普通用户 Token 被传到商家中心）
          if (user && isMerchant(user)) {
            saveAuth({ token, user })
            event.source.postMessage({ type: 'AUTH_SUCCESS' }, event.origin)
            router.go(0)     // 刷新页面以触发路由守卫
          }
        })
        .catch(() => {
          // Token 无效，忽略
        })
    })
  }
})

// 页面加载完成后，通知父窗口已就绪（如果有父窗口）
if (window.opener) {
  const shopOrigin = import.meta.env.VITE_SHOP_ORIGIN || 'http://localhost:3000'
  window.opener.postMessage({ type: 'MERCHANT_READY' }, shopOrigin)
}

/**
 * 路由守卫：在每次路由切换前执行身份验证
 * - guestOnly 页面（登录/注册）：已登录用户重定向到 Dashboard
 * - requiresAuth 页面：未登录用户重定向到登录页
 */
router.beforeEach((to, from, next) => {
  const token = getToken()
  const user = getCurrentUser()

  // 访客页面：已登录用户不允许访问，重定向到 Dashboard
  if (to.meta.guestOnly && token && user) {
    next('/dashboard')
    return
  }

  // 受保护页面：未登录用户重定向到登录页
  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }

  // 通过验证，允许导航
  next()
})

export default router
