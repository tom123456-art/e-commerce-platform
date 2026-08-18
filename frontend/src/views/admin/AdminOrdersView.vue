<template>
  <!--
    订单管理页 —— 展示所有订单，按状态显示标签颜色。
    本页面只读展示，订单状态流转（发货、退款等）在订单详情页处理。
  -->
  <div class="page-shell">
    <div class="page-header">
      <div>
        <span class="eyebrow">Orders</span>
        <h1>订单管理</h1>
        <p class="page-subtitle">查看所有订单，按状态筛选与导出。</p>
      </div>
      <!-- 导出订单 Excel：直接访问 /api/excel/exportOrders 触发文件下载 -->
      <button class="btn btn-secondary" @click="exportOrders">
        导出 Excel
      </button>
    </div>

    <p v-if="error" class="state error">{{ error }}</p>
    <p v-else-if="loading" class="state">加载中...</p>

    <div v-else class="table-card">
      <table class="data-table">
        <thead>
          <tr>
            <th>订单号</th>
            <th>用户ID</th>
            <th>金额</th>
            <th>收货人</th>
            <th>状态</th>
            <th>创建时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.id">
            <td>{{ order.orderNo }}</td>
            <td>{{ order.userId }}</td>
            <td>¥{{ Number(order.totalAmount).toFixed(2) }}</td>
            <td>{{ order.receiver }}</td>
            <td>
              <!-- 状态标签：根据 status 动态切换样式 -->
              <span :class="['status-tag', getStatusClass(order.status)]">
                {{ getStatusText(order.status) }}
              </span>
            </td>
            <td>{{ new Date(order.createTime).toLocaleString('zh-CN') }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
/**
 * AdminOrdersView 脚本
 *
 * 【状态映射】
 *   0 = 待支付（橙色）
 *   1 = 已支付（蓝色）
 *   2 = 已完成（绿色）
 *   3 = 已取消（红色）
 */
import { onMounted, ref } from 'vue'
import http from '../../utils/http'

export default {
  name: 'AdminOrdersView',
  setup() {
    const loading = ref(false)
    const error = ref('')
    const orders = ref([])

    // 状态码 → 中文文本
    const getStatusText = (s) => ({ 0: '待支付', 1: '已支付', 2: '已完成', 3: '已取消' }[s] || '未知')
    // 状态码 → CSS 类名
    const getStatusClass = (s) => ({ 0: 'pending', 1: 'paid', 2: 'completed', 3: 'cancelled' }[s] || '')
    //用http.get发生请求，拦截器会自动带token
    const exportOrders = async () => {
      try{
        const blob = await http.get('/excel/exportOrders', { 
          responseType: 'blob' 
        })
        const url = URL.createObjectURL(new Blob([blob]))
        const a = document.createElement('a')
        a.href = url
        a.download = 'orders.xlsx'
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        URL.revokeObjectURL(url)
      } catch (err) {
        error.value = err.message || '导出订单失败'
      }
    }

    /**
     * 加载订单列表
     * GET /api/orders → OrderController.getAll()
     */
    onMounted(async () => {
      loading.value = true
      try {
        const response = await http.get('/orders')
        orders.value = response.data || []
      } catch (err) {
        error.value = err.message || '加载订单失败'
      } finally {
        loading.value = false
      }
    })

    return { loading, error, orders, getStatusText, getStatusClass, exportOrders }
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

/* 订单状态颜色 */
.status-tag.pending { background: #fef3c7; color: #d97706; }
.status-tag.paid { background: #dbeafe; color: #2563eb; }
.status-tag.completed { background: #dcfce7; color: #16a34a; }
.status-tag.cancelled { background: #fee2e2; color: #dc2626; }
</style>
