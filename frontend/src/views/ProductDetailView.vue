<template>
  <div class="product-detail">
    <div class="detail-header">
      <div>
        <router-link to="/products" class="breadcrumb">‹ 返回商品列表</router-link>
        <h1>商品详情</h1>
        <p class="subtitle">请确认好商品的信息和数量后加入购物车</p>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error state">{{ error }}</div>
    <div v-else-if="product" class="detail-card">
      <div class="detail-layout">
        <section class="detail-gallery">
          <div class="image-card">
            <img
              :src="resolveImage(product)"
              :alt="product.name"
              class="main-image"
            />
            <span class="pill pill-info">{{ getCategoryLabel(product.categoryId) }}</span>
          </div>

          <div class="detail-summary">
            <h2>{{ product.name }}</h2>
            <p class="summary-text">{{ product.description }}</p>
            <div class="feature-list">
              <div class="feature-item">全国包邮</div>
              <div class="feature-item">7天无理由退换</div>
              <div class="feature-item">48小时内发货</div>
            </div>
          </div>
        </section>

        <aside class="purchase-panel">
          <div class="panel-title">立即购买</div>
          <div class="price-line">
            <strong class="price">￥{{ product.price }}</strong>
            <span class="market-price" v-if="product.marketPrice">￥{{ product.marketPrice }}</span>
          </div>
          <div class="stock-status">
            <span>库存</span>
            <strong>{{ product.stock }}</strong>
            <span>件</span>
          </div>

          <div class="purchase-info">
            <div class="info-row">
              <span class="info-label">配送</span>
              <span>全国包邮</span>
            </div>
            <div class="info-row">
              <span class="info-label">发货地</span>
              <span>上海</span>
            </div>
            <div class="info-row">
              <span class="info-label">售后</span>
              <span>7天无理由退货</span>
            </div>
          </div>

          <div class="quantity-cart">
            <label for="quantity">购买数量</label>
            <div class="quantity-control">
              <input
                id="quantity"
                v-model.number="quantity"
                type="number"
                min="1"
                :max="product.stock"
              />
            </div>
          </div>

          <div class="actions">
            <button class="btn btn-secondary" @click="addToCart">加入购物车</button>
            <button class="btn btn-primary" @click="buyNow">立刻下单</button>
          </div>

          <div class="panel-footnote">
            <div class="footnote-item">安心购物，支持7天无理由退货</div>
            <div class="footnote-item">快速发货，实时库存更新</div>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import http from "../utils/http";
import { isLoggedIn } from "../utils/auth";
import { getCategoryLabel, resolveProductImage } from "../utils/productCatalog";
import { alertMessage } from "../utils/modal";

export default {
  name: "ProductDetailView",
  setup() {
    // 1、路由
    // 获取当前路由信息：route.params.id --> URL中的商品id
    const route = useRoute();
    // 用于编程式导航：router.push('login')
    const router = useRouter();
    // 2、响应式状态
    // 商品数据
    const product = ref(null);
    // 购买数量
    const quantity = ref(1);
    // 加载状态
    const loading = ref(true);
    // 错误状态
    const error = ref("");

    // 3、工具函数
    // 登录检查
    const ensureLogin = () => {
      if (isLoggedIn()) return true;
      router.push("/login");
      return false;
    };

    const resolveImage = (product) => resolveProductImage(product);

    // 获取商品详情
    const fetchProduct = async () => {
      loading.value = true;
      error.value = "";
      try {
        const response = await http.get(`/products/${route.params.id}`);
        // 避免 data 为空时展示无效信息
        product.value = response.data || null;
      } catch (err) {
        // 4. catch 中 console.error('加载商品失败', e)
        error.value = err.message || "获取商品详情失败";
      } finally {
        // 5. finally 中 loading.value = false
        loading.value = false;
      }
    };

  

    // 加入购物车
    const addToCart = async () => {
      // 判断是否登录
      if (!ensureLogin()) return;
      if (!product.value) {
        await alertMessage("商品信息未加载，请稍后再试");
        return;
      }
      try {
        await http.post("/cart/items", {
          productId: product.value.id,
          quantity: normalizedQuantity(),
        });
        await alertMessage("已加入购物车");
      } catch (err) {
        await alertMessage(err.message || "加入购物车失败");
      }
    };
    // 数量规范化，保证数量的有效的正整数
    const normalizedQuantity = () => {
      const current = Number(quantity.value || 1);
      return current > 0 ? current : 1;
    };

    // 立即下单
    const buyNow = async () => {
      if (!ensureLogin()) return;
      if (!product.value) {
        await alertMessage("商品信息未加载，请稍后再试");
        return;
      }
      try {
        await http.post("/cart/items", {
          productId: product.value.id,
          quantity: normalizedQuantity(),
        });
        router.push("/checkout");
      } catch (err) {
        await alertMessage(err.message || "创建订单失败");
      }
    };

    onMounted(fetchProduct);
    
    return {
      product,
      quantity,
      loading,
      error,
      resolveImage,
      getCategoryLabel,
      addToCart,
      buyNow,
    };
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

.detail-layout {
  display: grid;
  grid-template-columns: 1.4fr 0.9fr;
  gap: 28px;
  align-items: start;
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
