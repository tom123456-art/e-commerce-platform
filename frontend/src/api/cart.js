/**
 * 购物车模块 API
 * 封装 /cart 相关接口，所有方法返回 Promise（http 拦截器已解包 Result.data）。
 */
import http from '@/utils/http'

export const getCart = () => http.get('/cart')                              // 获取列表
export const addToCart = (data) => http.post('/cart/items', data)            // { productId, quantity }
export const updateCartItem = (itemId, data) => http.put(`/cart/items/${itemId}`, data)   // 修改数量
export const removeCartItem = (itemId) => http.delete(`/cart/items/${itemId}`)            // 移除商品
export const clearCart = () => http.delete('/cart')                                       // 清空购物车
export const checkout = (data) => http.post('/cart/checkout', data)                       // 结算下单
