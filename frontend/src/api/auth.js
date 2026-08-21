/**
 * ============================================================================
 * 文件：frontend/src/api/auth.js
 * 作用：认证模块 API 封装
 * ============================================================================
 *
 * 【教学知识点：API 模块化组织】
 *
 * 按业务领域拆分 API 文件，每个文件负责一组相关接口：
 * - auth.js: 认证（登录、注册、登出）
 * - product.js: 商品管理
 * - order.js: 订单管理
 * - ...
 *
 * 这样的好处：
 * 1. 接口集中：同一业务的接口放在一起，便于查找
 * 2. 复用方便：多个组件可以复用同一份 API 定义
 * 3. 易于维护：接口变更只需改一处
 *
 * 【教学知识点：统一 http 封装】
 *
 * 所有 API 都通过 @/utils/http 导出的 axios 实例发起请求：
 * - 自动注入 Token（请求拦截器）
 * - 自动解包 response.data（响应拦截器）
 * - 自动处理 401（跳转登录页）
 * - 自动脱敏错误信息
 *
 * 因此 API 函数只需关心：
 * - HTTP 方法（get/post/put/delete）
 * - URL 路径
 * - 请求参数
 *
 * 【教学知识点：RESTful API 设计】
 *
 * 本项目的接口基本遵循 RESTful 风格：
 * - GET    /resources        获取资源列表
 * - GET    /resources/:id    获取单个资源
 * - POST   /resources        创建资源
 * - PUT    /resources/:id    更新资源
 * - DELETE /resources/:id    删除资源
 *
 * 通过 HTTP 方法区分操作类型，URL 表示资源路径。
 */

/**
 * 引入封装好的 axios 实例。
 *
 * @/ 是 Vite 配置的路径别名，指向 src/ 目录。
 * 等价于 import http from '../../utils/http'，但更简洁。
 */
import http from '@/utils/http'

// ============================================================================
// 用户认证接口
// ============================================================================

/**
 * 用户登录
 * @param {Object} data - { username, password }
 * @returns {Promise} 返回 { token, user }
 *
 * POST /auth/login
 * - 请求体：{ username, password }
 * - 响应：{ token, user }（http 拦截器已解包）
 *
 * 注意：登录接口不需要 Token（因为是获取 Token 的接口）。
 */
export const login = (data) => http.post('/auth/login', data)

/**
 * 用户注册
 * @param {Object} data - 注册信息
 * @returns {Promise}
 *
 * POST /auth/register
 * - 请求体：{ username, password, email, ... }
 * - 响应：注册成功的用户信息
 */
export const register = (data) => http.post('/auth/register', data)

/**
 * 用户登出
 * @returns {Promise}
 *
 * POST /auth/logout
 * - 通知后端注销当前 Token（加入黑名单）
 * - 响应通常为空
 *
 * 此接口需要 Token（请求拦截器会自动注入）。
 */
export const logout = () => http.post('/auth/logout')

/**
 * 获取当前用户信息
 * @returns {Promise} 返回用户对象
 *
 * GET /auth/me
 * - 通过 Token 识别用户身份
 * - 响应：用户完整信息（id、username、role、nickname 等）
 *
 * 常用于：
 * 1. 页面刷新后重新获取用户信息
 * 2. 检查 Token 是否仍然有效
 */
export const getCurrentUser = () => http.get('/auth/me')

/**
 * 根据用户名获取用户
 * @param {string} username
 * @returns {Promise}
 *
 * GET /users/username/:username
 * - 路径参数：username
 * - 响应：用户信息
 *
 * 注意：此接口放在 /users 路径下，属于用户管理范畴，
 * 但与认证相关（如注册时检查用户名是否已存在）。
 */
export const getUserByUsername = (username) => http.get(`/users/username/${username}`)

/**
 * 根据 ID 获取用户
 * @param {number|string} id
 * @returns {Promise}
 *
 * GET /users/:id
 * - 路径参数：用户 ID
 * - 响应：用户信息
 *
 * 模板字符串 `${id}` 用于动态拼接 URL。
 */
export const getUserById = (id) => http.get(`/users/${id}`)

// ============================================================================
// 商户认证接口
// ============================================================================

/**
 * 商户登录
 * @param {Object} data - { username, password }
 * @returns {Promise}
 *
 * POST /merchant/login
 * - 与用户登录类似，但走商户认证流程
 * - 响应：{ token, user }，user.role 为 'MERCHANT'
 *
 * 商户与普通用户共用 users 表，通过 role 区分。
 */
export const merchantLogin = (data) => http.post('/merchant/login', data)

/**
 * 商户注册
 * @param {Object} data - 商户注册信息
 * @returns {Promise}
 *
 * POST /merchant/register
 * - 注册成功后用户的 role 会被设置为 'MERCHANT'
 * - 通常需要额外的商户资质信息（店铺名称、联系方式等）
 */
export const merchantRegister = (data) => http.post('/merchant/register', data)

/**
 * 获取当前商户信息
 * @returns {Promise}
 *
 * GET /merchant/me
 * - 通过 Token 识别商户身份
 * - 响应：商户完整信息（含商户特有字段）
 */
export const getMerchantMe = () => http.get('/merchant/me')

/**
 * 获取商户仪表盘数据
 * @returns {Promise}
 *
 * GET /merchant/dashboard
 * - 响应：商户专属统计数据（销售额、订单数、商品数等）
 * - 用于商家中心首页展示
 */
export const getMerchantDashboard = () => http.get('/merchant/dashboard')

/**
 * 获取商户门店信息
 * @returns {Promise}
 *
 * GET /merchant/store
 * - 响应：门店基本信息（名称、地址、联系方式、logo 等）
 */
export const getMerchantStore = () => http.get('/merchant/store')

/**
 * 更新商户门店信息
 * @param {Object} data - 门店信息
 * @returns {Promise}
 *
 * PUT /merchant/store
 * - 请求体：完整的门店信息
 * - 响应：更新后的门店信息
 */
export const updateMerchantStore = (data) => http.put('/merchant/store', data)

/**
 * 获取商户商品列表
 * @param {Object} params - 查询参数
 * @returns {Promise}
 *
 * GET /merchant/products
 * - 查询参数：{ page, size, keyword, ... }
 * - 响应：分页商品列表（只包含当前商户的商品）
 *
 * axios 的 { params } 配置会自动将对象序列化为 URL 查询字符串：
 *   http.get('/merchant/products', { params: { page: 1, size: 10 } })
 *   实际请求：GET /merchant/products?page=1&size=10
 */
export const getMerchantProducts = (params) => http.get('/merchant/products', { params })

/**
 * 获取商户评论列表
 * @param {Object} params - 查询参数
 * @returns {Promise}
 *
 * GET /merchant/reviews
 * - 响应：当前商户所有商品的评论列表
 * - 用于商家中心评论管理
 */
export const getMerchantReviews = (params) => http.get('/merchant/reviews', { params })

/**
 * 回复商户评论
 * @param {Object} data - { reviewId, reply }
 * @returns {Promise}
 *
 * POST /merchant/reviews/reply
 * - 请求体：{ reviewId: 评论ID, reply: 回复内容 }
 * - 响应：回复成功的评论
 */
export const replyMerchantReview = (data) => http.post('/merchant/reviews/reply', data)

/**
 * 隐藏商户评论
 * @param {number|string} id
 * @returns {Promise}
 *
 * PUT /merchant/reviews/:id/hide
 * - 路径参数：评论 ID
 * - 用于商户屏蔽不当评论
 * - 注意：隐藏后用户端不再显示，但管理员仍可见
 */
export const hideMerchantReview = (id) => http.put(`/merchant/reviews/${id}/hide`)
