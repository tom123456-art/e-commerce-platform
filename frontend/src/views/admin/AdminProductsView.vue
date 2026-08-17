<template>
  <!--
    商品管理页 —— 展示商品列表，支持新增/编辑/删除，跳转到 Excel 批量导入。
  -->
  <div class="page-shell">
    <div class="page-header">
      <div>
        <span class="eyebrow">Products</span>
        <h1>商品管理</h1>
        <p class="page-subtitle">管理商品上下架、库存与价格，支持 Excel 批量导入。</p>
      </div>
      <div class="header-actions">
        <!-- 跳转到 Excel 批量导入区域（带 #import hash 定位） -->
        <button class="btn btn-secondary" @click="$router.push('/products/new#import')">Excel 批量导入</button>
        <button class="btn btn-primary" @click="$router.push('/products/new')">新增商品</button>
      </div>
    </div>

    <p v-if="error" class="state error">{{ error }}</p>
    <p v-else-if="loading" class="state">加载中...</p>

    <div v-else class="table-card">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>图片</th>
            <th>名称</th>
            <th>价格</th>
            <th>库存</th>
            <th>分类</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="product in products" :key="product.id">
            <td>{{ product.id }}</td>
            <td><img :src="resolveImage(product)" :alt="product.name" class="product-thumb" /></td>
            <td>{{ product.name }}</td>
            <td>¥{{ Number(product.price).toFixed(2) }}</td>
            <td :class="{ 'low-stock': product.stock <= 10 }">{{ product.stock }}</td>
            <td>{{ getCategoryLabel(product.categoryId) }}</td>
            <td>
              <span :class="['status-tag', product.status === 1 ? 'active' : 'inactive']">
                {{ product.status === 1 ? '上架' : '下架' }}
              </span>
            </td>
            <td>
              <button @click="$router.push(`/products/${product.id}/edit`)" class="btn btn-secondary btn-sm">编辑</button>
              <button @click="deleteProduct(product.id)" class="btn btn-danger btn-sm">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
/**
 * AdminProductsView 脚本
 *
 * 【依赖】
 *   - http：Axios 实例，发起 API 请求
 *   - PRODUCT_CATEGORIES, resolveProductImage：商品分类与图片地址工具函数
 *   - alertMessage：统一的弹窗提示（替代原生 alert）
 *   - confirmDialog：统一的确认对话框（替代原生 confirm）
 */
import { onMounted, ref } from 'vue'
import http from '../../utils/http'
import { PRODUCT_CATEGORIES, resolveProductImage } from '../../utils/productCatalog'
import { alertMessage, confirmDialog } from '../../utils/modal'

export default {
  name: 'AdminProductsView',
  setup() {
    const loading = ref(false)
    const error = ref('')
    const products = ref([])

    /**
     * 加载商品列表
     * GET /api/products → 返回 List<Product>
     */
    const loadProducts = async () => {
        loading.value = true
        error.value = ''
        try {
          const response = await http.get('/products')
          products.value = response.data || []
        } catch (err) {
          error.value = err.message || '加载商品失败'
        } finally {
          loading.value = false
        }
    }


    /**
     * 删除商品
     * DELETE /api/products/{id}
     */
    const deleteProduct = async (id) => {
      if (!confirm('确认删除此商品？此操作不可以撤销')) return
        try {
            await http.delete(`/products/${id}`)
            await loadProducts()
        } catch (err) {
            await alertMessage(err.message || '删除失败')
        }
    }

    /**
     * 分类 ID 转 中文名称
     * 从 PRODUCT_CATEGORIES 常量中查找匹配项
     */
    const getCategoryLabel = (categoryId) => {
      const category = PRODUCT_CATEGORIES.find(c => c.id === categoryId)
      return category ? category.label : '未分类'
    }

    /** 解析商品图片地址（处理远程/占位/SVG 生成等情况） */
    const resolveImage = (product) => resolveProductImage(product)

    onMounted(loadProducts)

    return { loading, error, products, deleteProduct, getCategoryLabel, resolveImage }
  }
}
</script>

<style scoped>
/* ============================================================
 * 管理后台通用样式 - 页面头部 / 按钮 / 状态 / 表格卡片
 * ============================================================ */

/* ---------- 页面头部 ---------- */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}

.eyebrow {
  display: inline-block;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: #4f7cff;
  background: rgba(79, 124, 255, 0.10);
  padding: 4px 10px;
  border-radius: 999px;
}

.page-header h1 {
  margin-top: 10px;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.2px;
}

.page-subtitle {
  color: #64748b;
  margin-top: 6px;
  font-size: 14px;
  line-height: 1.6;
}

.header-actions { display: flex; gap: 12px; align-items: center; }

/* ---------- 按钮 ---------- */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 18px;
  border: none;
  border-radius: 10px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  text-decoration: none;
  transition: transform .15s ease, box-shadow .15s ease, background-color .15s ease, color .15s ease;
}

.btn-primary {
  background: linear-gradient(135deg, #4f7cff 0%, #3558d3 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(79, 124, 255, 0.30);
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(79, 124, 255, 0.38);
}

.btn-secondary {
  background: #eef1f6;
  color: #334155;
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.btn-secondary:hover { background: #e2e8f0; }

.btn-danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  box-shadow: 0 3px 10px rgba(239, 68, 68, 0.25);
}

.btn-danger:hover {
  filter: brightness(1.05);
  transform: translateY(-1px);
}

.btn-sm { padding: 6px 12px; font-size: 13px; border-radius: 8px; }
.btn.compact { padding: 8px 14px; font-size: 13px; }

/* ---------- 状态文本 ---------- */
.state { padding: 48px 20px; text-align: center; color: #64748b; font-size: 14px; }
.state.error { color: #ef4444; font-weight: 600; }

/* ---------- 表格卡片 ---------- */
.table-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.05);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.data-table { width: 100%; border-collapse: collapse; }

.data-table th,
.data-table td {
  padding: 14px 18px;
  text-align: left;
  border-bottom: 1px solid #eef2f7;
}

.data-table th {
  background: #f8fafc;
  font-weight: 600;
  font-size: 12.5px;
  letter-spacing: 0.3px;
  text-transform: uppercase;
  color: #64748b;
}

.data-table tbody tr { transition: background-color .12s ease; }
.data-table tbody tr:hover { background: #f5f8ff; }
.data-table tbody tr:last-child td { border-bottom: none; }

.data-table select {
  padding: 6px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  font-size: 13px;
  outline: none;
  transition: border-color .15s ease, box-shadow .15s ease;
}

.data-table select:focus {
  border-color: #4f7cff;
  box-shadow: 0 0 0 3px rgba(79, 124, 255, 0.15);
}

/* ---------- 状态标签 ---------- */
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-tag::before {
  content: "";
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

/* ---------- 商品缩略图 ---------- */
.product-thumb {
  width: 52px;
  height: 52px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid rgba(15, 23, 42, 0.06);
}

/* 低库存高亮 */
.low-stock { color: #ef4444; font-weight: 700; }

/* 上下架状态 */
.status-tag.active { background: #dcfce7; color: #16a34a; }
.status-tag.inactive { background: #fee2e2; color: #dc2626; }
</style>
