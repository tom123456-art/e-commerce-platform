<template>
  <div class="dashboard">
    <h2 class="mc-title">数据看板</h2>
    <div class="stats-grid">
      <div class="stat-card clickable" @click="router.push('/products')">
        <div class="stat-value">{{ stats.totalProducts }}</div>
        <div class="stat-label">商品总数</div>
      </div>
      <div class="stat-card clickable" @click="router.push('/products')">
        <div class="stat-value">{{ stats.activeProducts }}</div>
        <div class="stat-label">在售商品</div>
      </div>
      <div class="stat-card clickable" @click="router.push('/reviews')">
        <div class="stat-value">{{ stats.totalReviews }}</div>
        <div class="stat-label">评论总数</div>
      </div>
      <div class="stat-card clickable" @click="router.push('/reviews')">
        <div class="stat-value">{{ stats.pendingReplies }}</div>
        <div class="stat-label">待回复</div>
      </div>
      <div class="stat-card accent clickable" @click="router.push('/reviews')">
        <div class="stat-value">{{ stats.averageRating }}</div>
        <div class="stat-label">平均评分</div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 商家数据看板视图
// 展示 5 个核心指标：商品总数、在售商品、评论总数、待回复、平均评分
// 点击卡片可跳转到对应的详情页面（商品管理 / 评论管理）

import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../utils/http'

const router = useRouter()

// 响应式统计数据对象
const stats = ref({
  totalProducts: 0,        // 商品总数
  activeProducts: 0,       // 在售商品
  totalReviews: 0,         // 评论总数
  pendingReplies: 0,       // 待回复
  averageRating: 0,        // 平均评分
  totalOrders: 0,          // 订单总数
  totalRevenue: 0          // 总营收
})

const loading = ref(false)

// 获取统计数据：调用后端 /api/merchant/dashboard 接口
async function fetchStats() {
  loading.value = true
  try {
    const res = await http.get("/merchant/dashboard")
    if(res.data){
      stats.value = res.data
    }
  } catch (error) {
    console.error('获取统计数据失败：', error)
  } finally {
    loading.value = false
  }
}

// 组件挂载时自动获取数据
onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.dashboard h2.mc-title { margin: 0 0 24px; color: #303133; }

/* CSS Grid 自适应网格：在不同屏幕尺寸下自动调整列数 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.stat-card {
  background: #fff;
  padding: 28px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

/* 可点击卡片：悬停上浮、按下回弹 */
.stat-card.clickable {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.stat-card.clickable:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}
.stat-card.clickable:active {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.stat-card.accent {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.stat-value { font-size: 36px; font-weight: 700; margin-bottom: 8px; }
.stat-card.accent .stat-value { color: #fff; }
.stat-label { font-size: 14px; color: #999; }
.stat-card.accent .stat-label { color: rgba(255, 255, 255, 0.8); }
</style>
