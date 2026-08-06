<template>
  <div class="products">
    <h1>全部商品</h1>
    <p class="subtitle">共 {{ pagination.total }} 件商品</p>

    <!-- 筛选区域 -->
    <div class="filter-bar">
      <input v-model.trim="filters.keyword" type="text" placeholder="搜索商品" />
      <select v-model="filters.categoryId">
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
              <template v-if="isAdminUser">
                <button class="btn btn-secondary compact" 
                @click="editProduct(product.id)">
                  编辑
                </button>
                <button class="btn btn-danger compact" 
                @click="deleteProduct(product.id)">
                  删除
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

<script>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import http from '../utils/http'
import openAdminWindow from '../utils/appLinks'
import { isAdmin } from '../utils/auth'
import { PRODUCT_CATEGORIES, getCategoryLabel, resolveProductImage } from '../utils/productCatalog'

// const router = useRouter()
// const loading = ref(true)
// const products = ref([])
// const categories = PRODUCT_CATEGORIES

/**
 * 工厂函数，创建默认的筛选条件对象
 * 使用工厂函数的好处：在重置筛选的时候需要恢复到默认值
 * 工厂函数可以保证每次返回都是完整对象，避免遗漏字段
 */
const createDefaultFilters = () => ({
  keyword: '',
  categoryId: '',
  minPrice: '',
  maxPrice: '',
  sortBy: 'latest',
  sortDirection: 'desc'
})

export default{
  name: 'ProductsView',
  setup() {
    const router = useRouter()
    // 1、响应式状态
    // 商品列表数据
    const products = ref([])
    // 加载状态
    const loading = ref(true)
    // 错误状态
    const error = ref('')
    // 筛选条件
    const filters = ref(createDefaultFilters())
    // 分类列表
    const categories = PRODUCT_CATEGORIES
    // 分页信息
    const pagination = ref({
      page: 1,
      pageSize: 8,
      total: 0,
      totalPages: 0
    })
    // 2、计算属性
    // 管理员判断
    const isAdminUser = computed(() => isAdmin())
    // 3、工具函数
    const goToProduct = (id) => router.push(`/product/${id}`)
    const resolveImage = (product) => resolveProductImage(product)
    const editProduct = (productId) => 
      openAdminWindow(`/products/${productId}/edit`)

    // 获取商品列表
    const fetchProducts = async (page = 1) => {
      // 1. 设置 loading.value = true
      loading.value = true
      error.value = ''
      // 2. 调用 http.get('/products/query', { params: {...} })
      //    params 包含：page, pageSize, keyword, categoryId, 
      //    minPrice, maxPrice, sortBy, sortDirection
      //    注意：空值用 undefined（不传给后端）
      try {
        const response = await http.get('/products/query', {
          params: {
            page,
            pageSize: pagination.value.pageSize,
            keyword: filters.value.keyword || undefined,
            categoryId: filters.value.categoryId || undefined,
            minPrice: filters.value.minPrice ?? undefined,
            maxPrice: filters.value.maxPrice ?? undefined,
            sortBy: filters.value.sortBy,
            sortDirection: filters.value.sortDirection
          }
        })
        // 避免data为空的时候报错
        products.value = response.data?.records || []
        // 3.展开运算符合并旧状态和新数据，只更新后端返回的字段
        pagination.value = {
          ...pagination.value, // ...展开运算符，复制对象的所有属性
          page: response.data?.page || page, // 设置或者覆盖page
          total: response.data?.total || 0,  // 设置total
          totalPages: response.data?.totalPages || 0  // 设置totalPages
        }
      } catch (err) {
        // 4. catch 中 console.error('加载商品失败', e)
        error.value = err.message || '获取商品列表失败'
      } finally {
        // 5. finally 中 loading.value = false
        loading.value = false
      }
    }
    // 4、用户操作
    // 重置到第一页的查询
    const applyFilters = () => fetchProducts(1)
    // 重置筛选方法
    const resetFilters = () => {
      filters.value = createDefaultFilters()
      fetchProducts(1)
    }
    const changePage = (page) => fetchProducts(page)
    
    // 删除商品
    const deleteProduct = async (productId) => {
      // 二次确认
      if(!window.confirm('确定删除这个商品吗？此操作不可恢复！')) return
      try {
        await http.delete(`/products/${productId}`)
        // 刷新当前页面
        fetchProducts(pagination.value.page)
      } catch (err) {
        error.value = err.message || '删除商品失败'
      }
    }
    onMounted(() => fetchProducts(1))
    return {
      products, loading, error, filters, categories, pagination,
      isAdminUser, goToProduct, editProduct, deleteProduct,
      applyFilters, resetFilters, changePage, resolveImage, getCategoryLabel
    }
  }
}

</script>

<style scoped>
/* Container */
.products { max-width: 1200px; margin: 0 auto; padding: 20px; }
.products h1 { margin-bottom: 8px; font-weight: 700; }
.subtitle { color: #666; margin-bottom: 20px; }

/* Filter bar */
.filter-bar { display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; align-items: center; background: transparent; }
.filter-bar input,
.filter-bar select { padding: 8px 12px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; background: #fff; }
.filter-actions { display: flex; gap: 8px; align-items: center; }

/* Button system (compact utility used in template) */
.btn { display: inline-flex; align-items: center; justify-content: center; border-radius: 4px; padding: 8px 14px; font-size: 14px; border: 1px solid transparent; cursor: pointer; transition: background 0.15s, transform 0.06s; }
.btn.compact { padding: 6px 10px; font-size: 13px; }
.btn:active { transform: translateY(1px); }
.btn-primary { background: #409eff; color: #fff; border-color: #409eff; }
.btn-primary:hover { filter: brightness(0.95); }
.btn-secondary { background: #fff; color: #333; border-color: #ddd; }
.btn-secondary:hover { background: #fafafa; }
.btn-danger { background: #f56c6c; color: #fff; border-color: #f56c6c; }

/* Loading / empty / error */
.loading { text-align: center; padding: 40px; color: #999; }
.error.state { color: #e34b4b; padding: 20px; background: #fff6f6; border-radius: 6px; }
.empty-card { padding: 20px; text-align: center; color: #666; background: #fff; border-radius: 6px; }

/* Product grid */
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 18px; }
.product-card { background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 6px 18px rgba(0,0,0,0.06); transition: transform 0.18s, box-shadow 0.18s; display: flex; flex-direction: column; }
.product-card:hover { transform: translateY(-6px); box-shadow: 0 12px 30px rgba(0,0,0,0.08); }

/* Image area */
.product-image-wrap { position: relative; width: 100%; height: 170px; background: #f5f5f5; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.product-image { width: 100%; height: 100%; object-fit: cover; display: block; }

/* Pill / category badge */
.pill { position: absolute; left: 10px; top: 10px; display: inline-block; padding: 4px 8px; border-radius: 12px; font-size: 12px; background: #ecf5ff; color: #409eff; }
.pill-info { /* kept for specific variant */ }

/* Content */
.product-content { padding: 12px 14px; display: flex; flex-direction: column; gap: 8px; flex: 1 1 auto; }
.product-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; }
.product-head h3 { font-size: 15px; margin: 0; line-height: 1.2; font-weight: 600; }
.stock-tag { font-size: 12px; color: #999; }

/* Description: allow up to 2 lines with ellipsis */
.description { color: #777; font-size: 13px; margin: 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; text-overflow: ellipsis; }

/* Footer with price and actions */
.product-footer { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-top: auto; }
.price { color: #f56c6c; font-size: 18px; font-weight: 700; margin: 0; }
.action-buttons { display: flex; gap: 8px; align-items: center; }

/* Pagination */
.pagination { display: flex; justify-content: center; align-items: center; gap: 12px; margin-top: 30px; padding: 16px 0; }
.pagination button { padding: 8px 14px; border: 1px solid #ddd; background: #fff; border-radius: 4px; cursor: pointer; }
.pagination button:disabled { opacity: 0.5; cursor: not-allowed; }

/* Responsive tweaks */
@media (max-width: 720px) {
  .filter-bar { gap: 8px; }
  .filter-bar input, .filter-bar select { flex: 1 1 100%; min-width: 0; }
  .product-grid { grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px; }
  .product-image-wrap { height: 140px; }
}
</style>