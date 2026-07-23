/**
 * Token管理工具
 * 使用localStorage存储token和用户信息
 */
const TOKEN_KEY='ecomerce_token'
const USER_KEY='ecommerce_user'

/**
 * 获取token
 */
export function getToken(){
    return localStorage.getItem(TOKEN_KEY)
}

/**
 * 设置token
*/
export function setToken(token){
    localStorage.setItem(TOKEN_KEY,token)
}

/**
 * 清除token
 */
export function removeToken(){
    localStorage.removeItem(TOKEN_KEY)
}

/**
 * 获取用户信息
 */
export function getUser(){
    const user = localStorage.getItem(USER_KEY)  
    return user ? JSON.parse(user) : null  
}

/**
 * 设置用户信息
 */
export function setUser(user){
    localStorage.setItem(USER_KEY,JSON.stringify(user))
}

/**
 * 清除用户信息
 */
export function removeUser(){
    localStorage.removeItem(USER_KEY)
}