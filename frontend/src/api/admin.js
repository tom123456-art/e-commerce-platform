/**
 * 管理后台模块 API —— 封装 /admin 相关接口
 *
 * 【设计原则】
 *   - 每个方法对应一个后端接口，方法名语义化
 *   - 返回 Promise，组件用 await 或 .then() 消费
 *   - 统一使用 http 实例（已配置 baseURL 和拦截器）
 */
import http from '@/utils/http'

/** 获取仪表盘数据（JSON 格式） */
export const getDashboard = () => http.get('/admin/dashboard')

/** 获取仪表盘可视化数据（HTML 格式，用于 iframe 嵌入） */
export const getDashboardVisualization = () => http.get('/admin/dashboard/visualization')

/** 获取展示策略配置（详见 10-管理后台-运营看板.md） */
export const getShowcaseStrategy = () => http.get('/admin/showcase-strategy')

/** 更新展示策略配置 */
export const updateShowcaseStrategy = (data) => http.put('/admin/showcase-strategy', data)

/** 自动调优展示策略 */
export const autoTuneShowcase = () => http.post('/admin/showcase-strategy/auto-tune')
