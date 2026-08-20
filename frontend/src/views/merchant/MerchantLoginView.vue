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
import { saveAuth } from '../../utils/auth'
import http from '../../utils/http'

const router = useRouter()
const form = ref({ username: '', password: '' })
const error = ref('')
const loading = ref(false)

// 处理登录提交的异步函数
const handleLogin = async () => {
  loading.value = true
  error.value = ''
  try {
    // 调用商家登录的接口
    const res = await http.post('/merchant/login', form.value)
    if(res.data){
      // 如果有返回数据
      saveAuth(res.data) // 保存token和用户信息
      router.push('/dashboard')
    } else {
      error.value = '登录失败'
    }
  } catch (err) {
    error.value = err.message || '登录失败'
  } finally {
    loading.value = false
  }
}

</script>
