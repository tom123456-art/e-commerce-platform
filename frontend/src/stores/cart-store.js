import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'

const CART_STORAGE_KEY = 'cart'  // localStorage 持久化 key

/** 从 localStorage 加载本地购物车（用于未登录场景的离线暂存） */
function loadCart() {
  try {
    const raw = localStorage.getItem(CART_STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

/**
 * 购物车状态管理（Pinia store）。
 * 注意：本 store 主要用于"未登录购物车"或前端缓存；
 *      登录后的购物车数据由后端 API 维护，前端通过 fetchCart 拉取。
 */
export const useCartStore = defineStore('cart', () => {
  const items = ref(loadCart())           // 商品列表
  const loading = ref(false)

  // computed：依赖 items，自动重算并缓存
  const totalPrice = computed(() =>
    items.value.reduce((sum, item) => sum + (item.price || 0) * (item.quantity || 0), 0)
  )
  const totalQuantity = computed(() =>
    items.value.reduce((sum, item) => sum + (item.quantity || 0), 0)
  )
  const isEmpty = computed(() => items.value.length === 0)

  /** 加入购物车（本地）：同商品累加数量 */
  function addItem(item) {
    const existing = items.value.find(i => i.productId === item.productId)
    if (existing) {
      existing.quantity += item.quantity
    } else {
      items.value.push(item)
    }
  }

  function removeItem(id) {
    items.value = items.value.filter(i => i.id !== id)
  }

  function clear() {
    items.value = []
  }

  // watch 深度监听 items，自动同步到 localStorage（持久化）
  watch(
    items,
    (next) => {
      try {
        localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(next))
      } catch {
        // storage 满或禁用，静默忽略
      }
    },
    { deep: true }
  )

  return { items, loading, totalPrice, totalQuantity, isEmpty, addItem, removeItem, clear }
})
