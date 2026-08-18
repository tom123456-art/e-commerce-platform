<template>
  <!--
    ============================================================
    【AdminProductEditorView.vue 模板教学注释】
    ============================================================

    1. 组件定位：
       AdminProductEditorView 是商品的"编辑/新增"页面，
       同一个组件同时处理"新增"和"编辑"两种模式（复用设计）。
       通过路由参数 route.params.id 判断当前模式：
       - 有 id → 编辑模式（isEdit = true）
       - 无 id → 新增模式（isEdit = false）

    2. 页面布局 — 双栏布局：
       左侧：商品信息表单（el-form）
       右侧：Excel 批量导入功能
       两个区域并排显示，充分利用宽屏空间。

    3. 表单设计模式 — Element Plus 表单：
       使用 el-form + el-form-item 构建表单：
       - :model="form"：绑定表单数据对象
       - :rules="rules"：绑定验证规则
       - ref="formRef"：获取表单实例，用于手动触发验证
       - @submit.prevent="submitProduct"：阻止默认提交，自定义提交逻辑
       - label-position="top"：标签显示在输入框上方

    4. 图片管理 — 三种方式：
       1. 选择服务器已上传图片（ImageBrowserDialog）
       2. 本地上传新图片（input[type=file]）
       3. 删除已选图片
       这种多通道图片管理是电商后台的常见需求。


    5. Excel 批量导入：
       - 支持拖拽上传 Excel 文件
       - 调用后端接口解析 Excel 并批量创建商品
       - 支持下载导入模板
  -->
  <div class="page-shell">
    <!-- 页面头部：标题 + 返回按钮 -->
    <div class="page-header">
      <div>
        <span class="eyebrow">Products</span>
        <!--
          动态标题：
          - 使用三元表达式根据 isEdit 切换标题
          - 编辑模式显示"编辑商品"，新增模式显示"新增商品"
        -->
        <h1>{{ isEdit ? '编辑商品' : '新增商品' }}</h1>
        <p class="page-subtitle">支持单个商品录入，也支持通过 Excel 批量新增商品。</p>
      </div>
      <div class="header-actions">
        <!-- 返回按钮：跳转回商品列表页 -->
        <el-button @click="$router.push('/products')">返回商品管理</el-button>
      </div>
    </div>

    <!--
      双栏布局容器：
      - CSS Grid：grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.9fr)
      - 左侧占 1.3 份宽度，右侧占 0.9 份宽度
      - minmax(0, 1.3fr)：最小 0，最大 1.3 倍份额
      - minmax(320px, 0.9fr)：最小 320px，最大 0.9 倍份额
    -->
    <div class="editor-layout">
      <!-- 左侧：商品表单 -->
      <el-card shadow="never" class="editor-card">
        <template #header>
          <span class="card-title">{{ isEdit ? '商品信息编辑' : '新增商品表单' }}</span>
        </template>

        <!--
          Element Plus 表单：
          - ref="formRef"：获取表单实例引用，用于手动触发验证
          - :model="form"：绑定表单数据对象（必须是对象）
          - :rules="rules"：绑定验证规则
          - label-position="top"：标签在输入框上方
          - @submit.prevent="submitProduct"：阻止浏览器默认提交行为
        -->
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="editor-form"
          @submit.prevent="submitProduct"
        >
          <!--
            商品名称输入框：
            - el-form-item：表单项容器，label 为标签文本
            - prop="name"：对应 rules 中的验证规则键名
            - v-model.trim="form.name"：双向绑定，.trim 自动去除首尾空格
            - maxlength="120"：限制最大输入长度
            - show-word-limit：显示字数统计
          -->
          <el-form-item label="商品名称" prop="name">
            <el-input v-model.trim="form.name" placeholder="请输入商品名称" maxlength="120" show-word-limit />
          </el-form-item>

    

          <!--
            价格和库存 — 并排布局：
            - el-row + el-col：Element Plus 的栅格系统
            - :gutter="16"：列间距 16px
            - :span="12"：每列占 12/24 = 一半宽度
            - el-input-number：数字输入框，支持 min、precision、step
          -->
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="价格" prop="price">
                <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="库存" prop="stock">
                <el-input-number v-model="form.stock" :min="0" :step="1" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>

          <!--
            分类和状态 — 并排布局：
            - el-select：下拉选择器
            - v-model.number：.number 修饰符自动将值转换为数字类型
            - 遍历 categories 数组生成选项
          -->
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="分类" prop="categoryId">
                <el-select v-model.number="form.categoryId" placeholder="请选择分类" style="width: 100%">
                  <el-option
                    v-for="category in categories"
                    :key="category.id"
                    :label="category.label"
                    :value="category.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="状态" prop="status">
                <el-select v-model.number="form.status" style="width: 100%">
                  <el-option :value="1" label="上架" />
                  <el-option :value="0" label="下架" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <!--
            商品图片管理：
            支持三种操作方式：
            1. 点击空白区域 → 打开图片浏览器选择服务器已上传图片
            2. 点击"本地上传" → 触发文件选择器上传新图片
            3. 点击"删除" → 清除已选图片

            隐藏的 input[type=file]：
            - style="display: none" 隐藏原生文件选择器
            - 通过 ref 触发 click 事件（triggerImageUpload）
            - @change="handleImageUpload" 监听文件选择事件
          -->
          <el-form-item label="商品图片">
            <!-- 隐藏的文件上传 input -->
            <input ref="imageFileInput" type="file" accept="image/*" @change="handleImageUpload" style="display: none">

            <!--
              无图片状态 — 显示上传占位区域：
              - @click="showImageBrowser = true" 打开图片浏览器
              - 显示图标和提示文字
            -->
            <div v-if="!form.image" class="image-upload-area" @click="showImageBrowser = true">
              <div class="upload-placeholder">
                <el-icon :size="48" color="#c0c4cc"><PictureFilled /></el-icon>
                <span>点击选择图片</span>
              </div>
            </div>

            <!--
              有图片状态 — 显示预览和操作按钮：
              - 显示图片预览
              - 底部悬浮操作栏：更换图片、本地上传、删除
              - @click.stop 阻止事件冒泡，避免触发父元素的点击事件
            -->
            <div v-else class="image-upload-area has-image">
              <div class="upload-preview">
                <img :src="resolveImage(form)" :alt="form.name || '商品图片'" class="preview-image">
                <div class="upload-actions">
                  <el-button size="small" @click.stop="showImageBrowser = true">更换图片</el-button>
                  <el-button size="small" @click.stop="triggerImageUpload">本地上传</el-button>
                  <el-button size="small" type="danger" plain @click.stop="removeImage">删除</el-button>
                </div>
              </div>
            </div>

            <span class="upload-hint">点击选择服务器已上传图片，也可"本地上传"新图片（JPG/PNG/GIF，≤5MB）</span>
          </el-form-item>

          <!--
            表单操作按钮：
            - 提交按钮：根据模式显示不同文本
            - 重置按钮：清空表单数据
          -->
          <el-form-item>
            <div class="form-actions">
              <el-button type="primary" :loading="saving" @click="submitProduct">
                {{ saving ? '保存中...' : isEdit ? '保存修改' : '创建商品' }}
              </el-button>
              <el-button @click="resetForm">重置表单</el-button>
            </div>
          </el-form-item>
        </el-form>
      </el-card>

      <!--
        右侧：Excel 批量导入区域：
        - id="import"：用于 URL hash 定位（从商品列表页跳转时 #import 定位到此处）
        - ref="importSection"：获取 DOM 引用，用于滚动定位
      -->
      <el-card id="import" ref="importSection" shadow="never" class="import-card">
        <template #header>
          <div class="section-title">
            <span class="card-title">Excel 批量新增</span>
            <el-button size="small" @click="downloadTemplate">下载导入模板</el-button>
          </div>
        </template>

        <p class="section-desc">请使用模板填写后再上传，支持 <code>.xlsx</code> 和 <code>.xls</code> 文件。</p>

        <!--
          模板字段说明：
          - el-alert：Element Plus 的提示组件
          - type="info"：信息类型（蓝色）
          - :closable="false"：不显示关闭按钮
          - show-icon：显示图标
        -->
        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="import-tips"
        >
          <template #title>模板字段说明</template>
          <template #default>
            <p>商品名称、商品描述、价格、库存、图片地址、分类ID、上架状态。</p>
            <p>分类 ID 可选：1 手机数码、2 电脑办公、3 智能家电、4 居家生活、5 运动户外、6 影音娱乐。</p>
          </template>
        </el-alert>

        <div class="import-actions">
          <!--
            el-upload 文件上传组件：
            - ref="uploadRef"：获取上传组件实例
            - :auto-upload="false"：禁用自动上传，手动触发
            - :limit="1"：限制只能选择 1 个文件
            - accept=".xlsx,.xls"：限制文件类型
            - :on-change="handleUploadChange"：文件选择变化时的回调
            - drag：启用拖拽上传模式
          -->
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            :on-change="handleUploadChange"
            :on-exceed="() => {}"
            drag
          >
            <el-icon :size="40"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">仅支持 .xlsx / .xls 文件</div>
            </template>
          </el-upload>

          <!-- 导入按钮：有文件时才可点击 -->
          <el-button type="primary" :loading="importing" :disabled="!selectedFile" @click="submitImport">
            {{ importing ? '导入中...' : '开始导入' }}
          </el-button>
        </div>

        <!-- 导入结果提示 -->
        <el-alert v-if="importResult" type="success" :closable="false" show-icon class="import-msg">
          {{ importResult }}
        </el-alert>
        <el-alert v-if="importError" type="error" :closable="false" show-icon class="import-msg">
          {{ importError }}
        </el-alert>
      </el-card>
    </div>

    <!--
      图片浏览器弹窗：
      - v-model="showImageBrowser"：控制弹窗的显示/隐藏
      - @select="onImageSelected"：用户选择图片后触发的事件
      - 子组件通过 emit('select', url) 将选中的图片 URL 传给父组件
    -->
    <ImageBrowserDialog v-model="showImageBrowser" @select="onImageSelected" />
  </div>
</template>

<script>
/**
 * ============================================================
 * 【AdminProductEditorView.vue 脚本教学注释】
 * ============================================================
 *
 * 1. 组件设计模式 — 表单页（Form Page）：
 *    这是管理后台中处理"新增/编辑"的标准模式：
 *    - 同一个组件处理新增和编辑两种模式（路由参数区分）
 *    - 编辑模式：onMounted 时加载已有数据填充表单
 *    - 新增模式：显示空表单
 *    - 提交时根据模式调用不同的 API（POST 新增 / PUT 更新）
 *
 * 2. 表单验证模式：
 *    Element Plus 的表单验证流程：
 *    1. 在 rules 中定义验证规则
 *    2. el-form-item 的 prop 属性关联 rules 中的键
 *    3. 提交时调用 formRef.value?.validate() 手动触发验证
 *    4. 验证通过才执行提交逻辑
 *
 * 3. 文件上传模式：
 *    两种上传方式：
 *    - 本地上传：使用隐藏的 input[type=file]，选择后通过 FormData 上传
 *    - 服务器图片选择：使用 ImageBrowserDialog 弹窗选择已上传图片
 *
 * 4. URL Hash 定位：
 *    路由路径 /products/new#import 中的 #import 是 URL hash。
 *    组件在 onMounted 时检查 route.hash，如果匹配则滚动到对应区域。
 *    这是从商品列表页的"Excel 批量导入"按钮跳转过来的定位机制。
 *
 * 5. 子组件通信 — props + events：
 *    ImageBrowserDialog 子组件：
 *    - v-model="showImageBrowser"：父 → 子（控制显示/隐藏）
 *    - @select="onImageSelected"：子 → 父（传递选中的图片 URL）
 *    这是 Vue 中父子组件通信的标准模式。
 */
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { PictureFilled, UploadFilled } from '@element-plus/icons-vue'
import http from '../../utils/http'
import { PRODUCT_CATEGORIES, resolveProductImage } from '../../utils/productCatalog'
import { alertMessage } from '../../utils/modal'
import ImageBrowserDialog from './ImageBrowserDialog.vue'

/**
 * 表单默认值工厂函数 — createForm
 *
 * 为什么用工厂函数：
 * - 每次调用返回一个全新的对象
 * - 避免多个引用指向同一个对象导致的意外修改
 * - 在重置表单和初始化时调用，确保得到干净的初始状态
 *
 * 表单字段说明：
 * - id：商品 ID（编辑模式有值，新增模式为 null）
 * - name：商品名称
 * - description：商品描述
 * - price：价格（默认 0）
 * - stock：库存（默认 0）
 * - image：图片地址
 * - categoryId：分类 ID（默认 1 = 手机数码）
 * - status：状态（默认 1 = 上架）
 */
const createForm = () => ({
  id: null,
  name: '',
  description: '',
  price: 0,
  stock: 0,
  image: '',
  categoryId: 1,
  status: 1
})

export default {
  name: 'AdminProductEditorView',
  // 注册子组件和图标组件
  components: { PictureFilled, UploadFilled, ImageBrowserDialog },
  setup() {
    // 获取路由实例
    const route = useRoute()
    const router = useRouter()

    /**
     * 表单相关引用：
     * - formRef：el-form 组件实例，用于调用 validate() 方法
     * - form：表单数据对象（响应式）
     * - categories：商品分类配置
     */
    const formRef = ref(null)
    const form = ref(createForm())
    const categories = PRODUCT_CATEGORIES

    /**
     * 各种状态标记：
     * - saving：是否正在保存（提交表单）
     * - importing：是否正在导入 Excel
     * - importResult：导入成功信息
     * - importError：导入错误信息
     * - importSection：Excel 导入区域的 DOM 引用
     * - selectedFile：选中的 Excel 文件
     * - imageFileInput：隐藏的文件 input DOM 引用
     * - imageUploading：是否正在上传图片
     * - showImageBrowser：是否显示图片浏览器弹窗
     * - uploadRef：el-upload 组件实例
     */
    const saving = ref(false)
    const importing = ref(false)
    const importResult = ref('')
    const importError = ref('')
    const importSection = ref(null)
    const selectedFile = ref(null)
    const imageFileInput = ref(null)
    const imageUploading = ref(false)
    const showImageBrowser = ref(false)
    const uploadRef = ref(null)

    /**
     * 判断当前模式 — computed
     *
     * isEdit 的计算逻辑：
     * - route.params.id 存在 → 编辑模式（true）
     * - route.params.id 不存在 → 新增模式（false）
     *
     * route.params 是 Vue Router 的动态路由参数：
     * - 路由配置：path: 'products/:id/edit'
     * - 访问 /products/123/edit 时，route.params.id = '123'
     * - 访问 /products/new 时，route.params.id = undefined
     */
    const isEdit = computed(() => Boolean(route.params.id))


    /**
     * 表单验证规则 — rules
     *
     * Element Plus 验证规则格式：
     * - 键名对应 el-form-item 的 prop 属性
     * - 每个规则对象包含：required、message、trigger
     * - trigger：触发验证的时机（'blur' 失焦时 / 'change' 值变化时）
     *
     * 验证流程：
     * 1. 用户操作表单（输入、选择等）
     * 2. 根据 trigger 设置自动触发验证
     * 3. 提交时手动调用 formRef.value?.validate() 全量验证
     * 4. 验证失败会显示错误提示
     */
    const rules = {
      name: [{required: true, message: '请输入商品名称', trigger: 'blur'}],
      price: [{required: true, message: '请输入商品价格', trigger: 'blur'}],
      stock: [{required: true, message: '请输入商品库存', trigger: 'blur'}],
      categoryId: [{required: true, message: '请选择分类', trigger: 'change'}]
    }


    /**
     * 获取商品数据 — fetchProduct
     * GET /api/producuts/{id}
     * 编辑模式下加载已有商品数据：
     * 1. 检查是否为编辑模式
     * 2. 发送 GET 请求获取商品详情
     * 3. 将返回数据与默认值合并后赋值给 form
     *
     * 对象合并技巧 — { ...createForm(), ...response.data }：
     * - createForm() 提供所有字段的默认值
     * - response.data 覆盖有值的字段
     * - 这样即使后端返回的数据缺少某些字段，表单也不会报错
     */
    const fetchProduct = async () => {
      if(!isEdit.value){
        form.value = createForm()
        return 
      }
      const response = await http.get(`/products/${route.params.id}`)
      // 与默认值合并，避免后端返回数据缺少字段导致undefinded
      form.value = { ...createForm(), ...response.data}
    }



    /**
     * 重置表单 — resetForm
     *
     * 将表单数据恢复到初始状态：
     * - 使用 createForm() 工厂函数获取全新对象
     */
    const resetForm = () => {
      form.value = createForm()
    }


    /**
     * 提交表单 — submitProduct
     *
     * 提交流程：
     * 1. 调用 formRef.value?.validate() 触发全量验证
     * 2. 验证失败：catch 块直接 return，不执行后续逻辑
     * 3. 验证通过：根据 isEdit 模式调用不同 API
     *    - 编辑模式：PUT /api/products（更新）
     *    - 新增模式：POST /api/products（创建）
     * 4. 成功后显示提示并跳转回商品列表页
     *
     * try/catch 的两层使用：
     * - 外层 try/catch：捕获验证失败（validate 返回 rejected Promise）
     * - 内层 try/finally：确保 saving 状态一定会被重置
     */
    const submitProduct = async () => {
      try {
        // 触发全量验证，如果验证失败则会抛异常
        await formRef.value?.validate()
      } catch (err) {
        return // 如果验证失败直接返回，不执行后续的逻辑
      }
      saving.value = true
      try {
        if(isEdit.value){
          await http.put('/products', form.value)
        } else {
          await http.post('/products', form.value)
        }
        await alertMessage(isEdit? '商品修改成功': '商品新增成功')
        router.push('/products')
      } finally {
        saving.value = false
      }
    }



    /**
     * 触发文件选择器 — triggerImageUpload
     *
     * 通过 ref 获取隐藏的 input[type=file] 元素，调用其 click() 方法。
     * 这是自定义文件上传按钮的标准实现方式：
     * - 隐藏原生 input（display: none）
     * - 用自定义按钮触发 input 的 click 事件
     */
    const triggerImageUpload = () => {
      imageFileInput.value?.click()
    }



    /**
     * 处理图片上传 — handleImageUpload
     *
     * 上传流程：
     * 1. 获取用户选择的文件
     * 2. 验证文件类型（必须是图片）
     * 3. 验证文件大小（不超过 5MB）
     * 4. 创建 FormData 对象
     * 5. 发送 POST 请求上传到后端
     * 6. 将返回的图片 URL 赋值给 form.image
     *
     * FormData 的作用：
     * - 用于构建 multipart/form-data 格式的请求体
     * - 文件上传必须使用此格式
     * - append('file', file) 添加文件字段
     */
    const handleImageUpload = async (event) => {
      const file = event.target.files?.[0]
      if(!file) return
      if(!file.type.startsWith('image/')) {
        await alertMessage('请选择图片文件')
        return
      }
      if(file.size > 5 * 1024 * 1024){
        await alertMessage('图片大小不能超过5M')
        return
      }
      try {
        const formData = new FormData()
        formData.append('file', file)
        const response = await http.post('/upload/image', formData)
        form.value.image = response.data.url || response.data
      } catch (err) {
        await alertMessage(err.message || '图片上传失败')
      } finally {
        // 清空输入的值，避免选择同一个文件
        if(imageFileInput.value) imageFileInput.value.value = ''
      }
    }



    /**
     * 删除图片 — removeImage
     *
     * 将 form.image 设为空字符串，清除已选图片。
     * 模板会根据 form.image 的值切换显示"上传占位"或"图片预览"。
     */
    const removeImage = () => {form.value.image = ''}



    /**
     * 图片浏览器选择回调 — onImageSelected
     *
     * 当用户在 ImageBrowserDialog 中选择图片后触发：
     * - url 参数是用户选中的图片 URL
     * - 将其赋值给 form.value.image
     * - 弹窗自动关闭（子组件内部处理）
     */
    const onImageSelected = (url) => { form.value.image = url}
   



    /**
     * 图片地址解析 — resolveImage
     *
     * 将商品的图片地址解析为可显示的 URL。
     * 处理远程图片、占位图片、SVG 生成等不同情况。
     */
    const resolveImage = (item) => resolveProductImage(item)



    /**
     * Excel 文件选择回调 — handleUploadChange
     *
     * 当用户选择 Excel 文件后触发：
     * - file 参数是 el-upload 组件封装的文件对象
     * - file.raw 是原生 File 对象
     * - 清空之前的导入结果信息
     */
    const handleUploadChange = (file) => {
      selectedFile.value = file?.raw || null
      importResult.value = ''
      importError.value = ''
    }


    /**
     * 提交 Excel 导入 — submitImport
     *
     * 导入流程：
     * 1. 检查是否选择了文件
     * 2. 创建 FormData，添加文件
     * 3. 发送 POST 请求到 /api/products/import
     * 4. 成功：显示导入数量，清空文件选择
     * 5. 失败：显示错误信息
     */
    const submitImport = async () => {
      if(!selectedFile.value){
        importError.value = '请先选择要导入的Excel文件'
        return
      }
      importing.value = true
      importResult.value = ''
      importError.value = ''
      try {
        const formData = new FormData()
        formData.append('file', selectedFile.value)
        const response = await http.post('/products/import', formData)
        importResult.value = `导入成功：本次共新增${response.data.importedCount}个商品`
        selectedFile.value = null
        uploadRef.value?.clearFiles()
      } catch (error) {
        importError.value = error.message || 'Excel导入失败'
      } finally {
        importing.value = false
      }
    }
    


   
    /**
     * 下载导入模板 — downloadTemplate
     * GET /api/excel/productImportTemplate
     * 文件下载流程：
     * 1. 请求后端获取 Excel 模板文件（responseType: 'blob'）
     * 2. 创建 Blob 对象（二进制大对象）
     * 3. 创建临时下载 URL（URL.createObjectURL）
     * 4. 创建隐藏的 <a> 标签并触发点击下载
     * 5. 清理临时 URL 和 DOM 元素
     *
     * responseType: 'blob' 的作用：
     * - 告诉 Axios 将响应数据作为 Blob 对象处理
     * - 而非尝试解析为 JSON 或文本
     * - 适用于文件下载场景
     */
    const downloadTemplate = async () => {
        const response = await http.get('/excel/productImportTemplate', {
          responseType: 'blob'
        })
        const blob = new Blob([response], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        })
        // 创建临时下载URL
        const downloadUrl = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = downloadUrl
        link.download = 'product_import_template.xlsx'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(downloadUrl) // 释放临时URL
    }
  



    /**
     * 滚动到导入区域 — scrollToImportIfNeeded
     *
     * 检查 URL hash 是否为 #import，如果是则滚动到 Excel 导入区域。
     * 这是从商品列表页点击"Excel 批量导入"按钮跳转过来时的定位机制。
     *
     * nextTick 的作用：
     * - 等待 DOM 更新完成后再执行滚动
     * - 因为 ref 引用的元素可能还未渲染到 DOM 中
     */
    const scrollToImportIfNeeded = async () => {
      if( route.hash !== '#import') return
      await nextTick() 
      importSection.value?.$el?.scrollIntoView({
        behavior: 'smooth', block: 'start'
      })
    }



    /**
     * 监听路由参数变化 — watch
     *
     * 两个 watch：
     * 1. 监听 route.params.id：当 ID 变化时重新加载商品数据
     *    - 适用于从编辑商品 A 切换到编辑商品 B 的场景
     * 2. 监听 route.hash：当 hash 变化时尝试滚动到对应区域
     */
    watch(() => route.params.id, fetchProduct)
    watch(() => route.hash, scrollToImportIfNeeded)



    /**
     * 生命周期钩子 — onMounted
     *
     * 组件挂载后执行：
     * 1. 加载商品数据（编辑模式）
     * 2. 检查是否需要滚动到导入区域
     */
    onMounted(async () => {
      await fetchProduct()
      await scrollToImportIfNeeded()
    })

    // 返回所有状态和函数供 template 使用
    return {
      form,
      formRef,
      rules,
      categories,
      isEdit,
      saving,
      importing,
      importResult,
      importError,
      importSection,
      imageFileInput,
      imageUploading,
      showImageBrowser,
      uploadRef,
      selectedFile,
      submitProduct,
      resetForm,
      handleUploadChange,
      submitImport,
      downloadTemplate,
      triggerImageUpload,
      handleImageUpload,
      removeImage,
      onImageSelected,
      resolveImage
    }
  }
}
</script>

<style scoped>
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

.header-actions { display: flex; gap: 12px; align-items: center; }
/*
 * ============================================================
 * 【AdminProductEditorView.vue 样式教学注释】
 * ============================================================
 *
 * 1. 双栏布局（.editor-layout）：
 *    - CSS Grid：左侧表单 + 右侧导入
 *    - minmax(0, 1.3fr)：左侧最小 0，最大 1.3 倍份额
 *    - minmax(320px, 0.9fr)：右侧最小 320px，最大 0.9 倍份额
 *
 * 2. 图片上传区域（.image-upload-area）：
 *    - 虚线边框（border: 2px dashed）表示可交互区域
 *    - 悬停时边框变色，提示用户可以点击
 *    - 有图片时边框变为实线，表示已选择状态
 *
 * 3. 上传操作栏（.upload-actions）：
 *    - position: absolute 定位在图片底部
    - 半透明黑色背景 + backdrop-filter 毛玻璃效果
    - 悬浮在图片上方，不遮挡图片内容
 *
 * 4. AI 结果卡片（.ai-result-card）：
 *    - 浅色背景 + 主题色边框，与表单区分
 *    - 显示 SEO 标题和卖点亮点列表
 */

/* 双栏布局 */
.editor-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.9fr);
  gap: 24px;
}

/* 卡片圆角 */
.editor-card,
.import-card {
  border-radius: 16px;
}

/* 卡片标题 */
.card-title {
  font-size: 16px;
  font-weight: 700;
}

/* 标签行（AI 按钮在右侧） */
.label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

/* 区域标题行 */
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 区域描述文本 */
.section-desc {
  color: var(--el-text-color-secondary);
  line-height: 1.7;
  margin-bottom: 16px;
}

.section-desc code {
  background: var(--el-fill-color-light);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

/* 导入提示 */
.import-tips {
  margin-bottom: 16px;
}

.import-tips p {
  margin: 2px 0;
  font-size: 13px;
}

/* 导入操作区域 */
.import-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 导入结果提示 */
.import-msg {
  margin-top: 12px;
}

/* 图片上传区域 — 虚线边框 */
.image-upload-area {
  border: 2px dashed var(--el-border-color);
  border-radius: 12px;
  overflow: hidden;
  transition: border-color 0.2s;
  width: 100%;
  cursor: pointer;
}

.image-upload-area:hover {
  border-color: var(--el-color-primary);
}

/* 有图片时 — 实线边框 */
.image-upload-area.has-image {
  border-style: solid;
  cursor: default;
}

/* 上传占位区域 */
.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
  color: var(--el-text-color-secondary);
  transition: background 0.2s;
}

.upload-placeholder:hover {
  background: var(--el-fill-color-lighter);
}

/* 图片预览容器 */
.upload-preview {
  position: relative;
}

/* 预览图片 */
.preview-image {
  width: 100%;
  height: 240px;
  object-fit: cover;
  display: block;
}

/* 上传操作栏 — 悬浮在图片底部 */
.upload-actions {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.65);
  border-radius: 8px;
  backdrop-filter: blur(4px);
}

/* 上传提示文字 */
.upload-hint {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

/* 表单操作按钮 */
.form-actions {
  display: flex;
  gap: 12px;
}

/* AI 结果卡片 */
.ai-result-card {
  margin-top: 12px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-color-primary-light-7);
}

.ai-seo {
  font-size: 14px;
  margin-bottom: 8px;
}

.ai-highlights {
  font-size: 14px;
  margin-bottom: 10px;
}

.ai-highlights ul {
  margin: 6px 0 0 18px;
  padding: 0;
}

.ai-highlights li {
  color: var(--el-text-color-regular);
  line-height: 1.7;
}

/* 响应式：小屏幕下切换为单栏 */
@media (max-width: 1080px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }
}
</style>
