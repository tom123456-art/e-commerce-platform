<template>
  <div class="product-detail">
    <div class="detail-header">
      <div>
        <h1>购物车</h1>
        <p class="subtitle">请核对商品信息</p>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error state">{{ error }}</div>
    <!-- 购物车有商品的时候的两栏布局 -->
    <div v-else-if="cartItems.length > 0" class="detail-card cart-panel">
      <section class="cart-body">
        <div class="cart-items">
          <div class="cart-header">
            <div>
              <span class="section-tag">购物车</span>
              <h2>我的购物车</h2>
            </div>
            <!-- 修改: 添加清空购物车按钮，方便用户一键清除所有商品 -->
            <button type="button" class="btn btn-secondary clear-btn" @click="clearCart">
              清空购物车
            </button>
          </div>

          <div class="cart-list">
            <article v-for="item in cartItems" :key="item.id" class="cart-item">
              <img :src="item.image" :alt="item.productName" class="item-image" />
              <div class="item-detail">
                <h3 class="item-name">{{ item.productName }}</h3>
                <p class="item-price">￥{{ formatPrice(item.price) }}</p>
                <p class="item-stock">库存：{{ item.stock }}</p>
                <div class="item-actions">
                  <!-- 修改: 添加数量操作按钮，提升购物体验，避免用户手动输入或跳转到商品页修改 -->
                  <div class="quantity-control">
                    <button type="button" class="control-btn" @click="updateQuantity(item, Number(item.quantity) - 1)" :disabled="item.quantity <= 1">-</button>
                    <span class="quantity-value">{{ item.quantity }}</span>
                    <button type="button" class="control-btn" @click="updateQuantity(item, Number(item.quantity) + 1)" :disabled="item.quantity >= item.stock">+</button>
                  </div>
                  <!-- 修改: 添加删除按钮，方便用户直接从购物车移除商品 -->
                  <button type="button" class="text-btn remove-btn" @click="removeItem(item.id)">删除</button>
                </div>
              </div>
              <div class="item-subtotal">
                <span>小计</span>
                <strong>￥{{ formatPrice(item.subtotal || item.price * item.quantity) }}</strong>
              </div>
            </article>
          </div>
        </div>

        <aside class="cart-summary">
          <div class="summary-badge">订单预览</div>
          <p class="summary-text">确认商品数量和合计金额，准备提交订单。</p>
          <div class="summary-row">
            <span>商品总数</span>
            <strong>{{ totalCount }}件</strong>
          </div>
          <div class="summary-row">
            <span>合计</span>
            <strong class="summary-price">￥{{ formatPrice(totalPrice) }}</strong>
          </div>
          <!-- 修改: 添加去结算按钮，明确下一步操作入口 -->
          <button type="button" class="btn btn-primary checkout-btn" @click="checkout">
            去结算
          </button>
          <p class="summary-note">享受便捷购物体验，优惠商品数量充足，立即结算更安心。</p>
        </aside>
      </section>
    </div>
    <!-- 购物车为空的时候 -->
    <div v-else>
      <p>购物车还是空的，先去挑选几件喜欢的商品吧！</p>
      <router-link to="/products" class="btn btn-primary">
        去购物
      </router-link>
    </div>
    <!-- 收货地址 -->
    <section>
      <div>
        <span>Address</span>
        <h3>收货地址</h3>
      </div>
      <a href="#address-management" @click.prevent="toggleAddressManager">
        {{ showAddressManager ? '收起收货地址管理':'点击显示收货地址管理' }}
      </a>
    </section>

    <!-- 收货地址Vue页面的嵌入 -->
    <section v-if="showAddressManager" id="address-management" 
    class="address-embed-section">
      <!-- 把AddressBookView文件嵌入 -->
       <AddressBookView embedded />
    </section>
  </div>
</template>

<script>
import { ref, onMounted, computed, nextTick } from "vue";
import { useRouter, useRoute } from "vue-router";
import http from "../utils/http";
import { alertMessage, confirmDialog } from "../utils/modal";
import { formatPrice } from "../utils/formatters";
import AddressBookView from "./AddressBookView.vue";

export default {
  name: "CartView",
  // 局部注册子组件
  components: {AddressBookView},
  setup() {
    // 1、路由
    const route = useRoute();
    const router = useRouter();
    // 2、响应式状态
    // 购物车数据
    const cartItems = ref([]);
    // 加载状态
    const loading = ref(true);
    // 错误状态
    const error = ref("");
    const showAddressManager = ref(false)

    // 3、工具函数
    // 计算总价
    const totalPrice = computed(() => {
      return cartItems.value.reduce((total, item) => {
        const subtotal = item.subtotal ?? item.price * item.quantity
        return total + Number(subtotal || 0)
      }, 0)
    })
    // 修改: 新增总商品数量计算，支持订单摘要显示实际商品件数而不是项数
    const totalCount = computed(() => {
      return cartItems.value.reduce((count, item) => count + Number(item.quantity || 0), 0)
    })
    // 获取购物车
    const fetchCart = async () => {
      loading.value = true
      error.value = ''
      try {
        const response = await http.get('/cart')
        cartItems.value = response.data || []
      } catch (err) {
        error.value = err.message || '获取购物车失败'
      } finally {
        loading.value = false
      }
    }
    // 更新商品数量
    const updateQuantity = async (item, quantity) => {
      quantity = Number(quantity)
      if (Number.isNaN(quantity)) return
      if (quantity <= 0 || quantity > item.stock) return
      try {
        await http.put(`/cart/items/${item.id}`, {quantity})
        await fetchCart()
      } catch (err) {
        await alertMessage(err.message || '更新数量失败')
      }
    }
    // 修改: 增加数量更新前的数值转换与校验，避免非数字或非法数量请求发送到后端

    // 删除单个商品
    const removeItem = async (id) => {
      try {
        await http.delete(`/cart/items/${id}`)
        await fetchCart()
      } catch (err) {
        await alertMessage(err.message || '删除商品失败')
      }
    }

    // 清空购物车
    const clearCart = async () => {
      // 提示
      const confirmed = await confirmDialog('确认清空购物车吗？')
      if(!confirmed) return
      try {
        await http.delete('/cart')
        await fetchCart()
      } catch (err) {
        await alertMessage(err.message || '清空购物车失败')
      }
    }

    // 地址管理切换
    const scrollToAddressSection = async () => {
      // Vue的全局API，会在下一次的DOM更新结束周resolve
      await nextTick()
      const target = document.getElementById('address-management')
      if(target){
        target.scrollIntoView({behavior:'smooth', block: 'start'})
      }
    }


    const toggleAddressManager = async () => {
      // 切换响应式布尔值
      // v-if=showAddressManager是true的时候，把id是address-management挂载到DOM
      // 如果false的时候卸载
      showAddressManager.value = !showAddressManager.value
      // 判断是展开还是收起
      if(showAddressManager.value){
        // 使用replace而不是push，是因为不会产生新的浏览器历史记录
        // 如果用户连续点击展开或收起的话，不会在前后后退的栈里堆一堆记录
        await router.replace({
          path: '/cart',
          // 把地址面板已展开的状态写入URL，如果后面刷新页面能自动恢复展开状态
          // 而不是回到收起状态
          hash: 'address-management'
        })
        // 让页面平滑的滚动到地址区块，当然必须先展开才能滚的动（有元素）
        await scrollToAddressSection()
      } else {
        // 未展开
        await router.replace({
          path: '/cart',
          hash: ''
        })
      }
    }

    // 去结算
    const checkout = () => {
      router.push('/checkout')
    }
    
    
    onMounted(async () => {
      await fetchCart()
      // 修改: 修复 hash 读取逻辑，从 route.hash 获取地址面板状态，保证刷新后可正确展开
      if (route.hash === '#address-management') {
        showAddressManager.value = true
        await scrollToAddressSection()
      }
    })

    return {
      cartItems,
      loading,
      error,
      totalPrice,
      totalCount,
      showAddressManager,
      removeItem,
      clearCart,
      checkout,
      toggleAddressManager,
      formatPrice
    }
  },
};
</script>

<style scoped>
.product-detail {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.detail-header {
  margin-bottom: 24px;
}

.breadcrumb {
  display: inline-block;
  margin-bottom: 12px;
  color: #409eff;
  text-decoration: none;
  font-size: 14px;
}

.breadcrumb:hover {
  text-decoration: underline;
}

.product-detail h1 {
  margin: 0 0 10px;
  font-size: 28px;
  font-weight: 700;
}

.subtitle {
  color: #656d7b;
  margin: 0;
  font-size: 15px;
}

.detail-card {
  background: #fff;
  border-radius: 18px;
  padding: 28px;
  box-shadow: 0 18px 45px rgba(50, 77, 135, 0.08);
}

.cart-panel {
  padding: 24px;
}

.cart-body {
  display: grid;
  grid-template-columns: 1.65fr 0.85fr;
  gap: 24px;
}

.cart-items {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  border-bottom: 1px solid rgba(56, 66, 85, 0.08);
  padding-bottom: 18px;
}

.section-tag {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(233, 63, 63, 0.08);
  color: #e93f3f;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 10px;
}

.cart-header h2 {
  margin: 0;
  font-size: 26px;
  color: #232d42;
}

.clear-btn {
  color: #4f566b;
  background: #f5f7fb;
  border: 1px solid #d9e2ef;
}

.cart-list {
  display: grid;
  gap: 16px;
}

.cart-item {
  display: grid;
  grid-template-columns: 100px 1fr auto;
  gap: 18px;
  align-items: center;
  padding: 18px 20px;
  border-radius: 18px;
  background: #fbfbfd;
  border: 1px solid rgba(64, 158, 255, 0.08);
}

.item-image {
  width: 100%;
  height: 100px;
  object-fit: cover;
  border-radius: 16px;
  background: #f4f7ff;
}

.item-detail {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 10px;
}

.item-name {
  margin: 0;
  color: #1f2739;
  font-size: 17px;
  font-weight: 700;
}

.item-price {
  margin: 0;
  color: #e93f3f;
  font-size: 16px;
  font-weight: 700;
}

.item-stock {
  margin: 0;
  color: #687087;
  font-size: 14px;
}

.item-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.quantity-control {
  display: inline-flex;
  align-items: center;
  border: 1px solid #d9e2ef;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.control-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: #232d42;
  font-size: 18px;
  cursor: pointer;
}

.control-btn:disabled {
  color: #b0b8c7;
  cursor: not-allowed;
}

.quantity-value {
  min-width: 44px;
  text-align: center;
  font-size: 15px;
  color: #232d42;
}

.text-btn {
  border: none;
  background: none;
  color: #409eff;
  font-size: 14px;
  cursor: pointer;
  padding: 0;
}

.remove-btn {
  color: #e93f3f;
}

.item-subtotal {
  text-align: right;
  min-width: 120px;
}

.item-subtotal span {
  display: block;
  color: #687087;
  font-size: 13px;
}

.item-subtotal strong {
  color: #232d42;
  font-size: 18px;
}

.cart-summary {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 24px;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #fff7f7 100%);
  border: 1px solid rgba(233, 63, 63, 0.12);
}

.summary-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 16px;
  border-radius: 999px;
  background: #ffe9e9;
  color: #e93f3f;
  font-weight: 700;
  font-size: 13px;
}

.summary-text {
  margin: 0;
  color: #4f566b;
  line-height: 1.8;
  font-size: 15px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid rgba(56, 66, 85, 0.08);
  color: #4f566b;
  font-size: 15px;
}

.summary-row:last-of-type {
  border-bottom: none;
}

.summary-price {
  color: #e93f3f;
  font-size: 26px;
}

.checkout-btn {
  width: 100%;
}

.summary-note {
  margin: 0;
  color: #687087;
  font-size: 13px;
}

.btn-primary {
  background: #e93f3f;
  color: #fff;
  border-color: #e93f3f;
  box-shadow: 0 12px 30px rgba(233, 63, 63, 0.18);
}

.btn-primary:hover {
  filter: brightness(0.96);
}

.btn-secondary:hover {
  background: #eef3fb;
}

@media (max-width: 960px) {
  .cart-body {
    grid-template-columns: 1fr;
  }

  .cart-summary {
    position: static;
    top: auto;
  }
}

@media (max-width: 720px) {
  .product-detail {
    padding: 18px 16px;
  }

  .detail-card {
    padding: 20px;
  }

  .cart-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .cart-item {
    grid-template-columns: 1fr;
    text-align: left;
  }

  .item-subtotal {
    text-align: left;
    min-width: auto;
  }

  .checkout-btn {
    width: 100%;
  }

  .summary-text,
  .summary-note {
    font-size: 14px;
  }

  .item-name {
    font-size: 16px;
  }
}

.detail-gallery,
.purchase-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.image-card {
  position: relative;
  background: #f7f9ff;
  border-radius: 18px;
  overflow: hidden;
  min-height: 460px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.pill {
  position: absolute;
  left: 18px;
  top: 18px;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 13px;
  background: rgba(64, 158, 255, 0.12);
  color: #409eff;
  font-weight: 600;
}

.detail-summary {
  padding: 22px 0 0;
}

.detail-summary h2 {
  font-size: 26px;
  line-height: 1.2;
  font-weight: 700;
  margin: 0 0 16px;
}

.summary-text {
  color: #4f566b;
  line-height: 1.75;
  font-size: 16px;
  margin: 0 0 24px;
}

.feature-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.feature-item {
  background: #f4f8ff;
  color: #3b4f83;
  padding: 12px 18px;
  border-radius: 14px;
  font-size: 14px;
}

.purchase-panel {
  background: #f9fafc;
  border-radius: 18px;
  padding: 24px;
  position: sticky;
  top: 30px;
}

.panel-title {
  color: #232d42;
  font-size: 18px;
  font-weight: 700;
}

.price-line {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin: 18px 0 12px;
}

.price {
  color: #e34b4b;
  font-size: 42px;
  font-weight: 800;
}

.market-price {
  color: #98a3b8;
  font-size: 16px;
  text-decoration: line-through;
}

.stock-status {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #5e687d;
  font-size: 14px;
}

.stock-status strong {
  color: #232d42;
}

.purchase-info {
  border-top: 1px solid rgba(56, 66, 85, 0.08);
  border-bottom: 1px solid rgba(56, 66, 85, 0.08);
  padding: 18px 0;
  display: grid;
  gap: 14px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #4f566b;
  font-size: 14px;
}

.info-label {
  color: #232d42;
  font-weight: 600;
}

.quantity-cart {
  display: grid;
  gap: 10px;
  margin: 12px 0 8px;
}

.quantity-cart label {
  font-weight: 600;
  color: #232d42;
}

.quantity-control {
  display: flex;
  align-items: center;
}

.quantity-control input {
  width: 100%;
  min-width: 100px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #d9e2ef;
  background: #fff;
  font-size: 15px;
  color: #232d42;
}

.actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 18px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  border-radius: 10px;
  padding: 14px 18px;
  font-size: 15px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: transform 0.12s, background 0.12s, box-shadow 0.12s;
}

.btn:active {
  transform: translateY(1px);
}

.btn-primary {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
  box-shadow: 0 10px 28px rgba(64, 158, 255, 0.18);
}

.btn-primary:hover {
  filter: brightness(0.94);
}

.btn-secondary {
  background: #fff;
  color: #232d42;
  border-color: #d9e2ef;
}

.btn-secondary:hover {
  background: #f5f7fb;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #687087;
}

.error.state {
  color: #d12d2d;
  padding: 22px;
  background: #fff3f3;
  border-radius: 14px;
}

.panel-footnote {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 20px;
}

.footnote-item {
  color: #4f566b;
  font-size: 14px;
  line-height: 1.6;
}

@media (max-width: 960px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .purchase-panel {
    position: static;
    top: auto;
  }
}

@media (max-width: 720px) {
  .product-detail {
    padding: 18px 16px;
  }

  .detail-card {
    padding: 20px;
  }

  .product-detail h1 {
    font-size: 24px;
  }

  .summary-text {
    font-size: 15px;
  }

  .price {
    font-size: 34px;
  }

  .actions {
    grid-template-columns: 1fr;
  }
}
</style>
