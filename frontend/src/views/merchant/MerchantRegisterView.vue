<template>
  <div class="register-page">
    <div class="register-card">
      <h1>商家注册</h1>
      <p class="subtitle">注册商家中心账号，开启您的店铺</p>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="form.username" type="text" placeholder="4-20位字母数字下划线" required />
          <span v-if="errors.username" class="error-msg">{{ errors.username }}</span>
        </div>
        <div class="form-group">
          <label>昵称</label>
          <input v-model="form.nickname" type="text" placeholder="请输入昵称" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="form.password" type="password" placeholder="8-20位，含大小写字母、数字和特殊字符" required />
        </div>
        <div class="form-group">
          <label>确认密码</label>
          <input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" required />
          <span v-if="errors.confirmPassword" class="error-msg">{{ errors.confirmPassword }}</span>
        </div>
        <div class="form-group">
          <label>邮箱</label>
          <input v-model="form.email" type="email" placeholder="请输入邮箱" required />
          <span v-if="errors.email" class="error-msg">{{ errors.email }}</span>
        </div>
        <div class="form-group">
          <label>手机号</label>
          <input v-model="form.phone" type="tel" placeholder="请输入手机号" required />
          <span v-if="errors.phone" class="error-msg">{{ errors.phone }}</span>
        </div>
        <div class="form-group">
          <label>店铺名称</label>
          <input v-model="form.storeName" type="text" placeholder="请输入店铺名称" required />
          <span v-if="errors.storeName" class="error-msg">{{ errors.storeName }}</span>
        </div>
        <div class="form-group">
          <label>店铺简介</label>
          <textarea v-model="form.storeDescription" rows="3" placeholder="请输入店铺简介（选填）"></textarea>
        </div>
        <div v-if="error" class="error-msg">{{ error }}</div>
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>
      <p class="register-link">
        已有账号？<router-link to="/login">立即登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../utils/http'

const router = useRouter()

// 响应式注册表单数据
const form = reactive({
  username: '', nickname: '', password: '', confirmPassword: '',
  email: '', phone: '', storeName: '', storeDescription: ''
})

// 字段级验证错误
const errors = reactive({
  username: '', nickname: '', password: '', confirmPassword: '',
  email: '', phone: '', storeName: ''
})

const error = ref('')
const loading = ref(false)

// 前端表单校验（与后端 @Pattern 注解保持一致）
const validateForm = () => {
  let valid = true
  Object.keys(errors).forEach(k => errors[k] = '')

  // 验证用户名：4-20位字母数字下划线
  if (!form.username || !/^[a-zA-Z0-9_]{4,20}$/.test(form.username)) {
    errors.username = '用户名：4-20位字母、数字或下划线'
    valid = false
  }
  // 验证密码：8-20位含大小写数字和特殊字符
  if (!form.password || !/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,20}/.test(form.password)) {
    errors.password = '密码：8-20位，含大小写字母、数字和特殊字符'
    valid = false
  }
  // 验证确认密码
  if (form.password !== form.confirmPassword) {
    errors.confirmPassword = '两次输入的密码不一致'
    valid = false
  }
  // 验证邮箱格式
  if (form.email && !/^\S+@\S+\.\S+$/.test(form.email)) {
    errors.email = '邮箱格式不正确'
    valid = false
  }
  // 验证手机号
  if (form.phone && !/^1[3-9]\d{9}$/.test(form.phone)) {
    errors.phone = '手机号格式不正确'
    valid = false
  }
  // 验证店铺名称
  if (!form.storeName) {
    errors.storeName = '请输入店铺名称'
    valid = false
  }
  return valid
}

// 处理注册的异步函数
const handleRegister = async () => {
  error.value = ''
  loading.value = true
  // 前端校验如果失败直接拦截，不发送请求
  if(!validateForm) return
  console.log(1213)
  try {
    const res = await http.post('/merchant/register', {
      username: form.username,
      nickname: form.nickname,
      password: form.password,
      email: form.email,
      phone: form.phone,
      storeName: form.storeName,
      storeDescription: form.storeDescription
    })
    console.log(res.data)
    if(res.data){
      router.push('/login')
    } else {
      error.value = '注册失败'
    }
  } catch (err) {
    error.value = err.message || '注册失败'
  } finally {
    loading.value = false
  }
}

</script>

<style scoped>
.register-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #f5f7fa; padding: 40px 16px; }
.register-card { width: 100%; max-width: 420px; background: #fff; border-radius: 12px; padding: 40px 36px; box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08); }
.register-card h1 { margin: 0 0 6px; font-size: 24px; color: #333; }
.subtitle { margin: 0 0 24px; color: #999; font-size: 14px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; color: #555; }
.form-group input, .form-group textarea { width: 100%; box-sizing: border-box; padding: 10px 12px; border: 1px solid #dcdfe6; border-radius: 6px; font-size: 14px; }
.form-group input:focus, .form-group textarea:focus { outline: none; border-color: #667eea; }
.error-msg { display: block; margin-top: 4px; color: #e74c3c; font-size: 12px; }
.btn-primary { width: 100%; padding: 11px; background: #667eea; color: #fff; border: none; border-radius: 6px; font-size: 15px; cursor: pointer; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.register-link { margin: 18px 0 0; text-align: center; font-size: 14px; color: #666; }
.register-link a { color: #667eea; text-decoration: none; }
</style>
