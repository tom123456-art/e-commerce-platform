<template>
  <!--
    ============================================================
    【AdminShowcaseStrategyView.vue 模板教学注释】
    ============================================================

    1. 组件定位：
       AdminShowcaseStrategyView 是推荐策略管理页面，
       用于配置商城首页的商品推荐算法参数。这是电商后台的
       高级功能，允许管理员精细化控制推荐行为。

    2. 推荐策略模式：
       系统支持两种推荐策略模式：
       - MANUAL（手动模式）：管理员手动设置各项权重
       - AUTO（自动模式）：系统根据历史数据自动微调权重
       管理员可以随时切换模式。

    3. 权重配置体系：
       推荐算法涉及多个维度的权重配置：
       - 热销榜权重：控制热销商品的排序因素
       - 匿名推荐权重：用于未登录用户的推荐
       - 个性化推荐权重：用于登录用户的推荐
       - 热度信号权重：影响推荐中的热度计算

    4. 数据统计展示：
       页面展示两个时间窗口的统计数据：
       - 短周期（默认 7 天）：近期数据，反映短期趋势
       - 长周期（默认 30 天）：长期数据，反映整体表现
       统计指标包括浏览量、加购量、支付订单、支付率等。

    5. 响应式表单：
       使用 Vue 3 的 reactive() 创建响应式表单对象。
       与 ref() 不同，reactive() 适合创建复杂的嵌套对象，
       访问属性时无需写 .value。
  -->
  <div class="page-shell strategy-page">
    <!-- 页面头部：标题 + 操作按钮 -->
    <div class="page-header">
      <div>
        <span class="eyebrow">Showcase Strategy</span>
        <h1>推荐策略</h1>
        <p class="page-subtitle">支持手动调权、浏览埋点统计，以及按近 7/30 天表现自动微调。</p>
      </div>
      <div class="header-actions">
        <!-- 刷新按钮：重新加载策略配置 -->
        <button class="btn btn-secondary" :disabled="loading" @click="loadStrategy">刷新</button>
        <!--
          自动微调按钮：
          - :disabled="saving || loading || form.mode !== 'AUTO'"
          - 只有在自动模式下才能手动触发微调
          - 点击后调用后端接口执行自动微调算法
        -->
        <button
          class="btn btn-secondary"
          :disabled="saving || loading || form.mode !== 'AUTO'"
          @click="runAutoTune"
        >
          立即自动微调
        </button>
        <!-- 保存按钮：保存当前配置 -->
        <button class="btn btn-primary" :disabled="saving || loading" @click="saveStrategy">保存配置</button>
      </div>
    </div>

    <!-- 错误信息显示 -->
    <p v-if="error" class="state error">{{ error }}</p>

    <!--
      策略模式配置区域：
      - 模式切换（手动/自动）
      - 时间窗口配置
      - 加购偏好权重
    -->
    <section class="strategy-card">
      <div class="section-heading">
        <h2>策略模式</h2>
        <!--
          模式标签：
          - 根据 form.mode 动态切换样式和文本
          - mode-auto：自动模式（绿色）
          - mode-manual：手动模式（蓝色）
        -->
        <span class="mode-pill" :class="form.mode === 'AUTO' ? 'mode-auto' : 'mode-manual'">
          {{ form.mode === 'AUTO' ? '自动模式' : '手动模式' }}
        </span>
      </div>

      <!--
        策略配置表单：
        使用原生 <label> + <select>/<input> 构建表单。
        v-model.number 双向绑定，自动转换为数字类型。
      -->
      <div class="form-grid">
        <label class="field">
          <span>模式</span>
          <select v-model="form.mode">
            <option value="MANUAL">手动模式</option>
            <option value="AUTO">自动模式</option>
          </select>
        </label>
        <label class="field">
          <span>短周期窗口（天）</span>
          <input v-model.number="form.shortWindowDays" type="number" min="1" max="30">
        </label>
        <label class="field">
          <span>长周期窗口（天）</span>
          <input v-model.number="form.longWindowDays" type="number" min="7" max="180">
        </label>
        <label class="field">
          <span>加购偏好权重</span>
          <!--
            加购偏好权重输入框：
            - min="0" max="1" step="0.01"：限制在 0-1 之间，步长 0.01
            - :disabled="weightInputsDisabled"：自动模式下禁用手动输入
          -->
          <input
            v-model.number="form.cartPreferenceWeight"
            type="number"
            min="0"
            max="1"
            step="0.01"
            :disabled="weightInputsDisabled"
          >
        </label>
      </div>

      <!-- 提示信息 -->
      <p class="hint">
        自动模式下会基于近 {{ form.shortWindowDays || 7 }}/{{ form.longWindowDays || 30 }} 天销量、浏览、加购和支付率做平滑微调，
        管理员可随时切回手动模式并直接改权重。
      </p>

      <!-- 元信息：最近操作时间 -->
      <div class="meta-row">
        <span>最近自动微调：{{ formatDateTime(strategy.lastAutoTunedAt) }}</span>
        <span>最近配置更新时间：{{ formatDateTime(strategy.updateTime) }}</span>
      </div>
    </section>

    <!--
      统计数据概览：
      展示短周期和长周期两个时间窗口的统计数据。
      每个窗口包含 8 个指标：浏览量、加购量、支付订单等。
    -->
    <section class="summary-grid">
      <!-- 短周期统计卡片 -->
      <article class="summary-card">
        <div class="section-heading">
          <h2>近 {{ strategy.shortWindowSummary?.windowDays || form.shortWindowDays || 7 }} 天</h2>
        </div>
        <div class="metrics-grid">
          <div class="metric-item">
            <span>浏览量</span>
            <strong>{{ formatInteger(strategy.shortWindowSummary?.viewCount) }}</strong>
          </div>
          <div class="metric-item">
            <span>加购量</span>
            <strong>{{ formatInteger(strategy.shortWindowSummary?.cartAddCount) }}</strong>
          </div>
          <div class="metric-item">
            <span>支付订单</span>
            <strong>{{ formatInteger(strategy.shortWindowSummary?.paidOrderCount) }}</strong>
          </div>
          <div class="metric-item">
            <span>支付件数</span>
            <strong>{{ formatInteger(strategy.shortWindowSummary?.paidQuantity) }}</strong>
          </div>
          <div class="metric-item">
            <span>支付金额</span>
            <strong>¥{{ formatAmount(strategy.shortWindowSummary?.paidAmount) }}</strong>
          </div>
          <div class="metric-item">
            <span>浏览转加购</span>
            <strong>{{ formatPercent(strategy.shortWindowSummary?.viewToCartRate) }}</strong>
          </div>
          <div class="metric-item">
            <span>浏览转支付</span>
            <strong>{{ formatPercent(strategy.shortWindowSummary?.paymentRate) }}</strong>
          </div>
          <div class="metric-item">
            <span>加购转支付</span>
            <strong>{{ formatPercent(strategy.shortWindowSummary?.cartPaymentRate) }}</strong>
          </div>
        </div>
      </article>

      <!-- 长周期统计卡片 -->
      <article class="summary-card">
        <div class="section-heading">
          <h2>近 {{ strategy.longWindowSummary?.windowDays || form.longWindowDays || 30 }} 天</h2>
        </div>
        <div class="metrics-grid">
          <div class="metric-item">
            <span>浏览量</span>
            <strong>{{ formatInteger(strategy.longWindowSummary?.viewCount) }}</strong>
          </div>
          <div class="metric-item">
            <span>加购量</span>
            <strong>{{ formatInteger(strategy.longWindowSummary?.cartAddCount) }}</strong>
          </div>
          <div class="metric-item">
            <span>支付订单</span>
            <strong>{{ formatInteger(strategy.longWindowSummary?.paidOrderCount) }}</strong>
          </div>
          <div class="metric-item">
            <span>支付件数</span>
            <strong>{{ formatInteger(strategy.longWindowSummary?.paidQuantity) }}</strong>
          </div>
          <div class="metric-item">
            <span>支付金额</span>
            <strong>¥{{ formatAmount(strategy.longWindowSummary?.paidAmount) }}</strong>
          </div>
          <div class="metric-item">
            <span>浏览转加购</span>
            <strong>{{ formatPercent(strategy.longWindowSummary?.viewToCartRate) }}</strong>
          </div>
          <div class="metric-item">
            <span>浏览转支付</span>
            <strong>{{ formatPercent(strategy.longWindowSummary?.paymentRate) }}</strong>
          </div>
          <div class="metric-item">
            <span>加购转支付</span>
            <strong>{{ formatPercent(strategy.longWindowSummary?.cartPaymentRate) }}</strong>
          </div>
        </div>
      </article>
    </section>

    <!--
      权重配置区域：
      使用 sections 数组动态渲染多个权重配置卡片。
      每个卡片包含一组相关的权重输入框。
    -->
    <section class="weight-grid">
      <article v-for="section in sections" :key="section.key" class="strategy-card">
        <div class="section-heading">
          <h2>{{ section.label }}</h2>
          <span class="section-desc">{{ section.description }}</span>
        </div>
        <div class="weights-list">
          <!--
            权重输入框：
            - v-model.number="form[section.key][field.key]"：动态路径绑定
            - section.key：如 'hot'、'anonymous'、'personalized'
            - field.key：如 'sales'、'revenue'、'orders'
            - :disabled="weightInputsDisabled"：自动模式下禁用
          -->
          <label v-for="field in section.fields" :key="field.key" class="field">
            <span>{{ field.label }}</span>
            <input
              v-model.number="form[section.key][field.key]"
              type="number"
              min="0"
              step="0.01"
              :disabled="weightInputsDisabled"
            >
          </label>
        </div>
      </article>
    </section>

    <!--
      日聚合数据表格：
      展示最近几天的每日统计数据。
      使用原生 HTML table，包含日期、浏览量、加购量等列。
    -->
    <section class="strategy-card">
      <div class="section-heading">
        <h2>近日日聚合</h2>
        <span class="section-desc">按商品浏览、加购、支付成功实时写入，按天汇总展示。</span>
      </div>
      <!--
        条件渲染：
        - 有数据：显示表格
        - 无数据：显示空状态提示
      -->
      <div v-if="strategy.recentDailyMetrics?.length" class="table-wrap">
        <table class="metric-table">
          <thead>
            <tr>
              <th>日期</th>
              <th>浏览量</th>
              <th>加购量</th>
              <th>支付订单</th>
              <th>支付件数</th>
              <th>支付金额</th>
              <th>支付率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in strategy.recentDailyMetrics" :key="item.metricDate">
              <td>{{ item.metricDate }}</td>
              <td>{{ formatInteger(item.viewCount) }}</td>
              <td>{{ formatInteger(item.cartAddCount) }}</td>
              <td>{{ formatInteger(item.paidOrderCount) }}</td>
              <td>{{ formatInteger(item.paidQuantity) }}</td>
              <td>¥{{ formatAmount(item.paidAmount) }}</td>
              <td>{{ formatPercent(item.paymentRate) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="empty-state">暂时还没有可用的浏览/加购/支付统计数据。</p>
    </section>
  </div>
</template>

<script>
/**
 * ============================================================
 * 【AdminShowcaseStrategyView.vue 脚本教学注释】
 * ============================================================
 *
 * 1. 组件设计模式 — 配置管理页：
 *    这是管理后台中"系统配置"类页面的标准模式：
 *    - 页面加载时从后端获取当前配置
 *    - 将配置数据填充到表单中
 *    - 用户修改表单后点击"保存"提交
 *    - 支持"重置"功能恢复到初始状态
 *
 * 2. reactive() vs ref()：
 *    本组件使用 reactive() 创建表单数据：
 *    - reactive()：适合创建复杂的嵌套对象，访问属性无需 .value
 *    - ref()：适合创建简单值（字符串、数字、布尔），访问需要 .value
 *    - 本组件的 form 对象有深层嵌套（form.hot.sales），用 reactive 更方便
 *
 * 3. 配置数据合并 — applyForm：
 *    从后端获取的配置可能不完整（缺少某些字段），
 *    applyForm 函数将后端数据与默认值深度合并：
 *    - 外层属性：{ ...default, ...payload }
 *    - 内层对象：{ ...default.hot, ...payload.hot }
 *    确保所有字段都有值，避免 undefined 导致的错误。
 *
 * 4. 条件禁用 — weightInputsDisabled：
 *    computed 属性，当 form.mode === 'AUTO' 时返回 true。
 *    自动模式下，权重输入框被禁用，防止用户手动修改
 *    被系统自动管理的权重值。
 *
 * 5. 数据格式化函数：
 *    - formatInteger：数字格式化为千分位（1,234,567）
 *    - formatAmount：金额格式化为 2 位小数
 *    - formatPercent：小数格式化为百分比（0.1234 → 12.34%）
 *    - formatDateTime：日期时间格式化为本地字符串
 *
 * ※ 练习骨架说明：
 *    下方 applyForm / loadStrategy / buildPayload / saveStrategy / runAutoTune
 *    五个核心业务函数已实现完整逻辑；createDefaultForm、sections、
 *    weightInputsDisabled 以及四个 format* 格式化函数保持不变。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import http from '../../utils/http'
import { alertMessage } from '../../utils/modal'
import { formatPrice, formatDate } from '../../utils/formatters';

/**
 * 默认表单数据工厂函数 — createDefaultForm
 *
 * 返回推荐策略的默认配置值：
 * - mode：策略模式（MANUAL 手动 / AUTO 自动）
 * - shortWindowDays：短周期窗口天数（默认 7 天）
 * - longWindowDays：长周期窗口天数（默认 30 天）
 * - cartPreferenceWeight：加购偏好权重（默认 0.6）
 * - hot：热销榜权重配置
 * - anonymous：匿名推荐权重配置
 * - personalized：个性化推荐权重配置
 * - hotSignal：热度信号权重配置
 *
 * 使用工厂函数的原因：
 * - 每次调用返回全新的对象
 * - 避免多个引用指向同一个对象
 * - 在重置表单和初始化时调用
 */
const createDefaultForm = () => ({
  mode: 'MANUAL',
  shortWindowDays: 7,
  longWindowDays: 30,
  cartPreferenceWeight: 0.6,
  hot: {
    sales: 0.55,
    revenue: 0.15,
    orders: 0.15,
    freshness: 0.1,
    inventory: 0.05
  },
  anonymous: {
    hot: 0.5,
    freshness: 0.25,
    inventory: 0.15,
    affordability: 0.1
  },
  personalized: {
    category: 0.5,
    hot: 0.25,
    price: 0.1,
    freshness: 0.1,
    inventory: 0.05
  },
  hotSignal: {
    sales: 0.5,
    revenue: 0.2,
    orders: 0.2,
    freshness: 0.1
  }
})

export default {
  name: 'AdminShowcaseStrategyView',
  setup() {
    /**
     * 响应式状态定义：
     * - strategy：从后端获取的完整策略数据（包含统计信息）
     * - form：表单数据（reactive 对象，用于双向绑定）
     * - loading：是否正在加载数据
     * - saving：是否正在保存数据
     * - error：错误信息
     */
    const strategy = ref({
      shortWindowSummary: null,
      longWindowSummary: null,
      recentDailyMetrics: []
    })
    const form = reactive(createDefaultForm())
    const loading = ref(false)
    const saving = ref(false)
    const error = ref('')

    /**
     * 权重配置区域定义 — sections
     *
     * 静态配置数组，定义了 4 个权重配置区域：
     * - hot：热销榜权重（销量、营收、订单、新鲜度、库存）
     * - anonymous：匿名推荐权重（热度、新鲜度、库存、价格友好度）
     * - personalized：个性化推荐权重（品类偏好、热度、价格带、新鲜度、库存）
     * - hotSignal：热度信号权重（销量、营收、订单、新鲜度）
     *
     * 配置驱动渲染：
     * - 模板通过 v-for 遍历 sections 数组动态渲染
     * - 新增/修改权重区域只需改配置，无需改模板
     */
    const sections = [
      {
        key: 'hot',
        label: '热销榜权重',
        description: '控制热销商品的销量、营收、订单、新鲜度与库存贡献。',
        fields: [
          { key: 'sales', label: '销量' },
          { key: 'revenue', label: '营收' },
          { key: 'orders', label: '订单数' },
          { key: 'freshness', label: '新鲜度' },
          { key: 'inventory', label: '库存健康' }
        ]
      },
      {
        key: 'anonymous',
        label: '匿名推荐权重',
        description: '用于未登录用户首页推荐。',
        fields: [
          { key: 'hot', label: '热度' },
          { key: 'freshness', label: '新鲜度' },
          { key: 'inventory', label: '库存健康' },
          { key: 'affordability', label: '价格友好度' }
        ]
      },
      {
        key: 'personalized',
        label: '个性化推荐权重',
        description: '用于登录用户推荐，兼顾品类偏好和转化表现。',
        fields: [
          { key: 'category', label: '品类偏好' },
          { key: 'hot', label: '热度' },
          { key: 'price', label: '价格带' },
          { key: 'freshness', label: '新鲜度' },
          { key: 'inventory', label: '库存健康' }
        ]
      },
      {
        key: 'hotSignal',
        label: '热度信号权重',
        description: '影响推荐中的热度信号聚合方式。',
        fields: [
          { key: 'sales', label: '销量' },
          { key: 'revenue', label: '营收' },
          { key: 'orders', label: '订单数' },
          { key: 'freshness', label: '新鲜度' }
        ]
      }
    ]

    /**
     * 权重输入框禁用状态 — weightInputsDisabled
     *
     * computed 属性，当 form.mode === 'AUTO' 时返回 true。
     * 自动模式下，系统自动管理权重，禁止手动修改。
     * 这是"条件禁用"模式：根据业务状态控制 UI 元素的可用性。
     */
    const weightInputsDisabled = computed(() => form.mode === 'AUTO')

    /**
     * 应用表单数据 — applyForm
     *
     * 将后端返回的配置数据应用到表单中。
     * 使用深度合并策略，确保所有字段都有值：
     *
     * 合并逻辑：
     * 1. 外层属性：{ ...default, ...payload }
     * 2. 内层对象：{ ...default.hot, ...payload.hot }
     * 3. 使用 Object.assign(form, next) 更新 reactive 对象
     *
     * 为什么需要深度合并：
     * - 后端可能返回不完整的配置（缺少某些字段）
     * - 如果直接赋值，缺少的字段会是 undefined
     * - 使用默认值填充缺失字段，避免模板中出现 undefined
     */
    const applyForm = (payload) => {
      const nextDefault = createDefaultForm()
      const next = {
        ...nextDefault,
        ...payload,
        hot: { ...nextDefault.hot, ...(payload?.hot || {}) },
        anonymous: { ...nextDefault.anonymous, ...(payload?.anonymous || {}) },
        personalized: { ...nextDefault.personalized, ...(payload?.personalized || {}) },
        hotSignal: { ...nextDefault.hotSignal, ...(payload?.hotSignal || {}) }
      }
      Object.assign(form, next)
    }

    /**
     * 加载策略配置 — loadStrategy
     *
     * 请求流程：
     * 1. 设置 loading = true
     * 2. 发送 GET 请求到 /api/admin/showcase-strategy
     * 3. 成功：将响应数据赋值给 strategy，并应用到表单
     * 4. 失败：将错误信息赋值给 error
     * 5. 重置 loading = false
     */
    const loadStrategy = async () => {
      loading.value = true
      error.value = ''
      try {
        const response = await http.get('/admin/showcase-strategy')
        strategy.value = response.data || {}
        applyForm(strategy.value)
      } catch (err) {
        error.value = err.message || '获取推荐策略失败'
      } finally {
        loading.value = false
      }
    }

    /**
     * 构建提交数据 — buildPayload
     *
     * 从表单中提取需要提交的数据：
     * - 从 reactive 对象中提取各属性值
     * - 使用展开运算符复制嵌套对象，避免引用问题
     * - Number() 转换确保数值类型正确
     *
     * 为什么需要手动构建：
     * - reactive 对象包含 Vue 内部代理信息
     * - 直接提交 reactive 对象可能导致序列化问题
     * - 手动提取确保只提交业务数据
     */
    const buildPayload = () => ({
      mode: form.mode,
      shortWindowDays: Number(form.shortWindowDays || 7),
      longWindowDays: Number(form.longWindowDays || 30),
      cartPreferenceWeight: Number(form.cartPreferenceWeight || 0),
      hot: { ...form.hot },
      anonymous: { ...form.anonymous },
      personalized: { ...form.personalized },
      hotSignal: { ...form.hotSignal }
    })

    /**
     * 保存策略配置 — saveStrategy
     *
     * 保存流程：
     * 1. 设置 saving = true
     * 2. 构建提交数据（buildPayload）
     * 3. 发送 PUT 请求到 /api/admin/showcase-strategy
     * 4. 成功：更新 strategy 数据，应用新配置，显示提示
     * 5. 失败：显示错误信息
     * 6. 重置 saving = false
     */
    const saveStrategy = async () => {
      saving.value = true
      error.value = ''
      try {
        const response = await http.put('/admin/showcase-strategy', buildPayload())
        strategy.value = response.data || {}
        applyForm(strategy.value)
        await alertMessage(form.mode === 'AUTO' ? '已切换到自动模式并完成一次自动微调' : '推荐策略已保存')
      } catch (err) {
        error.value = err.message || '保存推荐策略失败'
      } finally {
        saving.value = false
      }
    }

    /**
     * 执行自动微调 — runAutoTune
     *
     * 自动微调流程：
     * 1. 发送 POST 请求到 /api/admin/showcase-strategy/auto-tune
     * 2. 后端根据历史数据自动调整权重
     * 3. 返回调整后的策略配置
     * 4. 前端更新显示
     *
     * 这是"自动优化"功能：
     * - 系统分析近 7/30 天的销售数据
     * - 根据转化率、销量等指标自动调整权重
     * - 管理员可以一键触发，无需手动计算
     */
    const runAutoTune = async () => {
      saving.value = true
      error.value = ''
      try {
        const response = await http.post('/admin/showcase-strategy/auto-tune')
        strategy.value = response.data || {}
        applyForm(strategy.value)
        await alertMessage('自动微调已完成')
      } catch (err) {
        error.value = err.message || '执行自动微调失败'
      } finally {
        saving.value = false
      }
    }

    /**
     * 数字格式化函数：
     * - formatInteger：数字 → 千分位字符串（1234567 → '1,234,567'）
     * - formatAmount：数字 → 2 位小数字符串（123.4 → '123.40'）
     * - formatPercent：小数 → 百分比字符串（0.1234 → '12.34%'）
     * - formatDateTime：日期 → 本地化字符串（ISO 格式 → '2024/1/1 12:00:00'）
     *
     * 这些函数使用可选链和空值合并：
     * - value || 0：防止 null/undefined
     * - Number()：确保是数字类型
     */
    const formatInteger = (value) => Number(value || 0).toLocaleString('zh-CN')
    const formatAmount = (value) => Number(value || 0).toFixed(2)
    const formatPercent = (value) => `${(Number(value || 0) * 100).toFixed(2)}%`
    const formatDateTime = (value) => {
      if (!value) {
        return '暂无'
      }
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) {
        return value
      }
      return date.toLocaleString('zh-CN', { hour12: false })
    }

    /**
     * 生命周期钩子 — onMounted
     *
     * 组件挂载后自动加载策略配置。
     */
    onMounted(loadStrategy)

    // 返回所有状态和函数供 template 使用
    return {
      strategy,
      form,
      sections,
      loading,
      saving,
      error,
      weightInputsDisabled,
      loadStrategy,
      saveStrategy,
      runAutoTune,
      formatInteger,
      formatAmount,
      formatPercent,
      formatDateTime
    }
  }
}</script>

<style scoped>
/*
 * ============================================================
 * 【AdminShowcaseStrategyView.vue 样式教学注释】
 * ============================================================
 *
 * 1. 页面布局（.strategy-page）：
 *    - Flex 纵向排列，gap 控制区域间距
 *    - 响应式设计，小屏幕下自动调整
 *
 * 2. 策略卡片（.strategy-card）：
 *    - 大圆角、半透明背景、阴影
 *    - 内部使用 Flex 纵向排列
 *
 * 3. 模式标签（.mode-pill）：
 *    - 圆角药丸形状
 *    - mode-auto：绿色背景（自动模式）
 *    - mode-manual：蓝色背景（手动模式）
 *
 * 4. 指标网格（.metrics-grid）：
 *    - CSS Grid 自适应网格
 *    - 每个指标项有独立的背景色和圆角
 *
 * 5. 权重输入框（.field input）：
 *    - 禁用状态（disabled）时样式变灰
 *    - 提示用户当前是自动模式，无法手动修改
 */

/* 页面布局 */
.strategy-page,
.header-actions,
.meta-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.strategy-page {
  flex-direction: column;
}

/* 策略卡片 */
.strategy-card,
.summary-card {
  padding: 24px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--shadow-card);
}

.strategy-card {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* 统计概览网格 */
.summary-grid,
.weight-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}

/* 区域标题 */
.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.section-heading h2 {
  margin: 0;
}

/* 描述文本颜色 */
.section-desc,
.hint,
.meta-row,
.metric-item span,
.empty-state {
  color: var(--text-soft);
}

/* 模式标签 — 药丸形状 */
.mode-pill {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

/* 手动模式样式 */
.mode-manual {
  background: rgba(79, 124, 255, 0.12);
  color: var(--primary-strong);
}

/* 自动模式样式 */
.mode-auto {
  background: rgba(32, 198, 200, 0.14);
  color: #0f8d8f;
}

/* 表单网格 */
.form-grid,
.weights-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 14px;
}

/* 表单字段 */
.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-weight: 600;
}

.field input,
.field select {
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: rgba(248, 251, 255, 0.95);
}

/* 禁用状态样式 */
.field input:disabled {
  color: #94a3b8;
  background: #f8fafc;
}

/* 指标网格 */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 14px;
}

/* 指标项 */
.metric-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(79, 124, 255, 0.05);
}

.metric-item strong {
  font-size: 20px;
}

/* 表格容器 */
.table-wrap {
  overflow-x: auto;
}

/* 表格样式 */
.metric-table {
  width: 100%;
  border-collapse: collapse;
}

.metric-table th,
.metric-table td {
  padding: 12px 14px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  text-align: left;
  white-space: nowrap;
}

/* 响应式：小屏幕下按钮撑满宽度 */
@media (max-width: 720px) {
  .header-actions {
    width: 100%;
  }

  .header-actions .btn {
    flex: 1 1 100%;
  }
}
</style>
