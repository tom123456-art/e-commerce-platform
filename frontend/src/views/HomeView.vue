<template>
    <div class="home">
        <!-- 显示所有商品 -->
        <section class="section">
          <h2>全部商品</h2>
          <p class="section-desc">精选好物，品质保证</p>
          <div v-if="loading" class="loading">
              加载中...
          </div>
          <div v-else class="product-grid">
              <div v-for="product in products" :key="product.id" 
              class="product-card" @click="goToProduct(product.id)">
                <img :src="resolveImage(product)" :alt="product.name"
                class="product-img" />
                <div class="product-info">
                    <h3>{{ product.name }}</h3>
                    <p class="product-price">
                      ￥ {{ formatPrice(product.price) }}
                    </p>
                </div>
              </div>
          </div>
        </section>
    </div>
</template>

<script setup>
import {ref, onMounted} from 'vue'
import {useRouter} from 'vue-router'

import {formatPrice} from '../utils/formatters'
import {resolveProductImage } from '../utils/productCatalog'
import http from '../utils/http';


const router = useRouter()
const loading = ref(true)
const products = ref([])

const resolveImage = (product) => resolveProductImage(product)

const goToProduct = (id) => router.push(`/products/${id}`)


onMounted(async () => {
  try {
    const res = await http.get('/products')
    products.value = res.data || []
  } catch (e) {
    console.error("加载商品失败", e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.home {
  text-align: center;
  padding: 40px 0;
}

.home h1 {
  font-size: 32px;
  color: #333;
  margin-bottom: 10px;
}

.home p {
  color: #666;
  margin-bottom: 40px;
}

.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.feature-card {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.feature-card h3 {
  font-size: 20px;
  margin-bottom: 10px;
}

.feature-card p {
  color: #666;
}
</style>