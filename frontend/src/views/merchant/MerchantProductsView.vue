<template>
  <div class="products-page">
    <div class="page-header">
      <h2 class="mc-title">商品管理</h2>
      <router-link to="/products/new" class="btn-add">+ 新增商品</router-link>
    </div>

    <div class="filter-bar mc-filter">
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
          <span :class="['mc-badge', product.status === 1 ? 'mc-badge-active' : 'mc-badge-inactive']">
            {{ product.status === 1 ? '上架' : '下架' }}
          </span>
        </div>
        <div class="product-actions">
          <router-link :to="`/products/${product.id}/edit`" class="btn-edit">编辑</router-link>
          <button @click="deleteProduct(product.id)" class="btn-delete">删除</button>
        </div>
      </div>
    </div>

    <div v-if="filteredProducts.length === 0" class="empty-state mc-empty">
      暂无商品
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

<style scoped>
.products-page { /* 外层留白由 .merchant-main 提供，此处无需额外 padding */ }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

/* 新增商品按钮：复用主题主色 */
.btn-add {
  display: inline-flex;
  align-items: center;
  padding: 9px 18px;
  background: var(--mc-primary);
  color: #fff;
  border-radius: var(--mc-radius-sm);
  font-size: 14px;
  text-decoration: none;
  transition: background 0.2s;
}
.btn-add:hover { background: var(--mc-primary-dark); }

/* 商品卡片网格：自适应列数 */
.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}
.product-card {
  background: var(--mc-card-bg);
  border-radius: var(--mc-radius);
  box-shadow: var(--mc-shadow);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: transform 0.2s, box-shadow 0.2s;
}
.product-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--mc-shadow-hover);
}
.product-image {
  height: 160px;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.product-info {
  padding: 14px 16px 8px;
  flex: 1;
}
.product-info h3 {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--mc-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.price {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 700;
  color: var(--mc-danger);
}
.stock {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--mc-text-muted);
}
.product-actions {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
}
.btn-edit,
.btn-delete {
  flex: 1;
  text-align: center;
  padding: 7px 0;
  border-radius: var(--mc-radius-sm);
  font-size: 13px;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s;
}
.btn-edit {
  background: var(--mc-primary-light);
  color: var(--mc-primary);
  border: 1px solid transparent;
}
.btn-edit:hover { background: var(--mc-primary); color: #fff; }
.btn-delete {
  background: #fff;
  color: var(--mc-danger);
  border: 1px solid var(--mc-danger);
}
.btn-delete:hover { background: var(--mc-danger); color: #fff; }
</style>
