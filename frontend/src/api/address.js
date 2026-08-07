/**
 * 地址簿模块 API
 * 封装 /addresses 相关接口，所有方法返回 Promise。
 */
import http from '@/utils/http'

export const getAddresses = () => http.get('/addresses')                              // 获取列表
export const createAddress = (data) => http.post('/addresses', data)                 // 新增
export const updateAddress = (data) => http.put(`/addresses/${data.id}`, data)       // 修改
export const deleteAddress = (id) => http.delete(`/addresses/${id}`)                 // 删除
export const setDefaultAddress = (id) => http.put(`/addresses/${id}/default`)        // 设默认
