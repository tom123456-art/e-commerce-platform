import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
// API 层：与后端 /auth/* 接口对接
import { login as loginApi, register as registerApi,
         logout as logoutApi, getCurrentUser } from '../api/auth'
// 工具层：封装 localStorage 读写（即 04 章的 auth.js）
import { saveAuth, clearAuth, getToken, getCurrentUser as getStoredUser } from '../utils/auth'

/**
 * useAuthStore - 认证状态 Store
 *
 * Store ID 为 'auth'，在 Vue DevTools 中以此 ID 显示。
 * 调用方式：const authStore = useAuthStore()
 */
export const useAuthStore = defineStore('auth', () => {
  // ------------------------------------------------------------------------
  // State：响应式状态（替代 reactive，支持 ref 自动解包）
  // ------------------------------------------------------------------------

  /** Token：从 localStorage 初始化，刷新后仍保持登录态 */
  const token = ref(getToken() || '')
  /** 当前用户信息：对象结构包含 id/username/role 等字段 */
  const user = ref(getStoredUser() || null)

  // ------------------------------------------------------------------------
  // Getters：计算属性（派生状态，依赖变化时自动重算并缓存）
  // ------------------------------------------------------------------------

  /** 是否已登录：token 非空即为 true（!! 双重否定转布尔） */
  const isLoggedIn = computed(() => !!token.value)

  /** 是否为管理员：role 转大写后比较，兼容 'admin'/'Admin'/'ADMIN' */
  const isAdmin = computed(() => (user.value?.role || '').toUpperCase() === 'ADMIN')

  /** 是否为商家：与 isAdmin 对称，商家有独立后台（端口 3002） */
  const isMerchant = computed(() => (user.value?.role || '').toUpperCase() === 'MERCHANT')

  // ------------------------------------------------------------------------
  // Actions：业务方法
  // ------------------------------------------------------------------------

  /**
   * 登录：调用后端接口 -> 更新响应式状态 -> 持久化到 localStorage
   * 失败时 loginApi 抛错，由调用方 try/catch 处理。
   */
  async function login(credentials) {
    const res = await loginApi(credentials)      // 等待后端返回 { token, user }
    token.value = res.token                       // 更新响应式状态，触发视图刷新
    user.value = res.user
    saveAuth({ token: res.token, user: res.user }) // 持久化，刷新后仍保持登录
  }

  /**
   * 注册：只调用后端接口，不自动登录（需用户手动登录）。
   * 因此不修改 token/user 状态，只返回结果。
   */
  async function register(credentials) {
    const res = await registerApi(credentials)
    return res
  }

  /**
   * 登出：通知后端注销 -> 无论后端是否成功都清除本地状态。
   * 用 try/finally 保证前端状态一定被重置，避免"半登录"。
   */
  async function logout() {
    try {
      if (token.value) {
        await logoutApi()  // 通知后端清除 Redis 中的 Token
      }
    } finally {
      token.value = ''
      user.value = null
      clearAuth()  // 清除 localStorage
    }
  }

  /**
   * 刷新用户信息：页面刷新后从后端获取最新用户资料。
   * 用于 Token 存在但 user 对象为空的场景（如首次加载）。
   */
  async function fetchUser() {
    const res = await getCurrentUser()
    user.value = res
  }

  /**
   * 外部登录成功后同步状态（商家端直接走 http 接口登录时使用）：
   * 仅写 localStorage 不会触发响应式更新，导航栏等组件会停留在"未登录"状态；
   * 此方法同时更新响应式 token/user 并持久化。
   */
  function setAuth(data) {
    if (!data || !data.token) return
    token.value = data.token
    user.value = data.user
    saveAuth(data)
  }

  // 返回值：暴露给外部使用的 state/getters/actions
  return { token, user, isLoggedIn, isAdmin, isMerchant, login, register, logout, fetchUser, setAuth }
})
