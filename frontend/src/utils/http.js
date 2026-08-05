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

// 请求拦截器：自动添加 Token
http.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
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
        router.push('/login')
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