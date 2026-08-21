<template>
  <div class="page-shell ai-recommend">
    <div class="page-header">
      <div>
        <span class="eyebrow">AI Smart Recommend</span>
        <h1>AI 推荐</h1>
        <p class="page-subtitle">描述您的需求和预算，AI 为您精选最合适的商品。</p>
      </div>
    </div>

    <!-- 推荐表单：需求 + 预算 + 偏好分类 -->
    <div class="recommend-form">
      <div class="form-group">
        <label>描述您的需求</label>
        <textarea v-model="form.query" rows="3"
          placeholder="例如：我是大学生，需要一台能编程和做设计的笔记本电脑..." />
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>预算（可选）</label>
          <!-- v-model.number：自动转为数字 -->
          <input v-model.number="form.budget" type="number" min="0" placeholder="元">
        </div>
        <div class="form-group">
          <label>偏好分类（可选）</label>
          <select v-model="form.categoryPreference">
            <option value="">不限分类</option>
            <option v-for="cat in categories" :key="cat.value" :value="cat.value">{{ cat.label }}</option>
          </select>
        </div>
      </div>
      <div class="form-actions">
        <button class="btn btn-primary" :disabled="!form.query.trim() || loading" @click="doRecommend">
          {{ loading ? 'AI 分析中...' : '获取推荐' }}
        </button>
        <button class="btn btn-secondary" type="button" @click="resetForm">清空</button>
      </div>
    </div>

    <div v-if="loading" class="state">AI 正在根据您的需求筛选商品...</div>

    <!-- 推荐结果 -->
    <div v-else-if="result && result.recommendations && result.recommendations.length > 0" class="recommend-results">
      <div class="results-header">
        <h2>为您推荐</h2>
        <span v-if="result" class="pill" :class="result.fallback ? 'pill-template' : 'pill-ai'">
          {{ result.fallback ? '智能筛选' : 'AI 推荐' }}
        </span>
      </div>

      <div class="results-grid">
        <div v-for="(item, index) in result.recommendations" :key="item.id" class="recommend-card">
          <div class="card-rank">#{{ index + 1 }}</div>
          <div class="card-body">
            <div class="card-header">
              <h3>{{ item.name }}</h3>
              <span class="card-price">¥{{ item.price }}</span>
            </div>
            <div class="card-meta">
              <!-- 动态 class：根据分数切换颜色 -->
              <span class="card-score" :class="getScoreClass(item.score)">
                推荐指数 {{ item.score }}/100
              </span>
            </div>
            <p class="card-reason">{{ item.reason }}</p>
            <router-link :to="`/products/${item.id}`" class="btn btn-secondary compact">
              查看详情
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="searched && !loading" class="empty-state">
      <p>没有找到合适的推荐，请调整需求描述或预算后重试。</p>
    </div>

    <!-- 推荐示例（初始状态） -->
    <div v-else class="recommend-examples">
      <h3>推荐示例</h3>
      <div class="example-cards">
        <div v-for="ex in examples" :key="ex.query" class="example-card" @click="fillExample(ex)">
          <strong>{{ ex.title }}</strong>
          <p>{{ ex.query }}</p>
          <span v-if="ex.budget">预算：{{ ex.budget }}元</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { reactive, ref } from 'vue'
import http from '../utils/http'

const categories = [
  { value: '手机数码', label: '手机数码' },
  { value: '电脑办公', label: '电脑办公' },
  { value: '智能家电', label: '智能家电' },
  { value: '居家生活', label: '居家生活' },
  { value: '运动户外', label: '运动户外' },
  { value: '影音娱乐', label: '影音娱乐' }
]

const examples = [
  { title: '学生党笔记本', query: '我是大学生，需要一台能编程和做设计的笔记本电脑', budget: 5000 },
  { title: '运动耳机', query: '喜欢跑步和健身，想要一款防水防汗的无线耳机', budget: 500 },
  { title: '智能家居入门', query: '想给家里添置一些智能家居产品，提升生活品质', budget: 2000 },
  { title: '送礼推荐', query: '想给父母买一些实用的数码产品，操作要简单', budget: 3000 }
]

export default {
  name: 'AiRecommendView',
  setup() {
    // reactive：多字段表单适合用 reactive（直接访问属性，不需要 .value）
    const form = reactive({
      query: '',
      budget: null,
      categoryPreference: ''
    })
    const result = ref(null)
    const loading = ref(false)
    const searched = ref(false)

    /**
     * 根据分数返回 CSS 类名（动态 class 绑定）
     * >= 80：绿色（强推荐）
     * >= 60：黄色（中等推荐）
     * < 60：灰色（弱推荐）
     */
    const getScoreClass = (score) => {
      if (score >= 80) return 'score-high'
      if (score >= 60) return 'score-mid'
      return 'score-low'
    }

    /**
     * 获取推荐
     * 条件构建请求参数：只有填了的字段才加入 payload
     */
    const doRecommend = async () => {
      if (!form.query.trim() || loading.value) return

      loading.value = true
      searched.value = true
      result.value = null

      try {
        const payload = { query: form.query.trim() }
        if (form.budget) payload.budget = form.budget                    // 可选字段
        if (form.categoryPreference) payload.categoryPreference = form.categoryPreference

        const response = await http.post('/ai/recommend', payload)
        result.value = response.data
      } catch (err) {
        result.value = null
      } finally {
        loading.value = false
      }
    }

    const resetForm = () => {
      form.query = ''
      form.budget = null
      form.categoryPreference = ''
      result.value = null
      searched.value = false
    }

    const fillExample = (ex) => {
      form.query = ex.query
      form.budget = ex.budget || null
    }

    return {
      form, result, loading, searched,
      categories, examples,
      getScoreClass, doRecommend, resetForm, fillExample
    }
  }
}
</script>

<style scoped>
.ai-recommend { max-width: 900px; margin: 0 auto; padding: 20px; }
.recommend-form {
  padding: 24px;
  border: 1px solid #eee;
  border-radius: 20px;
  background: #fff;
  margin-bottom: 20px;
}
.form-group { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
.form-group label { font-weight: 800; }
.form-group textarea, .form-group input, .form-group select {
  padding: 13px 14px;
  border: 1px solid #ddd;
  border-radius: 15px;
  font: inherit;
}
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-actions { display: flex; gap: 12px; }
.btn { padding: 13px 24px; border: none; border-radius: 15px; cursor: pointer; font-weight: 700; }
.btn-primary { background: #4f7cff; color: #fff; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary { background: #ecf5ff; color: #4f7cff; border: 1px solid #b3d8ff; }
.compact { padding: 8px 16px; font-size: 13px; }
.state { text-align: center; padding: 60px 20px; color: #666; }
.recommend-results { display: flex; flex-direction: column; gap: 18px; }
.results-header { display: flex; align-items: center; gap: 14px; }
.results-header h2 { font-size: 24px; }
.pill { display: inline-flex; padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.pill-template { background: rgba(245,158,11,0.12); color: #b45309; }
.pill-ai { background: rgba(79,124,255,0.12); color: #4f7cff; }
.results-grid { display: flex; flex-direction: column; gap: 16px; }
.recommend-card {
  display: flex;
  gap: 18px;
  padding: 22px;
  border: 1px solid #eee;
  border-radius: 18px;
  background: #fff;
}
.card-rank {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px; height: 48px;
  border-radius: 14px;
  background: rgba(79,124,255,0.1);
  color: #4f7cff;
  font-size: 20px;
  font-weight: 800;
}
.card-body { flex: 1; display: flex; flex-direction: column; gap: 10px; }
.card-header { display: flex; justify-content: space-between; gap: 12px; }
.card-header h3 { font-size: 18px; flex: 1; }
.card-price { font-size: 22px; font-weight: 800; color: #4f7cff; }
.card-meta { display: flex; gap: 10px; }
.card-score { padding: 4px 10px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.score-high { background: rgba(34,197,94,0.12); color: #15803d; }
.score-mid { background: rgba(245,158,11,0.12); color: #b45309; }
.score-low { background: rgba(148,163,184,0.12); color: #64748b; }
.card-reason { color: #666; line-height: 1.7; font-size: 14px; }
.empty-state { text-align: center; padding: 60px 20px; color: #666; }
.recommend-examples {
  padding: 30px;
  border: 1px solid #eee;
  border-radius: 20px;
  background: #fff;
}
.recommend-examples h3 { margin-bottom: 16px; }
.example-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}
.example-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px;
  border: 1px solid #ddd;
  border-radius: 18px;
  background: #fff;
  cursor: pointer;
}
.example-card:hover { border-color: #4f7cff; }
.example-card strong { font-size: 16px; }
.example-card p { color: #666; font-size: 14px; line-height: 1.6; }
.example-card span { color: #4f7cff; font-size: 13px; font-weight: 700; }
</style>
