/**
 * Token 管理工具
 *
 * 使用 localStorage 存储 token 和用户信息
 */

const TOKEN_KEY = 'ecommerce_token'
const USER_KEY = 'ecommerce_user'

/**
 * 获取 Token
 * @returns {string|null} Token 字符串
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 设置 Token
 * @param {string} token Token 字符串
 */
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 移除 Token
 */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 获取用户信息
 * @returns {object|null} 用户对象
 */
export function getUser() {
  const user = localStorage.getItem(USER_KEY)
  return user ? JSON.parse(user) : null
}

/**
 * 设置用户信息
 * @param {object} user 用户对象
 */
export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

/**
 * 移除用户信息
 */
export function removeUser() {
  localStorage.removeItem(USER_KEY)
}

/**
 * 是否已经登录，token如果存在则认为已登录
 * @returns {boolean} 是否已登录
 */
export function isLoggedIn() {
  return !!getToken()
}

/**
 * 保存认证信息
 * @param {object} data 包含 token 和 user 的对象
 */
export function saveAuth(data){
  setToken(data.token)
  setUser(data.user)
}

/**
 * 清除认证信息
 */
export function clearAuth(){
  removeToken()
  removeUser()
}
/**
 * 获取当前登录用户,从localStorage中获取用户信息
 * @returns {object|null} 用户对象
 */
export function getCurrentUser(){
  return getUser()
}

/**
 * 判断当前用户是否是管理员
 * @returns {boolean} 是否是管理员
 */
export function isAdmin(user) {
  return (user?.role || '').toUpperCase() === 'ADMIN'
}

/**
 * 判断当前用户是否是商家
 * @returns {boolean} 是否是商家
 */
export function isMerchant(user) {
  return (user?.role || '').toUpperCase() === 'MERCHANT'
}
