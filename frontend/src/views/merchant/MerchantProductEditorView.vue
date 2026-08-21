<template>
  <div class="editor-page">
    <div class="page-header">
      <h2 class="mc-title">{{ isEdit ? '编辑商品' : '新增商品' }}</h2>
    </div>

    <form class="editor-form" @submit.prevent="handleSubmit">
      <div class="form-group">
        <label>商品名称</label>
        <input v-model="form.name" type="text" placeholder="请输入商品名称" required />
      </div>
      <div class="form-group">
        <label>商品描述</label>
        <textarea v-model="form.description" rows="4" placeholder="请输入商品描述"></textarea>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>价格（元）</label>
          <input v-model.number="form.price" type="number" min="0" step="0.01" required />
        </div>
        <div class="form-group">
          <label>库存</label>
          <input v-model.number="form.stock" type="number" min="0" step="1" required />
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>分类ID</label>
          <input v-model.number="form.categoryId" type="number" min="1" required />
        </div>
        <div class="form-group">
          <label>状态</label>
          <select v-model.number="form.status">
            <option :value="1">上架</option>
            <option :value="0">下架</option>
          </select>
        </div>
      </div>
      <div class="form-group">
        <label>商品图片 URL</label>
        <input v-model="form.image" type="text" placeholder="请输入图片地址" />
      </div>
      <div class="form-actions">
        <button type="button" class="btn-cancel" @click="$router.push('/products')">取消</button>
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? '保存中...' : '保存' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../../utils/http'

const route = useRoute()
const router = useRouter()
// ★ 计算属性：URL 中有 id 则为编辑模式，否则为新增模式
const isEdit = computed(() => !!route.params.id)
const loading = ref(false)

// 表单数据，默认值对应新增模式
const form = ref({
  name: '', description: '', price: 0, stock: 0,
  categoryId: 1, image: '', status: 1
})

// 编辑模式下，挂载时拉取该商品现有信息回填表单
onMounted(async () => {
  if (!isEdit.value) return
  try {
    const res = await http.get(`/merchant/products/${route.params.id}`)
    if (res.data) {
      const p = res.data
      form.value = {
        name: p.name || '',
        description: p.description || '',
        price: p.price ?? 0,
        stock: p.stock ?? 0,
        categoryId: p.categoryId ?? 1,
        image: p.image || '',
        status: p.status ?? 1
      }
    }
  } catch (e) {
    console.error('加载商品信息失败', e)
  }
})

// 处理表单提交的异步函数
const handleSubmit = async () => {
  loading.value = true
  try {
    // 根据不同的模式调用不同的接口
    if(isEdit.value){
      // 编辑模式
      await http.put(`/merchant/products/${route.params.id}`, form.value)
    } else {
      // 新增模式
      await http.post('/merchant/products', form.value)
    }
    // 跳转回商品列表页面
    router.push('/products')
  } catch (error) {
    console.error('保存商品信息失败', error)
  } finally {
    loading.value = false
  }
}


</script>

<style scoped>
.editor-page { max-width: 720px; margin: 0 auto; }
.editor-form { background: #fff; padding: 28px; border-radius: 12px; box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08); }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; color: #555; }
.form-group input, .form-group textarea, .form-group select { width: 100%; box-sizing: border-box; padding: 10px 12px; border: 1px solid #dcdfe6; border-radius: 6px; font-size: 14px; }
.form-group input:focus, .form-group textarea:focus, .form-group select:focus { outline: none; border-color: #667eea; }
.form-row { display: flex; gap: 16px; }
.form-row .form-group { flex: 1; }
.form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 8px; }
.btn-primary { padding: 10px 28px; background: #667eea; color: #fff; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-cancel { padding: 10px 28px; background: #fff; color: #666; border: 1px solid #dcdfe6; border-radius: 6px; font-size: 14px; cursor: pointer; }
</style>
