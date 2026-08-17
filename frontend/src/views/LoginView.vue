<template>
  <!-- 左右两栏布局：左侧介绍区 + 右侧登录表单 -->
  <div class="auth-layout">
    <!-- 左侧介绍区域：根据是否管理员登录，显示不同文案和背景色 -->
    <section class="auth-intro" :class="introClass">
      <span class="eyebrow">{{ isAdminLogin ? 'Admin Access' : 'Welcome Back' }}</span>
      <h1>{{ isAdminLogin ? '进入后台管理系统' : '欢迎回来' }}</h1>
      <p>{{ introText }}</p>
    </section>

    <!-- 右侧登录表单 -->
    <section class="auth-card">
      <div class="auth-head">
        <h2>{{ isAdminLogin ? '管理员登录' : '登录账号' }}</h2>
        <p>{{ isAdminLogin ? '请使用管理员账号密码登录独立后台。' : '使用账号密码登录，继续你的购物体验。' }}</p>
      </div>

      <!--
        @submit.prevent 阻止表单默认提交（页面刷新）
        :disabled="submitting" 提交中时禁用按钮，防止重复提交
      -->
      <form class="auth-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">用户名</label>
          <!-- v-model.trim 双向绑定 + 自动去首尾空格 -->
          <input id="username" v-model.trim="form.username" type="text"
                 placeholder="请输入用户名" required maxlength="50">
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input id="password" v-model.trim="form.password" type="password"
                 placeholder="请输入密码" required maxlength="100">
        </div>

        <button class="btn btn-primary submit-btn" type="submit" :disabled="submitting">
          {{ submitting ? '登录中...' : '登录' }}
        </button>

        <!-- 条件渲染错误信息 -->
        <p v-if="error" class="error">{{ error }}</p>
      </form>

      <!-- 底部链接：根据登录类型显示不同内容 -->
      <p class="switch-link">
        <template v-if="isAdminLogin">
          返回商城，<a :href="shopLoginUrl" class="link-inline">用户登录</a>
        </template>
        <template v-else>
          还没有账号？<router-link to="/register" class="link-inline">立即注册</router-link>
        </template>
      </p>
    </section>
  </div>
</template>

<script>
/**
 * LoginView.vue 组件逻辑（Composition API，Options API 的 setup() 写法）
 *
 * 【关键导入】
 * - saveAuth / clearAuth / isAdmin / isMerchant：来自 utils/auth.js
 *   注意：auth.js 只导出 saveAuth（不是 setToken/setUser），统一保存 token+user
 * - openAdminWindow / openMerchantWindow：跨窗口打开独立后台（端口 3001/3002）
 */
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../utils/http'
import { clearAuth, isAdmin, isMerchant, saveAuth } from '../utils/auth'
import { buildShopUrl, openAdminWindow, openMerchantWindow } from '../utils/appLinks'

export default {
  name: 'LoginView',
  setup() {
    const route = useRoute()
    const router = useRouter()

    // ------------------------------------------------------------------------
    // 响应式状态
    // ------------------------------------------------------------------------

    const form = ref({ username: '', password: '' })
    const error = ref('')           // 错误提示
    const submitting = ref(false)   // 提交中状态（禁用按钮）

    // ------------------------------------------------------------------------
    // 计算属性
    // ------------------------------------------------------------------------

    /**
     * isAdminLogin：根据路由 meta.adminLogin 判断是否管理员登录模式
     * 管理员后台（端口 3001）的登录路由会设置 meta: { adminLogin: true }
     */
    const isAdminLogin = computed(() => Boolean(route.meta.adminLogin))

    const introText = computed(() => {
      return isAdminLogin.value
        ? '管理员登录后可进入独立后台，统一完成商品、用户和订单管理。'
        : '登录后可继续管理购物车、提交订单，并随时查看订单状态。'
    })

    const introClass = computed(() => isAdminLogin.value ? 'admin-intro' : 'user-intro')

    // ------------------------------------------------------------------------
    // 登录处理
    // ------------------------------------------------------------------------

    const handleLogin = async () => {
      error.value = ''

      // 前端表单校验
      if (!form.value.username || !form.value.password) {
        error.value = '请输入用户名和密码'
        return
      }

      submitting.value = true

      try {
        // http 拦截器已解包 response.data，payload 直接是业务数据
        const response = await http.post('/auth/login', form.value)
        const payload = response.data

        // 校验返回数据完整性
        if (!payload?.token) {
          clearAuth()
          error.value = '登录响应缺少token，请检查后端配置'
          return
        }

        // 管理员登录校验：以管理员身份登录但账号不是管理员，拒绝
        if (isAdminLogin.value && !isAdmin(payload.user)) {
          clearAuth()
          error.value = '当前账号不是管理员，无法进入后台'
          return
        }

        /**
         * 【saveAuth 保存认证信息】
         * 统一保存 token + user 到 localStorage（替代旧的 setToken/setUser 两个函数）
         * 之后 http.js 的请求拦截器会自动在每个请求头携带 Authorization: Bearer <token>
         */
        saveAuth(payload)

        // 按角色跳转：管理员 -> 独立后台窗口；商家 -> 商家后台；普通用户 -> 首页
        if (isAdmin(payload.user)) {
          openAdminWindow('/')
          return
        }
        if (isMerchant(payload.user)) {
          openMerchantWindow('/')
          return
        }

        // 普通用户：优先跳转到 redirect 参数指定的页面，否则首页
        const redirect = route.query.redirect
        router.push(redirect || '/')
      } catch (err) {
        // Axios 拦截器已统一处理错误信息脱敏
        error.value = err.message || '登录失败，请稍后重试'
      } finally {
        submitting.value = false
      }
    }

    return {
      form, error, submitting, handleLogin,
      isAdminLogin, introText, introClass,
      // shopLoginUrl: buildShopUrl('/login')
    }
  }
}
</script>

<style scoped>
.auth-layout {
  width: min(1120px, 100%);
  display: grid;
  grid-template-columns: minmax(320px, 1.1fr) minmax(360px, 460px);
  gap: 0;
  border-radius: 28px;
  overflow: hidden;
  background: var(--surface);
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.14);
}

.auth-intro {
  position: relative;
  padding: 48px 40px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 620px;
}

.auth-intro::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at top right, rgba(255, 255, 255, 0.22), transparent 28%);
  pointer-events: none;
}

.auth-intro > * {
  position: relative;
  z-index: 1;
}

.eyebrow {
  display: inline-block;
  width: fit-content;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.auth-intro h1 {
  margin: 18px 0 12px;
  font-size: 2rem;
  line-height: 1.2;
}

.auth-intro p {
  max-width: 420px;
  font-size: 1rem;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.92);
}

.user-intro {
  background: linear-gradient(135deg, rgba(79, 124, 255, 0.98), rgba(32, 198, 200, 0.9));
}

.admin-intro {
  background: linear-gradient(135deg, rgba(19, 34, 56, 0.97), rgba(79, 124, 255, 0.93));
}

.auth-card {
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: rgba(255, 255, 255, 0.96);
}

.auth-head h2 {
  font-size: 1.65rem;
  margin-bottom: 8px;
  color: var(--text);
}

.auth-head p {
  color: var(--muted);
  margin-bottom: 24px;
  line-height: 1.7;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

label {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--text);
}

input {
  width: 100%;
  padding: 14px 15px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #f8fbff;
  color: var(--text);
  font-size: 0.95rem;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

input:focus {
  outline: none;
  border-color: rgba(79, 124, 255, 0.45);
  box-shadow: 0 0 0 4px rgba(79, 124, 255, 0.12);
  background: white;
}

.submit-btn {
  width: 100%;
  margin-top: 6px;
  padding: 14px 16px;
  border: none;
  border-radius: 999px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--primary), var(--secondary));
  box-shadow: 0 12px 24px rgba(79, 124, 255, 0.24);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 28px rgba(79, 124, 255, 0.28);
}

.submit-btn:disabled {
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
  background: #c5d4ff;
}

.error {
  margin-top: 2px;
  padding: 11px 14px;
  border-radius: 12px;
  border: 1px solid rgba(239, 68, 68, 0.12);
  background: rgba(239, 68, 68, 0.08);
  color: var(--danger);
  font-size: 0.95rem;
  text-align: center;
}

.switch-link {
  margin-top: 24px;
  font-size: 0.95rem;
  color: var(--muted);
  text-align: center;
}

.link-inline {
  color: var(--primary);
  font-weight: 700;
  text-decoration: none;
}

.link-inline:hover {
  text-decoration: underline;
}

@media (max-width: 960px) {
  .auth-layout {
    grid-template-columns: 1fr;
  }

  .auth-intro {
    min-height: 320px;
  }
}

@media (max-width: 640px) {
  .auth-card,
  .auth-intro {
    padding: 32px 24px;
  }
}
</style>