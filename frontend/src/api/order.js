/**
 * 订单模块 API —— 封装 /orders 与 /payment 相关接口
 *
 * 【设计原则】
 *   - 所有方法返回 http (axios) 的 Promise，由调用方处理成功/失败
 *   - URL 路径与后端 @RequestMapping 严格对应
 *   - 复杂参数用对象传递，简单 ID 用路径参数
 */
import http from '@/utils/http'

/** 获取订单列表（管理员看全部，普通用户看自己） */
export const getOrders = (params) => http.get('/orders', { params })

/** 获取订单基本信息 */
export const getOrderById = (id) => http.get(`/orders/${id}`)

/** 获取订单详情（含商品明细） */
export const getOrderDetail = (id) => http.get(`/orders/${id}/detail`)

/** 根据订单号查询 */
export const getOrderByOrderNo = (orderNo) => http.get(`/orders/orderNo/${orderNo}`)

/** 查询某用户的订单 */
export const getOrdersByUserId = (userId) => http.get(`/orders/user/${userId}`)

/** 更新订单（普通用户仅能确认收货：传 { id, targetStatus: 2 }） */
export const updateOrder = (data) => http.put('/orders', data)

/** 删除订单 */
export const deleteOrder = (id) => http.delete(`/orders/${id}`)

/** 创建支付（返回 paymentUrl） */
export const createPayment = (data) => http.post('/payment/create', data)

/** 支付回调（一般由后端直接处理，前端很少调用） */
export const paymentCallback = (data) => http.post('/payment/callback', data)

/** 导出订单 Excel（管理端用） */
export const exportOrders = () => http.get('/excel/exportOrders', { responseType: 'blob' })
