<template>
  <div class="page-shell ai-search">
    <div class="page-header">
      <div>
        <span class="eyebrow">AI Smart Search</span>
        <h1>AI 搜索</h1>
        <p class="page-subtitle">用自然语言描述您想要的商品，AI 为您智能匹配。</p>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <input
        v-model="query"
        type="text"
        placeholder="例如：适合跑步用的防水耳机、学生党性价比手机..."
        @keydown.enter="doSearch"
      />
      <button class="btn btn-primary" :disabled="!query.trim() || loading" @click="doSearch">
        {{ loading ? '搜索中...' : 'AI 搜索' }}
      </button>
    </div>

    <!-- 搜索元信息：模式标识 + 结果数 -->
    <div v-if="searched" class="search-meta">
      <span v-if="result" class="pill" :class="result.fallback ? 'pill-template' : 'pill-ai'">
        {{ result.fallback ? '关键词匹配' : 'AI 智能搜索' }}
      </span>
      <span v-if="result && result.products" class="result-count">
        找到 {{ result.products.length }} 个匹配商品
      </span>
    </div>

    <!-- 四种状态的条件渲染 -->
    <div v-if="loading" class="state">AI 正在分析您的搜索意图...</div>

    <div v-else-if="result && result.products && result.products.length > 0" class="results-grid">
      <div v-for="product in result.products" :key="product.id" class="result-card">
        <div class="result-header">
          <h3>{{ product.name }}</h3>
          <span class="result-price">¥{{ product.price }}</span>
        </div>
        <p class="result-reason">{{ product.reason }}</p>
        <router-link :to="`/product/${product.id}`" class="btn btn-secondary compact">
          查看详情
        </router-link>
      </div>
    </div>

    <div v-else-if="searched && !loading" class="empty-state">
      <p>没有找到匹配的商品，换个关键词试试？</p>
    </div>

    <!-- 搜索示例（初始状态） -->
    <div v-else class="search-hints">
      <h3>搜索示例</h3>
      <div class="hint-tags">
        <button v-for="hint in searchHints" :key="hint" class="hint-tag" @click="query = hint">
          {{ hint }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import http from '../utils/http'

const searchHints = [
  '适合跑步用的防水耳机',
  '学生党性价比手机',
  '智能家居入门套装',
  '办公用笔记本电脑',
  '运动健身装备',
  '居家生活好物'
]

export default {
  name: 'AiSearchView',
  setup() {
    const query = ref('')        // 搜索关键词
    const result = ref(null)     // 搜索结果
    const loading = ref(false)   // 加载状态
    const searched = ref(false)  // 是否已搜索（区分初始状态和无结果）

    /**
     * 执行搜索
     * 流程：校验 → 设置状态 → POST /ai/search → 保存结果
     */
    const doSearch = async () => {
      if (!query.value.trim() || loading.value) return

      loading.value = true
      searched.value = true
      result.value = null

      try {
        const response = await http.post('/ai/search', { query: query.value.trim() })
        result.value = response.data
      } catch (err) {
        result.value = null
        // 错误处理：可加 ElMessage.error(err.message)
      } finally {
        loading.value = false
      }
    }

    return { query, result, loading, searched, searchHints, doSearch }
  }
}
</script>

<style scoped>
.ai-search { max-width: 900px; margin: 0 auto; padding: 20px; }
.search-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.search-bar input {
  flex: 1;
  padding: 13px 14px;
  border: 1px solid #ddd;
  border-radius: 15px;
  font-size: 16px;
}
.btn {
  padding: 13px 24px;
  border: none;
  border-radius: 15px;
  cursor: pointer;
  font-weight: 700;
}
.btn-primary { background: #4f7cff; color: #fff; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary { background: #ecf5ff; color: #4f7cff; border: 1px solid #b3d8ff; }
.compact { padding: 8px 16px; font-size: 13px; }
.search-meta { display: flex; gap: 12px; align-items: center; padding: 0 8px; margin-bottom: 16px; }
.pill { display: inline-flex; padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.pill-template { background: rgba(245,158,11,0.12); color: #b45309; }
.pill-ai { background: rgba(79,124,255,0.12); color: #4f7cff; }
.result-count { color: #666; font-size: 14px; }
.state { text-align: center; padding: 60px 20px; color: #666; }
.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 18px;
}
.result-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 22px;
  border: 1px solid #eee;
  border-radius: 18px;
  background: #fff;
  transition: transform 0.22s;
}
.result-card:hover { transform: translateY(-4px); }
.result-header { display: flex; justify-content: space-between; gap: 12px; }
.result-header h3 { font-size: 18px; flex: 1; }
.result-price { font-size: 20px; font-weight: 800; color: #4f7cff; }
.result-reason { color: #666; line-height: 1.7; font-size: 14px; }
.empty-state { text-align: center; padding: 60px 20px; color: #666; }
.search-hints {
  padding: 30px;
  border: 1px solid #eee;
  border-radius: 20px;
  background: #fff;
}
.search-hints h3 { margin-bottom: 16px; }
.hint-tags { display: flex; flex-wrap: wrap; gap: 10px; }
.hint-tag {
  padding: 10px 18px;
  border: 1px solid #ddd;
  border-radius: 999px;
  background: #fff;
  cursor: pointer;
}
.hint-tag:hover { border-color: #4f7cff; color: #4f7cff; }
</style>
