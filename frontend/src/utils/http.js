/**
 * Axios HTTP 请求封装
 *
 * 功能：
 * 1. 自动添加 Token 到请求头
 * 2. 统一处理响应错误
 * 3. 401 自动跳转登录页
 */
import axios from 'axios'
import { getToken, removeToken } from './auth'
import router from '../router'

// 创建 Axios 实例
const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 公开接口（登录/注册等）不需要携带 Token，否则后端鉴权过滤器会因携带
// 过期/无效的 Token 直接返回 401，导致连登录、注册都无法访问。
const PUBLIC_PATHS = [
  '/auth/login',
  '/auth/register',
  '/merchant/login',
  '/merchant/register'
]

// 请求拦截器：自动添加 Token
http.interceptors.request.use(
  (config) => {
    const token = getToken()
    // 公开接口不附加 Authorization 头
    const isPublic = PUBLIC_PATHS.some((p) => (config.url || '').includes(p))
    if (token && !isPublic) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理错误
http.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      const { status } = error.response

      // 401 未授权：清除 Token，跳转登录
      if (status === 401) {
        removeToken()
        // 登录/注册等公开接口本身就是去登录页，不要再跳转，避免覆盖错误提示
        const url = error.config?.url || ''
        const isPublic = PUBLIC_PATHS.some((p) => url.includes(p))
        if (!isPublic) {
          router.push('/login')
        }
      }

      // 403 禁止访问
      if (status === 403) {
        alert('没有权限访问')
      }
    }
    return Promise.reject(error)
  }
)

export default http