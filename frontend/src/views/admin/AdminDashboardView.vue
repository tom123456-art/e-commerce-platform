<template>
  <!--
    ============================================================
    【AdminDashboardView.vue 模板教学注释】
    ============================================================

    1. 组件定位：
       AdminDashboardView 是管理后台的"运营看板"页面，
       用于展示数据可视化图表（由 Python PyEcharts 生成的 HTML）。
       这是管理员登录后看到的第一个页面，提供业务数据的概览。

    2. 页面状态管理 — 三态模式：
       管理后台页面通常需要处理三种状态：
       - 加载中（loading）：显示 loading 提示
       - 加载失败（error）：显示错误信息
       - 加载成功：显示实际内容
       这种"三态模式"是前后端分离项目中处理异步数据加载的标准模式。

    3. iframe 安全沙箱：
       使用 <iframe sandbox="allow-scripts"> 嵌入 PyEcharts 生成的 HTML。
       sandbox 属性限制 iframe 内的能力：
       - allow-scripts：允许执行脚本（PyEcharts 需要）
       - 不允许访问父页面 DOM、Cookie、表单等敏感资源
       这是防御 XSS 攻击的重要安全措施。

    4. srcdoc 属性：
       使用 :srcdoc="dashboardHtml" 将 HTML 字符串直接嵌入 iframe，
       而非通过 URL 加载。这样可以避免跨域问题，
       但也意味着需要特别注意 XSS 防护（sandbox 属性）。
  -->
  <div class="page-shell">
    <!-- 页面头部：标题 + 刷新按钮 -->
    <div class="page-header">
      <div>
        <span class="eyebrow">Dashboard</span>
        <h1>运营看板</h1>
        <p class="page-subtitle">已聚合热卖榜、首页推荐榜与推荐权重配置，便于持续联调和运营观察。</p>
      </div>
      <!--
        刷新按钮：
        - @click="fetchVisualization" 点击时重新请求后端数据
        - 这是手动刷新模式，适用于不频繁更新的看板数据
      -->
      <button class="btn btn-secondary compact" @click="fetchVisualization">刷新看板</button>
    </div>

    <!--
      条件渲染 — 三态模式：
      - v-if="error"：有错误时显示错误信息
      - v-else-if="loading"：加载中时显示 loading 提示
      - v-else：加载成功时显示实际内容

      Vue 的 v-if / v-else-if / v-else 是互斥的条件渲染，
      只有第一个为真的条件会被渲染到 DOM 中。
    -->
    <p v-if="error" class="state error">{{ error }}</p>
    <p v-else-if="loading" class="state">加载中...</p>

    <div v-else class="dashboard-frame-card">
      <!--
        iframe 嵌入 PyEcharts 可视化：
        - v-if="dashboardHtml"：只有当有数据时才渲染 iframe
        - :srcdoc="dashboardHtml"：将 HTML 字符串作为 iframe 内容
        - sandbox="allow-scripts"：安全沙箱，限制 iframe 能力
        - title="PyEcharts Dashboard"：无障碍访问标题
      -->
      <iframe
        v-if="dashboardHtml"
        class="dashboard-frame"
        :srcdoc="dashboardHtml"
        sandbox="allow-scripts"
        title="PyEcharts Dashboard"
      />
    </div>
  </div>
</template>

<script>
/**
 * ============================================================
 * 【AdminDashboardView.vue 脚本教学注释】
 * ============================================================
 *
 * 1. 组件设计模式 — 数据可视化看板：
 *    看板页面的核心模式是：
 *    - 页面加载时（onMounted）自动请求后端数据
 *    - 将返回的 HTML/图表数据渲染到页面上
 *    - 提供手动刷新按钮让用户获取最新数据
 *
 * 2. API 调用模式：
 *    使用 http（Axios 实例）调用后端接口：
 *    - GET /admin/dashboard/visualization：获取 PyEcharts 生成的 HTML
 *    - responseType: 'text'：告诉 Axios 将响应解析为纯文本（而非 JSON）
 *    - 这是因为后端返回的是 HTML 字符串，不是标准的 { success, data } 格式
 *
 * 3. 生命周期钩子 — onMounted：
 *    onMounted 在组件挂载到 DOM 后执行，
 *    是发起初始数据请求的最佳时机。
 *    此时 DOM 已经准备好，可以安全地操作 DOM 元素。
 *
 * 4. 安全考虑：
 *    - 使用 sandbox 属性限制 iframe 能力
 *    - 生产环境建议后端配置 CSP（Content-Security-Policy）响应头
 *    - 进一步限制内联脚本执行，防止 XSS 攻击
 */
import { onMounted, ref } from 'vue'
import http from '../../utils/http'

export default {
  name: 'AdminDashboardView',
  /**
   * setup() 函数 — Composition API 入口
   */
  setup() {
    const loading = ref(false)
    const error = ref('')
    const dashboardHtml = ref('')

    const fetchVisualization = async () => {
      loading.value = true
      error.value = ''
      try {
        const response = await http.get('/admin/dashboard/visualization', {
          responseType: 'text'
        })
        // 安全检查：确保响应数据是字符串类型
        dashboardHtml.value = typeof response === 'string' ? response : ''
      } catch (err) {
        // 错误信息提取：优先使用后端返回的错误信息，兜底显示通用提示
        error.value = err.message || '获取 PyEcharts 看板失败'
      } finally {
        // finally 块确保 loading 状态一定会被重置
        loading.value = false
      }
    }

    /**
     * 生命周期钩子 — onMounted
     */
    onMounted(fetchVisualization)

    // 返回状态和函数供 template 使用
    return {
      loading,
      error,
      dashboardHtml,
      fetchVisualization
    }
  }
}</script>

<style scoped>
/* ============================================================
 * AdminDashboardView.vue 样式 - 运营看板
 * ============================================================ */

/* ---------- 页面头部（与各管理页面统一） ---------- */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}

.eyebrow {
  display: inline-block;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: #4f7cff;
  background: rgba(79, 124, 255, 0.10);
  padding: 4px 10px;
  border-radius: 999px;
}

.page-header h1 {
  margin-top: 10px;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.2px;
}

.page-subtitle {
  color: #64748b;
  margin-top: 6px;
  font-size: 14px;
  line-height: 1.6;
}

/* ---------- 按钮 ---------- */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 18px;
  border: none;
  border-radius: 10px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  text-decoration: none;
  transition: transform .15s ease, box-shadow .15s ease, background-color .15s ease, color .15s ease;
}

.btn-primary {
  background: linear-gradient(135deg, #4f7cff 0%, #3558d3 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(79, 124, 255, 0.30);
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(79, 124, 255, 0.38);
}

.btn-secondary {
  background: #eef1f6;
  color: #334155;
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.btn-secondary:hover { background: #e2e8f0; }

.btn-danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  box-shadow: 0 3px 10px rgba(239, 68, 68, 0.25);
}

.btn-danger:hover {
  filter: brightness(1.05);
  transform: translateY(-1px);
}

.btn-sm { padding: 6px 12px; font-size: 13px; border-radius: 8px; }
.btn.compact { padding: 8px 14px; font-size: 13px; }

/* ---------- 仪表盘卡片 ---------- */
.dashboard-frame-card {
  overflow: hidden;
  min-height: 920px;
  padding: 14px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.05);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.dashboard-frame {
  width: 100%;
  min-height: 890px;
  border: none;
  border-radius: 14px;
  background: #f7faff;
}
</style>