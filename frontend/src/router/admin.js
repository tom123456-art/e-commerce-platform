/**
 * 管理后台路由配置 —— 独立于用户端路由（router/index.js）。
 *
 * 【双前端架构】
 *   用户端（端口 3000）与管理后台（端口 3001）是两个独立的 Vue 应用：
 *   - 共享同一个后端 API（端口 8080）
 *   - 各自有独立的路由、入口组件（App.vue vs AdminApp.vue）、构建配置
 *   - 权限隔离：管理后台的页面普通用户无法访问
 *
 * 【嵌套路由】
 *   AdminLayout 作为父组件，提供侧边栏 + 顶部栏 + <router-view />
 *   仪表盘、用户管理、商品管理等子页面在 <router-view /> 中渲染
 */
import { createRouter, createWebHistory } from 'vue-router'
import { getCurrentUser, isAdmin, isLoggedIn, saveAuth } from '../utils/auth'

const routes = [
  // 登录页（独立，不在 AdminLayout 内）
  {
    path: '/login',
    name: 'admin-login',
    component: () => import('../views/LoginView.vue'),
    meta: { guestOnly: true, adminLogin: true }  // adminLogin 标记用于显示"管理后台登录"标题
  },

  // 管理后台主页面（嵌套路由）
  {
    path: '/',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },  // 父路由 meta 会被子路由继承
    children: [
      // 仪表盘（默认子路由，path: '' 匹配父路径 '/'）
      {
        path: '',
        name: 'admin-dashboard',
        component: () => import('../views/admin/AdminDashboardView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      // 用户管理
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('../views/admin/AdminUsersView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      // 新增商品（注意：静态路径 products/new 必须在动态路径 products/:id/edit 之前！）
      {
        path: 'products/new',
        name: 'admin-product-create',
        component: () => import('../views/admin/AdminProductEditorView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      // 编辑商品（:id 是动态路由参数）
      {
        path: 'products/:id/edit',
        name: 'admin-product-edit',
        component: () => import('../views/admin/AdminProductEditorView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      // 商品列表
      {
        path: 'products',
        name: 'admin-products',
        component: () => import('../views/admin/AdminProductsView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      // 订单管理
      {
        path: 'orders',
        name: 'admin-orders',
        component: () => import('../views/admin/AdminOrdersView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },{
        path: 'showcase-strategy',
        name: 'ShowcaseStrategy',
        component: () => import('../views/admin/AdminShowcaseStrategyView.vue'),
        meta: { title: '推荐策略' }
      }
    ]
  },

  // 404 兜底：未匹配的路径重定向到首页（必须放在最后）
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 路由守卫 —— 在每次导航前执行权限校验。
 *
 * 【重要安全提示】
 *   前端路由守卫只是用户体验优化，不能作为真正的权限控制！
 *   真正的权限校验在后端 API（Spring Security 路径级配置）。
 *   即使绕过前端守卫，后端也会返回 403。
 */
router.beforeEach((to, from, next) => {
  // 处理 URL 参数中的 Token（从用户端跳转时携带，如 http://localhost:3001/?token=xxx）
  const urlToken = to.query.token
  if (urlToken) {
    import('../utils/http').then(({ default: http }) => {
      http.get('/auth/me', { headers: { Authorization: `Bearer ${urlToken}` } })
        .then(res => {
          const payload = res.data
          const user = payload?.data || payload
          // 验证用户是否是管理员
          if (user && isAdmin(user)) {
            saveAuth({ token: urlToken, user })
          }
          // 清除 URL 中的 token 参数（避免泄露到浏览器历史记录）
          const { token: _, ...queryWithoutToken } = to.query
          next({ path: to.path, query: queryWithoutToken })
        })
        .catch(() => {
          const { token: _, ...queryWithoutToken } = to.query
          next({ path: '/login', query: queryWithoutToken })
        })
    })
    return  // 注意：这里 return，下面的代码不会执行
  }

  const loggedIn = isLoggedIn()
  const currentUser = getCurrentUser()

  // 规则 1：已登录的管理员访问登录页 → 重定向到首页
  if (to.meta.guestOnly && loggedIn && isAdmin(currentUser)) {
    next('/')
    return
  }

  // 规则 2：需要登录但未登录 → 跳转登录页
  if (to.meta.requiresAuth && !loggedIn) {
    next('/login')
    return
  }

  // 规则 3：需要管理员权限但非管理员 → 跳转登录页
  if (to.meta.requiresAdmin && !isAdmin(currentUser)) {
    next('/login')
    return
  }

  // 规则 4：放行
  next()
})

export default router
