<template>
  <!-- 左右两栏布局：左侧注册引导 + 右侧注册表单 -->
  <div class="auth-layout register-layout">
    <section class="auth-intro register-intro">
      <span class="eyebrow">Create Account</span>
      <h1>创建账号，完善个人资料</h1>
      <p>注册时补齐昵称、邮箱、手机号，后续登录、下单和收货信息管理会更顺畅。</p>
    </section>

    <section class="auth-card register-card">
      <div class="auth-head">
        <h2>注册账号</h2>
        <p>创建你的商城账号，开始挑选喜欢的商品。</p>
      </div>

      <!-- @submit.prevent 阻止表单默认提交，由 handleRegister 异步处理 -->
      <form class="auth-form" @submit.prevent="handleRegister">
        <div class="form-grid">
          <!--
            每个字段结构：label + input(v-model.trim + @blur校验) + 错误提示(v-if)
            @blur="validateField('fieldName')" 失焦时即时校验
          -->
          <div class="form-group">
            <label for="username">用户名</label>
            <input id="username" v-model.trim="form.username" type="text"
                   placeholder="4-20 位字母、数字或下划线" required maxlength="20"
                   @blur="validateField('username')">
            <p v-if="fieldErrors.username" class="field-error">{{ fieldErrors.username }}</p>
          </div>
          <div class="form-group">
            <label for="nickname">昵称</label>
            <input id="nickname" v-model.trim="form.nickname" type="text"
                   placeholder="请输入昵称" required maxlength="30"
                   @blur="validateField('nickname')">
            <p v-if="fieldErrors.nickname" class="field-error">{{ fieldErrors.nickname }}</p>
          </div>
          <div class="form-group">
            <label for="password">密码</label>
            <input id="password" v-model.trim="form.password" type="password"
                   placeholder="8-20 位，需含大小写字母、数字和特殊字符" required maxlength="20"
                   @blur="validateField('password')">
            <p v-if="fieldErrors.password" class="field-error">{{ fieldErrors.password }}</p>
          </div>
          <div class="form-group">
            <label for="confirmPassword">确认密码</label>
            <input id="confirmPassword" v-model.trim="form.confirmPassword" type="password"
                   placeholder="请再次输入密码" required maxlength="20"
                   @blur="validateField('confirmPassword')">
            <p v-if="fieldErrors.confirmPassword" class="field-error">{{ fieldErrors.confirmPassword }}</p>
          </div>
          <div class="form-group">
            <label for="email">邮箱</label>
            <input id="email" v-model.trim="form.email" type="email"
                   placeholder="请输入邮箱" required maxlength="50"
                   @blur="validateField('email')">
            <p v-if="fieldErrors.email" class="field-error">{{ fieldErrors.email }}</p>
          </div>
          <div class="form-group">
            <label for="phone">手机号</label>
            <input id="phone" v-model.trim="form.phone" type="tel"
                   placeholder="请输入 11 位手机号" required maxlength="11"
                   @blur="validateField('phone')">
            <p v-if="fieldErrors.phone" class="field-error">{{ fieldErrors.phone }}</p>
          </div>
        </div>

        <button class="btn btn-primary submit-btn" type="submit" :disabled="submitting">
          {{ submitting ? '注册中...' : '注册' }}
        </button>
        <!-- 全局错误和成功信息 -->
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="success" class="success">{{ success }}</p>
      </form>

      <p class="switch-link">
        已有账号？<router-link to="/login" class="link-inline">立即登录</router-link>
      </p>
    </section>
  </div>
</template>

<script>
/**
 * RegisterView.vue 组件逻辑
 *
 * 【API 使用重点】
 * 1. reactive() vs ref()：form 用 ref（整体替换），fieldErrors 用 reactive（逐字段修改）
 * 2. 表单验证模式：validateField 单字段校验（@blur 调用）+ validateForm 整体校验（提交时调用）
 * 3. 正则表达式：usernamePattern / passwordPattern / phonePattern / emailPattern
 */
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../utils/http'

// 正则常量定义在组件外，避免每次校验重建正则对象（性能优化）
const usernamePattern = /^[A-Za-z0-9_]{4,20}$/
const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@#$%^&+=!_.\-])[A-Za-z\d@#$%^&+=!_.\-]{8,20}$/
const phonePattern = /^1\d{10}$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export default {
  name: 'RegisterView',
  setup() {
    const router = useRouter()

    // ------------------------------------------------------------------------
    // 响应式状态
    // ------------------------------------------------------------------------

    /** 表单数据（含完整字段：用户名、密码、确认密码、昵称、邮箱、手机号） */
    const form = ref({
      username: '',
      password: '',
      confirmPassword: '',
      nickname: '',
      email: '',
      phone: ''
    })

    /**
     * fieldErrors：字段级错误信息（reactive，逐字段修改更自然）
     * 校验通过设为 ''，失败设为错误提示；模板中 v-if="fieldErrors.xxx" 控制显隐
     */
    const fieldErrors = reactive({
      username: '',
      nickname: '',
      password: '',
      confirmPassword: '',
      email: '',
      phone: ''
    })

    const error = ref('')       // 全局错误（提交失败）
    const success = ref('')     // 成功信息
    const submitting = ref(false)

    // ------------------------------------------------------------------------
    // 表单校验
    // ------------------------------------------------------------------------

    /**
     * validateField：单字段校验，@blur 时调用
     * 根据 field 名执行不同校验逻辑，结果写入 fieldErrors[field]
     */
    const validateField = (field) => {
      switch (field) {
        case 'username':
          fieldErrors.username = usernamePattern.test(form.value.username)
            ? '' : '用户名需为 4-20 位字母、数字或下划线'
          break
        case 'nickname':
          fieldErrors.nickname = form.value.nickname ? '' : '请输入昵称'
          break
        case 'password':
          fieldErrors.password = passwordPattern.test(form.value.password)
            ? '' : '密码需为 8-20 位且包含大小写字母、数字和特殊字符'
          break
        case 'confirmPassword':
          // 密码一致性校验（&& 短路求值：先检查 password 非空再比对）
          fieldErrors.confirmPassword =
            form.value.password && form.value.password === form.value.confirmPassword
              ? '' : '两次输入的密码不一致'
          break
        case 'email':
          fieldErrors.email = emailPattern.test(form.value.email)
            ? '' : '请输入正确的邮箱格式'
          break
        case 'phone':
          fieldErrors.phone = phonePattern.test(form.value.phone)
            ? '' : '请输入正确的 11 位手机号'
          break
      }
    }

    /**
     * validateForm：整体校验，提交前调用
     * 遍历所有字段逐一校验，返回第一个错误（空串表示通过）
     */
    const validateForm = () => {
      Object.keys(fieldErrors).forEach(validateField)
      for (const msg of Object.values(fieldErrors)) {
        if (msg) return msg
      }
      return ''
    }

    // ------------------------------------------------------------------------
    // 注册处理
    // ------------------------------------------------------------------------

    const handleRegister = async () => {
      error.value = ''
      success.value = ''

      // 整体校验
      const validationMessage = validateForm()
      if (validationMessage) {
        error.value = validationMessage
        return
      }

      submitting.value = true

      try {
        // 调用后端注册接口
        await http.post('/auth/register', form.value)
        success.value = '注册成功，请登录'

        // 延迟 800ms 跳转，让用户看到成功提示
        setTimeout(() => {
          router.push('/login')
        }, 800)
      } catch (err) {
        // 常见错误：用户名已存在（后端返回 400 + 错误消息）
        error.value = err.message || '注册失败，请稍后重试'
      } finally {
        submitting.value = false
      }
    }

    return {
      form, fieldErrors, error, success, submitting,
      validateField, handleRegister
    }
  }
}
</script>

<style scoped>
.register-layout {
  width: min(1180px, 100%);
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(420px, 560px);
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
  min-height: 680px;
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

.register-intro {
  background: linear-gradient(135deg, rgba(79, 124, 255, 0.98), rgba(32, 198, 200, 0.92));
}

.auth-card {
  padding: 44px 40px;
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
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
  margin-top: 10px;
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

.error,
.success {
  margin-top: 10px;
  padding: 11px 14px;
  border-radius: 12px;
  font-size: 0.95rem;
  text-align: center;
}

.error {
  border: 1px solid rgba(239, 68, 68, 0.12);
  background: rgba(239, 68, 68, 0.08);
  color: var(--danger);
}

.success {
  border: 1px solid rgba(5, 150, 105, 0.12);
  background: rgba(5, 150, 105, 0.08);
  color: var(--success);
}

.field-error {
  margin: 0;
  font-size: 13px;
  color: var(--danger);
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
  .register-layout {
    grid-template-columns: 1fr;
  }

  .auth-intro {
    min-height: 320px;
  }
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .auth-card,
  .auth-intro {
    padding: 32px 24px;
  }
}
</style>