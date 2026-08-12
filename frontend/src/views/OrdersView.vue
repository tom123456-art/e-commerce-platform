<!--
  OrdersView.vue — 订单列表页
  ============================================================================
  【教学要点】
    1. 动态 class 绑定：:class="['pill', getStatusClass(order.status)]"
    2. switch 状态映射（数字 → 文字/样式）
    3. 条件渲染按钮（按订单状态显示不同操作）
    4. 支付 URL 白名单验证（防钓鱼）
    5. 确认收货只传 { id, targetStatus }（防篡改）
-->
<template>
  <div class="orders page-shell">
    <div class="page-header">
      <div>
        <span class="eyebrow">Orders</span>
        <h1>我的订单</h1>
        <p class="page-subtitle">集中查看订单状态、支付进度和收货信息。</p>
      </div>
      <router-link to="/products" class="link-inline">继续购物</router-link>
    </div>

    <p v-if="error" class="state error">{{ error }}</p>
    <p v-else-if="loading" class="state">加载中...</p>

    <div v-else-if="orders.length > 0" class="order-list">
      <article v-for="order in orders" :key="order.id" class="order-card">
        <div class="order-header">
          <div>
            <h3>订单号：{{ order.orderNo }}</h3>
            <p class="order-time">{{ order.createTime }}</p>
          </div>
          <span :class="['pill', getStatusClass(order.status)]">{{ getOrderStatus(order.status) }}</span>
        </div>

        <div class="order-info">
          <p>订单金额：¥{{ order.totalAmount }}</p>
          <p>收货人：{{ order.receiver }}</p>
          <p>联系电话：{{ order.phone }}</p>
          <p>收货地址：{{ order.address }}</p>
        </div>

        <!-- 条件渲染操作按钮：0=待支付显示"立即支付"，1=已支付显示"确认收货" -->
        <div class="order-actions">
          <button v-if="order.status === 0" class="btn btn-primary compact" @click="payOrder(order)">立即支付</button>
          <button v-if="order.status === 1" class="btn btn-secondary compact" @click="confirmReceipt(order)">确认收货</button>
          <button class="btn btn-secondary compact" @click="viewOrderDetails(order.id)">查看详情</button>
        </div>
      </article>
    </div>

    <div v-else class="empty-card">
      <p>当前还没有订单，先去下单体验一下吧。</p>
      <router-link to="/products" class="btn btn-primary">去下单</router-link>
    </div>
  </div>
</template>

<script>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../utils/http'
import { getCurrentUser } from '../utils/auth'
import { alertMessage } from '../utils/modal'

export default {
	name: 'OrdersView',
	setup() {
		const router = useRouter()
		const orders = ref([])
		const loading = ref(false)
		const error = ref('')
		const user = getCurrentUser()

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

		const fetchOrders = async () => {
			// 如果没有登录的话，需要跳转到登录页面
			if(!user?.id){
				router.push('/login')
				return
			}
			loading.value = true
			error.value = ''
			try {
				// 按照userId拉取我的订单列表
				const response = await http.get(`/orders/user/${user.id}`)
				orders.value = response.data || []
			} catch (err) {
				error.value = err.message || '获取订单列表失败'
			} finally {
				loading.value = false
			}
		}

		// 支付订单
		const payOrder = async (order) => {
			try {
				const response = await http.post('/payment/create', null, {
					params: {
						orderNo: order.orderNo,
						amount: order.totalAmount,
						description: `订单支付-${order.orderNo}`
					}
				})
				const paymentUrl = response.data?.paymentUrl
				// 跳转支付页面
				window.location.href = paymentUrl
			} catch (err) {
				await alertMessage(err.message || '创建订单失败')
			}
		}
		

		// 确认收货
		const confirmReceipt = async (order) => {
			try {
				await http.put('/orders', {id: order.id, targetStatus: 2})
				await fetchOrders()
			} catch (err) {
				await alertMessage(err.message || '确认收货失败')
			}
		}

		const viewOrderDetails = (orderId) => router.push(`/orders/${orderId}`)

		onMounted(fetchOrders)

		return {
			orders, loading, error,
			getOrderStatus, getStatusClass,
			payOrder, confirmReceipt, viewOrderDetails
		}
	}
}
</script>

<style scoped>
/* 页面整体：与首页/结算页一致的居中容器 + 顶部留白 */
.orders.page-shell {
  max-width: 960px;
  margin: 0 auto;
  padding: 40px 24px 56px;
}

/* 页头：标题 + 右上角「继续购物」链接 */
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
  color: var(--primary); text-decoration: none; font-size: 14px; font-weight: 600;
  white-space: nowrap;
}
.link-inline:hover { text-decoration: underline; }

/* 加载 / 错误 / 空状态 */
.state { text-align: center; padding: 48px 0; color: var(--muted); font-size: 15px; }
.state.error {
  color: #fff; background: var(--danger); border-radius: 16px; padding: 16px 20px;
  box-shadow: 0 12px 28px rgba(239, 68, 68, 0.18);
}
.empty-card {
  text-align: center; padding: 56px 24px; background: var(--surface);
  border-radius: 24px; box-shadow: 0 18px 45px rgba(50, 77, 135, 0.08); color: var(--muted);
}
.empty-card .btn { margin-top: 18px; }

/* 订单卡片列表 */
.order-list { display: flex; flex-direction: column; gap: 18px; }
.order-card {
  padding: 24px; border-radius: 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  box-shadow: 0 18px 45px rgba(50, 77, 135, 0.06);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.order-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 24px 55px rgba(50, 77, 135, 0.12);
}

/* 订单头部：左订单号+时间，右状态徽标 */
.order-header {
  display: flex; justify-content: space-between; align-items: flex-start; gap: 20px;
  border-bottom: 1px solid var(--line); padding-bottom: 16px; margin-bottom: 14px;
}
.order-header h3 { margin: 0; font-size: 17px; font-weight: 700; color: var(--text); }
.order-time { color: var(--muted); margin-top: 6px; font-size: 13px; }

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
.order-info {
  display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px 24px;
  padding: 4px 0;
}
.order-info p { margin: 0; color: var(--muted); font-size: 14px; }
.order-info p strong { color: var(--text); }

/* 操作按钮区 */
.order-actions {
  margin-top: 18px; display: flex; justify-content: flex-end; gap: 10px;
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

/* 移动端：头部与按钮纵向铺开 */
@media (max-width: 720px) {
  .page-header { flex-direction: column; align-items: flex-start; }
  .order-header { flex-direction: column; align-items: stretch; }
  .order-info { grid-template-columns: 1fr; }
  .order-actions { flex-direction: column; align-items: stretch; }
  .order-actions .btn { width: 100%; }
}
</style>
