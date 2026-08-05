<template>
  <div class="products">
    <h1>全部商品</h1>
    <p class="subtitle">共 {{ pagination.total }} 件商品</p>

    <!-- 筛选区域 -->
    <div class="filter-bar">
      <input v-model.trim="filters.keyword" type="text" placeholder="搜索商品" @keyup.enter="loadProducts" />
      <select v-model="filters.categoryId" @change="loadProducts">
        <option value="">全部分类</option>
        <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.label }}</option>
      </select>
      <input v-model.number="filters.minPrice" type="number" min="0" placeholder="最低价" />
      <input v-model.number="filters.maxPrice" type="number" min="0" placeholder="最高价" />
      <select v-model="filters.sortBy">
        <option value="latest">最新</option>
        <option value="price">价格</option>
        <option value="stock">库存</option>
        <option value="name">名称</option>
      </select>
      <select v-model="filters.sortDirection">
        <option value="desc">降序</option>
        <option value="asc">升序</option>
      </select>
      <!-- <button @click="loadProducts" class="search-btn">搜索</button> -->
       <div class="filter-actions">
          <button class="btn btn-primary compact" @click="applyFilters" >
              开始筛选
          </button>
          <button class="btn btn-secondary compact" @click="resetFilters" >
              重置条件
          </button>
       </div>
    </div>

    <!-- 商品列表 -->
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error state">{{ error }}</div>
    <div v-else-if="!products.length" class="empty-card">
      <p>当前筛选条件之下暂无商品，请尝试调整关键词或其他筛选条件</p>
    </div>
    <div v-else class="product-grid">
      <article v-for="product in products" :key="product.id" 
      class="product-card">
        <div class="product-image-wrap">
          <!-- 图片 -->
          <img :src="resolveImage(product)" :alt="product.name" 
          class="product-image" />
          <!-- 分类 -->
          <span class="pill pill-info">
            {{ getCategoryLabel(product.categoryId) }}
          </span> 
        </div> 
        <div class="product-content">
          <div class="product-head">
              <h3>{{ product.name }}</h3>
              <span class="stock-tag">库存{{ product.stock }}</span>
          </div>
          <p class="description"> {{ product.description }}</p>
          <div class="product-footer">
            <p class="price"> ￥{{ product.price }}</p>
            <div class="action-buttons">
              <button class="btn btn-primary compact"
               @click="goToProduct(product.id)">
                查看详情
              </button>
              <!-- 管理员的专属按钮，可以编辑或删除商品 -->
              <template v-if="idAdminUser">
                <button class="btn btn-secondary compact" 
                @click="editProduct(product.id)">
                  编辑
                </button>
                <button class="btn btn-danger compact" 
                @click="deleteProduct(product.id)">
                  编辑
                </button>
              </template>
            </div>
          </div>
        </div>
      </article>
    </div>

    <!-- 分页 -->
    <div v-if="pagination.totalPages > 1" class="pagination">
      <button :disabled="pagination.page <= 1" @click="changePage(pagination.page - 1)">上一页</button>
      <span>第 {{ pagination.page }} / {{ pagination.totalPages }} 页</span>
      <button :disabled="pagination.page >= pagination.totalPages" @click="changePage(pagination.page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../utils/http'
import { PRODUCT_CATEGORIES, getCategoryLabel, resolveProductImage } from '../utils/productCatalog'

const router = useRouter()
const loading = ref(true)
const products = ref([])
const categories = PRODUCT_CATEGORIES

const filters = reactive({
  keyword: '',
  categoryId: '',
  minPrice: '',
  maxPrice: '',
  sortBy: 'latest',
  sortDirection: 'desc'
})

const pagination = reactive({
  page: 1,
  pageSize: 8,
  total: 0,
  totalPages: 0
})

const resolveImage = (product) => resolveProductImage(product)
import { formatPrice } from '@/utils/formatters'
const goToProduct = (id) => router.push(`/products/${id}`)

// TODO: 课堂练习 -- 实现商品列表加载
// 提示：
// 1. 设置 loading.value = true
// 2. 调用 http.get('/products/query', { params: {...} })
//    params 包含：page, pageSize, keyword, categoryId, minPrice, maxPrice, sortBy, sortDirection
//    注意：空值用 undefined（不传给后端）
// 3. 成功后：
//    - products.value = res.data.items || []
//    - pagination.total = res.data.total || 0
//    - pagination.totalPages = Math.ceil(pagination.total / pagination.pageSize)
// 4. catch 中 console.error('加载商品失败', e)
// 5. finally 中 loading.value = false

const loadProducts = async () => {
  // ====== 课堂练习：在下面写出商品列表加载逻辑 ======
  loading.value = true
  try {
    // 步骤1：调用 http.get('/products/query', { params: {...} })
    // 步骤2：保存商品列表和分页信息
  } catch (e) {
    // 步骤3：错误处理
  } finally {
    // 步骤4：关闭 loading
  }
  // ====== 课堂练习结束 ======
}

const changePage = (page) => {
  pagination.page = page
  loadProducts()
}

onMounted(() => loadProducts())
</script>

<style scoped>
.products { max-width: 1200px; margin: 0 auto; padding: 20px; }
.products h1 { margin-bottom: 8px; }
.subtitle { color: #666; margin-bottom: 20px; }
.filter-bar { display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; }
.filter-bar input, .filter-bar select { padding: 8px 12px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; }
.search-btn { padding: 8px 20px; background: #409eff; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.loading { text-align: center; padding: 40px; color: #999; }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 20px; }
.product-card { background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); cursor: pointer; transition: transform 0.2s; }
.product-card:hover { transform: translateY(-4px); }
.product-img { width: 100%; height: 180px; object-fit: cover; }
.product-info { padding: 12px; }
.product-info h3 { font-size: 16px; margin-bottom: 8px; }
.product-desc { color: #999; font-size: 12px; margin-bottom: 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-price { color: #f56c6c; font-size: 20px; font-weight: bold; margin-bottom: 8px; }
.product-category { display: inline-block; padding: 2px 8px; background: #ecf5ff; color: #409eff; border-radius: 4px; font-size: 12px; }
.pagination { display: flex; justify-content: center; align-items: center; gap: 20px; margin-top: 30px; padding: 20px; }
.pagination button { padding: 8px 16px; border: 1px solid #ddd; background: #fff; border-radius: 4px; cursor: pointer; }
.pagination button:disabled { opacity: 0.5; cursor: not-allowed; }
</style>