/**
 * AI 模块 API
 * 封装 /ai 相关接口，统一通过 http 实例发送请求
 */
import http from '@/utils/http'

/**
 * AI 聊天对话
 * @param {Object} data - { message: string, sessionId?: string }
 * @returns {Promise}
 */
export const chat = (data) => http.post('/ai/chat', data)

/**
 * AI 商品搜索
 * @param {Object} data - { query: string }
 * @returns {Promise}
 */
export const search = (data) => http.post('/ai/search', data)

/**
 * AI 商品推荐
 * @param {Object} data - { query: string, budget?: number, categoryPreference?: string }
 * @returns {Promise}
 */
export const recommend = (data) => http.post('/ai/recommend', data)

/**
 * AI 文案生成（商品描述自动生成）
 * @param {Object} data - { productName?, category?, keyFeatures?, style? }
 *   - productName / category / keyFeatures：手动传入的商品信息
 *   - style：文案风格（如"专业"、"活泼"、"简洁"），默认由 Service 层决定
 * @returns {Promise}
 */
export const describe = (data) => http.post('/ai/describe', data)
