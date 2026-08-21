<template>
  <div class="products-page">
    <div class="page-header">
      <h2>商品管理</h2>
      <router-link to="/products/new" class="btn-add">+ 新增商品</router-link>
    </div>

    <div class="filter-bar">
      <input v-model="keyword" placeholder="搜索商品名称..." @input="filterProducts" />
      <select v-model="statusFilter" @change="filterProducts">
        <option value="">全部状态</option>
        <option value="1">上架</option>
        <option value="0">下架</option>
      </select>
    </div>

    <div class="products-grid">
      <div v-for="product in filteredProducts" :key="product.id" class="product-card">
        <div class="product-image">
          <img :src="product.image || '/placeholder.png'" :alt="product.name" />
        </div>
        <div class="product-info">
          <h3>{{ product.name }}</h3>
          <p class="price">¥{{ product.price }}</p>
          <p class="stock">库存: {{ product.stock }}</p>
          <span :class="['status-tag', product.status === 1 ? 'active' : 'inactive']">
            {{ product.status === 1 ? '上架' : '下架' }}
          </span>
        </div>
        <div class="product-actions">
          <router-link :to="`/products/${product.id}/edit`" class="btn-edit">编辑</router-link>
          <button @click="deleteProduct(product.id)" class="btn-delete">删除</button>
        </div>
      </div>
    </div>

    <div v-if="filteredProducts.length === 0" class="empty-state">
      <p>暂无商品</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import http from '../../utils/http'

const products = ref([])                // 商品列表
const keyword = ref('')                 // 搜索关键词
const statusFilter = ref('')            // 状态过滤器：空=全部，1=上架，0=下架

// 计算属性：根据关键词和状态过滤商品（响应式，输入框变化自动重新计算）
const filteredProducts = computed(() => {
  return products.value.filter(p => {
    const matchKeyword = !keyword.value || p.name.includes(keyword.value)
    const matchStatus = statusFilter.value === '' || String(p.status) === statusFilter.value
    return matchKeyword && matchStatus
  })
})

onMounted(async () => {
  try {
    const res = await http.get('/merchant/products')
    if (res.data) products.value = res.data
  } catch (e) {
    console.error('Failed to load products', e)
  }
})

const filterProducts = () => {
  // 过滤由 computed 自动处理，此处无需额外逻辑
}

const deleteProduct = async (id) => {
  if (!confirm('确定要删除此商品吗？')) return
  try {
    await http.delete(`/merchant/products/${id}`)
    // 从列表中移除已删除的商品
    products.value = products.value.filter(p => p.id !== id)
  } catch (e) {
    console.error('Failed to delete product', e)
  }
}
</script>
