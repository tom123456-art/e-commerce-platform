<template>
  <div class="store-page">
    <div class="page-header">
      <h2 class="mc-title">店铺信息</h2>
    </div>

    <form class="store-form" @submit.prevent="handleSave">
      <div class="form-group">
        <label>店铺名称</label>
        <input v-model="form.storeName" type="text" placeholder="请输入店铺名称" required />
      </div>
      <div class="form-group">
        <label>店铺简介</label>
        <textarea v-model="form.storeDescription" rows="3" placeholder="请输入店铺简介"></textarea>
      </div>
      <div class="form-group">
        <label>店铺 Logo URL</label>
        <input v-model="form.storeLogo" type="text" placeholder="请输入店铺 Logo 地址" />
      </div>
      <div class="form-group">
        <label>联系电话</label>
        <input v-model="form.contactPhone" type="tel" placeholder="请输入联系电话" />
      </div>
      <div class="form-group">
        <label>联系邮箱</label>
        <input v-model="form.contactEmail" type="email" placeholder="请输入联系邮箱" />
      </div>
      <div class="form-group">
        <label>店铺地址</label>
        <input v-model="form.address" type="text" placeholder="请输入店铺地址" />
      </div>

      <!-- 保存结果提示 -->
      <div v-if="message" :class="['tip', success ? 'tip-success' : 'tip-error']">{{ message }}</div>

      <div class="form-actions">
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? '保存中...' : '保存' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../../utils/http'

// 店铺信息表单
const form = ref({
  storeName: '', storeDescription: '', storeLogo: '',
  contactPhone: '', contactEmail: '', address: ''
})

const loading = ref(false)
const message = ref('')           // 操作提示
const success = ref(false)

// 组件在挂载的时候就执行，功能是加载店铺信息
onMounted(async () => {
  try {
    const res = await http.get('/merchant/store')
    if(res.data){
      // 如果有数据，那么将后端返回的店铺信息填充进表单
      form.value = {
        storeName: res.data.storeName || '',
        storeDescription: res.data.storeDescription || '',
        storeLogo: res.data.storeLogo || '',
        contactPhone: res.data.contactPhone || '',
        contactEmail: res.data.contactEmail || '',
        address: res.data.address || ''
      }
    }
  } catch (error) {
    console.error('加载店铺信息失败', error)
  }
})

// 保存店铺信息的异步函数
const handleSave = async () => {
  loading.value = true
  message.value = ''
  success.value = false
  try {
    const res = await http.put('/merchant/store', form.value)
    if(res.data){
      message.value = '保存成功'
      success.value = true
    }
  } catch (error) {
    message.value = error.message || '保存失败'
    success.value = false
  } finally {
    loading.value = false
  }
}



</script>

<style scoped>
.store-page { max-width: 720px; margin: 0 auto; }
.store-form { background: #fff; padding: 28px; border-radius: 12px; box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08); }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; color: #555; }
.form-group input, .form-group textarea { width: 100%; box-sizing: border-box; padding: 10px 12px; border: 1px solid #dcdfe6; border-radius: 6px; font-size: 14px; }
.form-group input:focus, .form-group textarea:focus { outline: none; border-color: #667eea; }
.tip { margin: 12px 0; padding: 10px 12px; border-radius: 6px; font-size: 14px; }
.tip-success { background: #f0f9eb; color: #67c23a; }
.tip-error { background: #fef0f0; color: #e74c3c; }
.form-actions { display: flex; justify-content: flex-end; margin-top: 8px; }
.btn-primary { padding: 10px 28px; background: #667eea; color: #fff; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
