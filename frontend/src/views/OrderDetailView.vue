<!--
  OrderDetailView.vue — 订单详情页
  ============================================================================
  【教学要点】
    1. route.params 获取动态路由参数（/orders/:id）
    2. 嵌套数据访问：detail.order.orderNo（三层）
    3. 与 OrdersView 共享状态映射逻辑（实际项目可抽 composable）
    4. 支付 URL 白名单（同 OrdersView）
-->
<template>
  <div class="order-detail page-shell">
    <div class="page-header">
      <div>
        <span class="eyebrow">Order Detail</span>
        <h1>订单详情</h1>
        <p class="page-subtitle">核对订单信息、商品明细和当前处理状态。</p>
      </div>
      <router-link to="/orders" class="link-inline">返回订单列表</router-link>
    </div>

    <p v-if="error" class="state error">{{ error }}</p>
    <p v-else-if="loading" class="state">加载中...</p>

    <!-- detail 初始为 null，加载成功后变为 { order, orderItems } -->
    <div v-else-if="detail" class="detail-layout">
      <section class="detail-card">
        <div class="detail-head">
          <div>
            <h2>订单号：{{ detail.order.orderNo }}</h2>
            <p>创建时间：{{ detail.order.createTime }}</p>
          </div>
          <span :class="['pill', getStatusClass(detail.order.status)]">
            {{ getOrderStatus(detail.order.status) }}
          </span>
        </div>

        <div class="detail-info">
          <p>收货人：{{ detail.order.receiver }}</p>
          <p>联系电话：{{ detail.order.phone }}</p>
          <p>收货地址：{{ detail.order.address }}</p>
          <p>订单金额：¥{{ detail.order.totalAmount }}</p>
        </div>

        <div class="detail-actions">
          <button v-if="detail.order.status === 0" class="btn btn-primary compact" @click="payOrder">立即支付</button>
          <button v-if="detail.order.status === 1" class="btn btn-secondary compact" @click="confirmReceipt">确认收货</button>
        </div>
      </section>

      <section class="detail-card">
        <h2>商品明细</h2>
        <div v-for="item in detail.orderItemList" :key="item.id" class="item-row">
          <div>
            <h3>{{ item.productName }}</h3>
            <p>数量：{{ item.quantity }}</p>
          </div>
          <div class="item-price">¥{{ item.price * item.quantity }}</div>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '../utils/http'
import { alertMessage } from '../utils/modal'

export default {
	name: 'OrderDetailView',
	setup(){
		const route = useRoute()
		const detail = ref(null)
		const loading = ref(false)
		const error = ref('')

		// 订单状态码
		const getOrderStatus = (status) => {
			switch(status){
				case 0: return '待支付'
				case 1: return '已支付'
				case 2: return '已完成'
				default: return '未知状态'
			}
		}

		// 不同状态码不同的CSS样式
		const getStatusClass = (status) => {
			switch(status){
				case 0: return 'pill-warning'
				case 1: return 'pill-info'
				case 2: return 'pill-success'
				default: return 'pill-danger'
			}
		}

		const fetchDetail = async () => {
			loading.value = true
			error.value = ''
			try {
				const response = await http.get(`/orders/${route.params.id}/detail`)
				detail.value = response.data
			} catch (err) {
				error.value = err.message || '获取订单详情失败'
			} finally {
				loading.value = false
			}
		}

		// 支付订单
		const payOrder = async () => {
			try {
				const response = await http.post('/payment/create', null, {
					params: {
						orderNo: detail.value.order.orderNo,
						amount: detail.value.order.totalAmount,
						description: `订单支付-${detail.value.order.orderNo}`
					}
				})
				const paymentUrl = response.data?.paymentUrl
				// 跳转支付页面
				window.location.href = paymentUrl
			} catch (err) {
				await alertMessage(err.message || '创建支付失败')
			}
		}
		

		// 确认收货
		const confirmReceipt = async () => {
			try {
				await http.put('/orders', {id: detail.value.order.id, targetStatus: 2})
				await fetchDetail()
			} catch (err) {
				await alertMessage(err.message || '确认收货失败')
			}
		}

		onMounted(fetchDetail)

		return {
			detail, loading, error,
			getOrderStatus, getStatusClass,
			payOrder, confirmReceipt
		}
	}
}
</script>

<style scoped>
/* 页面整体：与订单列表页一致的居中容器 */
.order-detail.page-shell {
  max-width: 960px;
  margin: 0 auto;
  padding: 40px 24px 56px;
}

/* 页头 */
.page-header {
  display: flex; align-items: flex-end; justify-content: space-between;
  gap: 16px; margin-bottom: 28px;
}
.eyebrow {
  display: inline-block; font-size: 13px; font-weight: 700; letter-spacing: 1px;
  text-transform: uppercase; color: var(--primary);
  background: rgba(79, 124, 255, 0.12); padding: 4px 10px; border-radius: 999px;
  margin-bottom: 10px;
}
.page-header h1 { font-size: 30px; font-weight: 800; color: var(--text); margin: 0; }
.page-subtitle { margin: 8px 0 0; color: var(--muted); font-size: 14px; }
.link-inline {
  color: var(--primary); text-decoration: none; font-size: 14px; font-weight: 600; white-space: nowrap;
}
.link-inline:hover { text-decoration: underline; }

/* 加载 / 错误状态 */
.state { text-align: center; padding: 48px 0; color: var(--muted); font-size: 15px; }
.state.error {
  color: #fff; background: var(--danger); border-radius: 16px; padding: 16px 20px;
  box-shadow: 0 12px 28px rgba(239, 68, 68, 0.18);
}

/* 两张卡片：订单信息 + 商品明细 */
.detail-layout { display: flex; flex-direction: column; gap: 20px; }
.detail-card {
  padding: 24px; border-radius: 20px;
  background: var(--surface); border: 1px solid var(--line);
  box-shadow: 0 18px 45px rgba(50, 77, 135, 0.06);
}
.detail-card h2 { margin: 0 0 16px; font-size: 18px; font-weight: 700; color: var(--text); }

/* 订单头部：左订单号+时间，右状态徽标 */
.detail-head {
  display: flex; justify-content: space-between; align-items: flex-start; gap: 20px;
  border-bottom: 1px solid var(--line); padding-bottom: 16px; margin-bottom: 16px;
}
.detail-head h2 { margin: 0; font-size: 18px; font-weight: 700; color: var(--text); }
.detail-head p { margin: 6px 0 0; color: var(--muted); font-size: 13px; }

/* 状态徽标 */
.pill {
  display: inline-flex; align-items: center; padding: 5px 12px; border-radius: 999px;
  font-size: 13px; font-weight: 700; line-height: 1; white-space: nowrap;
}
.pill-warning { background: rgba(245, 158, 11, 0.14); color: #b45309; }
.pill-info { background: rgba(79, 124, 255, 0.14); color: var(--primary); }
.pill-success { background: rgba(5, 150, 105, 0.14); color: var(--success); }
.pill-danger { background: rgba(239, 68, 68, 0.14); color: var(--danger); }

/* 订单信息区 */
.detail-info {
  display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px 24px;
}
.detail-info p { margin: 0; color: var(--muted); font-size: 14px; }
.detail-info p strong { color: var(--text); }

/* 操作按钮区 */
.detail-actions {
  margin-top: 20px; display: flex; justify-content: flex-end; gap: 10px;
}
.btn {
  display: inline-flex; align-items: center; justify-content: center;
  padding: 10px 18px; border-radius: 12px; font-size: 14px; font-weight: 600;
  border: 1px solid transparent; cursor: pointer; transition: filter 0.12s, transform 0.12s;
}
.btn:active { transform: translateY(1px); }
.btn.compact { padding: 9px 16px; }
.btn-primary { background: var(--primary); color: #fff; }
.btn-primary:hover { filter: brightness(0.95); }
.btn-secondary { background: #fff; color: var(--text); border-color: var(--line); }
.btn-secondary:hover { background: #f5f7fb; }

/* 商品明细列表 */
.item-row {
  display: flex; justify-content: space-between; align-items: center; gap: 16px;
  padding: 16px 0; border-bottom: 1px solid var(--line);
}
.item-row:last-child { border-bottom: none; }
.item-row h3 { margin: 0 0 4px; font-size: 15px; font-weight: 600; color: var(--text); }
.item-row p { margin: 0; color: var(--muted); font-size: 13px; }
.item-price {
  font-size: 18px; font-weight: 800; color: var(--danger);
  white-space: nowrap;
}

/* 移动端：头部与按钮纵向铺开 */
@media (max-width: 720px) {
  .page-header { flex-direction: column; align-items: flex-start; }
  .detail-head { flex-direction: column; align-items: stretch; }
  .detail-info { grid-template-columns: 1fr; }
  .detail-actions { flex-direction: column; align-items: stretch; }
  .detail-actions .btn { width: 100%; }
  .item-row { flex-direction: column; align-items: flex-start; }
}
</style>
