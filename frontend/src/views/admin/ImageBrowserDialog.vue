<template>
  <!--
    ============================================================
    【ImageBrowserDialog.vue 模板教学注释】
    ============================================================

    1. 组件定位：
       ImageBrowserDialog 是一个"图片浏览器"对话框组件，
       用于让用户从服务器已上传的图片中选择一张。
       它是 AdminProductEditorView 的子组件。

    2. 组件通信模式 — v-model + emit：
       这是 Vue 中父子组件通信的经典模式：
       - 父组件：v-model="showImageBrowser" 控制对话框显示/隐藏
       - 子组件：通过 emit('update:modelValue', val) 通知父组件状态变化
       - 子组件：通过 emit('select', url) 将选中的图片 URL 传给父组件

       v-model 的本质：
       - 父 → 子：通过 props.modelValue 传入
       - 子 → 父：通过 emit('update:modelValue') 更新

    3. Element Plus 对话框 — el-dialog：
       - v-model="visible"：控制对话框的显示/隐藏
       - title="选择已上传图片"：对话框标题

    4. 图片选择交互：
       - 点击图片选中（高亮显示）
       - 点击"使用选中图片"按钮确认选择
  -->
  <el-dialog
    v-model="visible"
    title="选择已上传图片"
    width="720px"
    :close-on-click-modal="true"
    @close="handleClose"
  >
    <div v-if="loading" class="browser-loading">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <div v-else-if="images.length === 0" class="browser-empty">
      <el-empty description="暂无已上传的图片，请先上传图片" />
    </div>

    <div v-else class="image-grid">
      <div
        v-for="img in images"
        :key="img.filename"
        class="image-item"
        :class="{ selected: selected?.filename === img.filename }"
        @click="selected = img"
      >
        <img :src="img.url" :alt="img.filename" loading="lazy">
        <div class="image-overlay">
          <el-icon v-if="selected?.filename === img.filename" class="check-icon"><CircleCheck /></el-icon>
        </div>
        <div class="image-name" :title="img.filename">{{ img.filename }}</div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleRefresh" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新列表
        </el-button>
        <div class="footer-right">
          <el-button @click="handleClose">取消</el-button>
          <el-button type="primary" :disabled="!selected" @click="handleConfirm">
            使用选中图片
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script>
/**
 * ============================================================
 * 【ImageBrowserDialog.vue 脚本教学注释】
 * ============================================================
 *
 * 1. 组件设计模式 — 对话框组件（Dialog Component）
 * 2. props + emits 声明：
 *    - props.modelValue：接收父组件传入的布尔值
 *    - emits：['update:modelValue', 'select']
 * 3. watch 双向同步：
 *    - watch(props.modelValue)：父组件值变化时更新子组件 visible
 *    - watch(visible)：子组件 visible 变化时通知父组件
 * 4. 图片数据来源：
 *    调用后端 API GET /api/upload/images 获取服务器已上传图片列表
 */
import { ref, watch } from 'vue'
import { CircleCheck, Loading, Refresh } from '@element-plus/icons-vue'
import http from '../../utils/http'

export default {
  name: 'ImageBrowserDialog',
  // 注册图标组件
  components: { CircleCheck, Loading, Refresh },
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'select'],
  setup(props, { emit }) {
    const visible = ref(false)
    const loading = ref(false)
    const images = ref([])
    const selected = ref(null)

    watch(() => props.modelValue, (val) => {
      visible.value = val
      if (val) {
        loadImages()
      }
    })

    watch(visible, (val) => {
      emit('update:modelValue', val)
    })

    const loadImages = async () => {
      loading.value = true
      selected.value = null
      try {
        const res = await http.get('/upload/images', {
          params: {
            dir: '../frontend/public/images/products',
            urlPrefix: '/images/products'
          }
        })
        images.value = res.data || []
      } catch {
        images.value = []
      } finally {
        loading.value = false
      }
    }

    const handleRefresh = () => {
      loadImages()
    }

    const handleClose = () => {
      visible.value = false
    }

    const handleConfirm = () => {
      if (selected.value) {
        emit('select', selected.value.url)
        visible.value = false
      }
    }

    return {
      visible,
      loading,
      images,
      selected,
      handleRefresh,
      handleClose,
      handleConfirm
    }
  }
}
</script>

<style scoped>
/* 加载状态 */
.browser-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px 0;
  color: var(--el-text-color-secondary);
}

/* 空状态 */
.browser-empty {
  padding: 24px 0;
}

/* 图片网格 */
.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  max-height: 420px;
  overflow-y: auto;
  padding: 4px;
}

/* 图片项 */
.image-item {
  position: relative;
  border: 2px solid var(--el-border-color-lighter);
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
  background: var(--el-fill-color-lighter);
}

.image-item:hover {
  border-color: var(--el-color-primary-light-3);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.image-item.selected {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-5);
}

.image-item img {
  width: 100%;
  height: 110px;
  object-fit: cover;
  display: block;
}

.image-overlay {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.check-icon {
  font-size: 24px;
  color: var(--el-color-primary);
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.3));
}

.image-name {
  padding: 6px 8px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  background: var(--el-fill-color-light);
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.footer-right {
  display: flex;
  gap: 8px;
}
</style>