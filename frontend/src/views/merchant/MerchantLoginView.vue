<template>
  <div class="login-page">
    <div class="login-card">
      <h1>商家登录</h1>
      <p class="subtitle">登录商家中心管理您的店铺</p>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="form.username" type="text" placeholder="请输入用户名" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="form.password" type="password" placeholder="请输入密码" required />
        </div>
        <div v-if="error" class="error-msg">{{ error }}</div>
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      <p class="register-link">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import http from '../../utils/http'

const router = useRouter()
const authStore = useAuthStore()
const form = ref({ username: '', password: '' })
const error = ref('')
const loading = ref(false)

// 处理登录提交的异步函数
const handleLogin = async () => {
  loading.value = true
  error.value = ''
  try {
    // 调用商家登录的接口
    // 注意：http 拦截器已解包 response.data，res 是后端 Result 包装体
    // { success, code, data: { token, user } }，token 在 res.data 内
    const res = await http.post('/merchant/login', form.value)
    if(res && res.data && res.data.token){
      // 登录成功：同步 Pinia store（导航栏右上角即时切换为 用户名+退出）+ 持久化
      authStore.setAuth(res.data)
      router.push('/dashboard')
    } else {
      error.value = '登录失败'
    }
  } catch (err) {
    // 优先展示后端返回的清晰错误信息（如 "用户名或密码错误"），而非 axios 状态码文本
    error.value = err.response?.data?.message || err.message || '登录失败'
  } finally {
    loading.value = false
  }
}

</script>

<style scoped>
/* 卡片在可用视区内垂直居中（扣除顶部导航 60px 与主内容区上下留白 64px） */
.login-page {
  min-height: calc(100vh - 124px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
}
.login-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}
.login-card h1 { margin: 0 0 6px; font-size: 24px; color: #333; }
.subtitle { margin: 0 0 24px; color: #999; font-size: 14px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; color: #555; }
.form-group input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 14px;
}
.form-group input:focus { outline: none; border-color: #667eea; }
.error-msg { display: block; margin: 0 0 12px; color: #e74c3c; font-size: 13px; }
.btn-primary {
  width: 100%;
  padding: 11px;
  background: #667eea;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-primary:hover { background: #5a67d8; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.register-link { margin: 18px 0 0; text-align: center; font-size: 14px; color: #666; }
.register-link a { color: #667eea; text-decoration: none; }
.register-link a:hover { text-decoration: underline; }
</style>
