<!--
  CheckoutView.vue — 结算页面
  ============================================================================
  【教学要点】
    1. Promise.all() 并发获取多组数据（购物车 + 地址）
    2. radio 单选按钮组的 v-model 绑定
    3. submitting 状态防重复提交
    4. computed 自动求和总价
    5. 提交成功后 router.push 跳转
-->
<template>
	<div class="checkout page-shell">
		<div class="page-header">
			<div>
				<span class="eyebrow">Checkout</span>
				<h1>提交订单</h1>
				<p class="page-subtitle">
					请选择已维护好的收货地址并核对商品金额，确认无误后提交订单。
				</p>
			</div>
			<div class="checkout-links">
				<router-link to="/cart#address-management" class="link-inline">去购物车管理收货地址</router-link>
				<router-link to="/cart" class="link-inline">返回购物车</router-link>
			</div>
		</div>

		<p v-if="error" class="state error">{{ error }}</p>
		<p v-else-if="loading" class="state">加载中...</p>

		<div v-else-if="cartItems.length" class="checkout-layout">
			<!-- 左侧：地址选择 + 提交按钮 -->
			<section class="checkout-form">
				<div class="section-head">
					<h2>收货地址</h2>
					<span class="pill pill-info">{{ addresses.length }} 条</span>
				</div>

				<div v-if="!addresses.length" class="empty-card">
					<p>你还没有收货地址，请先去购物车页面的地址管理区域新增地址。</p>
					<router-link to="/cart#address-management" class="btn btn-primary">去管理收货地址</router-link>
				</div>

				<!-- 单选按钮组：v-model 共享 selectedAddressId，选中时自动高亮 -->
				<div v-else class="address-list">
					<label v-for="address in addresses" :key="address.id" class="address-option"
						:class="{ active: selectedAddressId === address.id }">
						<input v-model="selectedAddressId" :value="address.id" type="radio" name="address" />
						<div class="address-option-body">
							<div class="address-option-head">
								<div>
									<h3>{{ address.receiver }}</h3>
									<p>{{ address.phone }}</p>
								</div>
								<span v-if="address.isDefault === 1" class="pill pill-success">默认地址</span>
							</div>
							<p>{{ fullAddress(address) }}</p>
						</div>
					</label>
				</div>

				<!-- 提交按钮：submitting 或未选地址时禁用 -->
				<button class="btn btn-primary submit-btn" type="button" :disabled="submitting || !selectedAddressId"
					@click="submitOrder">
					{{ submitting ? "提交中..." : "提交订单" }}
				</button>
			</section>

			<!-- 右侧：订单商品摘要 -->
			<aside class="checkout-summary">
				<div class="summary-head">
					<h2>订单商品</h2>
					<span class="pill pill-info">{{ cartItems.length }} 件</span>
				</div>
				<div v-for="item in cartItems" :key="item.id" class="summary-item">
					<div>
						<h3>{{ item.productName }}</h3>
						<p>x {{ item.quantity }}</p>
					</div>
					<span>¥{{ item.subtotal || item.price * item.quantity }}</span>
				</div>
				<div class="summary-total">
					<strong>合计</strong>
					<strong>¥{{ totalPrice }}</strong>
				</div>
			</aside>
		</div>

		<div v-else class="empty-card">
			<p>购物车为空，暂时无法结算。</p>
			<router-link to="/products" class="btn btn-primary">去购物</router-link>
		</div>
	</div>
</template>

<script>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import http from "../utils/http";
import { alertMessage } from "../utils/modal";

export default {
	name: "CheckoutView",
	setup() { 
		const router = useRouter()
		const cartItems = ref([])
		const addresses = ref([])
		const selectedAddressId = ref(null)
		const loading = ref(false)
		const submitting = ref(false)
		const error = ref('')

		// computed自动求和，当cartItems任意商品数量或价格发生改变都会自动重新计算总价
		const totalPrice = computed(() => {
			return cartItems.value.reduce((total, item) =>{
				// 优先使用后端计算好的subtotal，否则使用前端兜底的价格*数量
				return total + 
					Number(item.subtotal || item.price * item.quantity || 0)
			}, 0)
		})
		// 拼接省市区详细地址
		const fullAddress = (address) =>
			`${address.province} ${address.city} ${address.district} ${address.detailAddress}`
		// 拉取购物车商品列表
		const fetchCart = async () => {
			const response = await http.get('/cart')
			cartItems.value = response.data || []
		}
		// 拉取地址列表
		const fetchAddresses = async () => {
			const response = await http.get('/addresses')
			addresses.value = response.data || []
			// 默认选中
			const defaultAddr = addresses.value.find(
				(item) => item.isDefault === 1
			)
			selectedAddressId.value = defaultAddr?.id || addresses.value[0]?.id || null
		}

		// 初始化数据
		const initData = async () => {
			loading.value = true
			error.value = ''
			// 并发请求，同时拉去购物车和地址，如果任一失败则catch
			try {
				await Promise.all([fetchCart(), fetchAddresses()])
			} catch (err) {
				error.value = err.message || '获取结算信息失败'
			} finally {
				loading.value = false
			}
		}

		// 提交订单
		const submitOrder = async () => {
			// 前置校验，如果没有选择地址就直接拦截，避免无效请求
			if(!selectedAddressId.value){
				error.value = '请选择收货地址'
				return 
			}
			// 当按钮为提交状态的时候，禁用按钮防止重复点击
			submitting.value = true
			try {
				const response = await http.post('/cart/checkout', {
					addressId: selectedAddressId.value
				})
				const orderId = response.data?.order?.id
				await alertMessage('下单成功')
				if(orderId){
					router.push(`/orders/${orderId}`)
					return
				}
				router.push('/orders')
			} catch (err) {
				error.value = err.message || '提交订单失败'
			} finally {
				submitting.value = false
			}
		}

		onMounted(initData)
		return {
			cartItems, addresses, selectedAddressId,
			loading, submitting, error,
			totalPrice, submitOrder, fullAddress
		}
		
	},
};
</script>

<style scoped>
/* 页面整体：与其他订单页一致的居中容器 */
.checkout.page-shell {
	max-width: 1080px;
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
.checkout-links { display: flex; gap: 12px; flex-wrap: wrap; }
.link-inline {
	color: var(--primary); text-decoration: none; font-size: 14px; font-weight: 600; white-space: nowrap;
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

/* 两栏布局 */
.checkout-layout {
	display: grid;
	grid-template-columns: 1.3fr 1fr;
	gap: 24px;
	align-items: start;
}

.checkout-form,
.checkout-summary {
	padding: 24px;
	border-radius: 20px;
	background: var(--surface);
	border: 1px solid var(--line);
	box-shadow: 0 18px 45px rgba(50, 77, 135, 0.06);
}

.section-head,
.summary-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12px;
	margin-bottom: 18px;
}
.section-head h2,
.summary-head h2 { margin: 0; font-size: 18px; font-weight: 700; color: var(--text); }

/* 状态徽标（条数提示） */
.pill {
	display: inline-flex; align-items: center; padding: 5px 12px; border-radius: 999px;
	font-size: 13px; font-weight: 700; line-height: 1; white-space: nowrap;
}
.pill-info { background: rgba(79, 124, 255, 0.14); color: var(--primary); }
.pill-success { background: rgba(5, 150, 105, 0.14); color: var(--success); }

/* 地址选择 */
.address-list {
	display: flex;
	flex-direction: column;
	gap: 14px;
}
.address-option {
	display: flex;
	gap: 14px;
	padding: 16px;
	border: 1px solid var(--line);
	border-radius: 18px;
	background: #f8fbff;
	cursor: pointer;
	transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.address-option.active {
	border-color: var(--primary);
	box-shadow: 0 12px 28px rgba(79, 124, 255, 0.14);
}
.address-option input { margin-top: 6px; accent-color: var(--primary); }
.address-option-body { flex: 1; }
.address-option-head {
	display: flex;
	justify-content: space-between;
	gap: 12px;
	margin-bottom: 8px;
}
.address-option-body h3 { margin: 0; font-size: 16px; font-weight: 700; color: var(--text); }
.address-option-body p { margin: 0; color: var(--muted); line-height: 1.7; }

.submit-btn { width: 100%; margin-top: 18px; }

/* 订单商品摘要 */
.summary-item,
.summary-total {
	display: flex;
	justify-content: space-between;
	align-items: center;
	gap: 16px;
	padding: 12px 0;
	border-bottom: 1px solid var(--line);
}
.summary-item h3 { margin: 0 0 4px; font-size: 15px; font-weight: 600; color: var(--text); }
.summary-item p { margin: 0; color: var(--muted); font-size: 13px; }
.summary-total {
	border-bottom: none;
	margin-top: 8px;
	padding-top: 16px;
	font-size: 20px;
}
.summary-total strong:last-child { color: var(--danger); font-weight: 800; }

/* 通用按钮 */
.btn {
	display: inline-flex; align-items: center; justify-content: center;
	padding: 10px 18px; border-radius: 12px; font-size: 14px; font-weight: 600;
	border: 1px solid transparent; cursor: pointer; transition: filter 0.12s, transform 0.12s;
}
.btn:active { transform: translateY(1px); }
.btn-primary { background: var(--primary); color: #fff; }
.btn-primary:hover { filter: brightness(0.95); }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }

/* 移动端 */
@media (max-width: 960px) {
	.checkout-layout { grid-template-columns: 1fr; }
	.page-header { flex-direction: column; align-items: flex-start; }
}
</style>
