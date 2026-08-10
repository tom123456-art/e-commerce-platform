<template>
  <div :class="embedded ? 'embedded-address-book' : 'page-shell'">
    <!-- 独立页面时的页头 -->
    <header v-if="!embedded" class="page-header">
      <h2>收货地址管理</h2>
      <p class="page-subtitle">管理您的收货地址，下单时更快捷</p>
    </header>

    <div class="address-layout">
      <!-- ================= 新增 / 编辑 表单 ================= -->
      <section class="card address-form-card">
        <div class="card-head">
          <h3 class="card-title">{{ editingId ? '编辑地址' : '新增地址' }}</h3>
          <p class="card-desc">{{ editingId ? '修改已有收货地址' : '填写并保存一个新的收货地址' }}</p>
        </div>

        <form class="address-form" @submit.prevent="submitAddress">
          <!-- 收件人和手机号 -->
          <div class="form-grid-2">
            <div class="form-group">
              <label for="receiver">收件人</label>
              <input
                id="receiver"
                v-model.trim="form.receiver"
                type="text"
                placeholder="请输入收件人姓名"
                required
                maxlength="20"
                @blur="validateField('receiver')"
              />
              <p v-if="fieldErrors.receiver" class="field-error">{{ fieldErrors.receiver }}</p>
            </div>
            <div class="form-group">
              <label for="phone">手机号</label>
              <input
                id="phone"
                v-model.trim="form.phone"
                type="tel"
                placeholder="请输入收件人手机号"
                required
                maxlength="11"
                @blur="validateField('phone')"
              />
              <p v-if="fieldErrors.phone">{{ fieldErrors.phone }}</p>
            </div>
          </div>

          <!-- 省市区三级联动 -->
          <div class="region-row">
            <div class="form-group">
              <label for="province">省份</label>
              <select
                id="province"
                v-model="form.province"
                class="region-select"
                @change="handleProvinceChange"
                required
              >
                <option value="">请选择省份</option>
                <option
                  v-for="p in provinces"
                  :key="p.name"
                  :value="p.name"
                >
                  {{ p.name }}
                </option>
              </select>
              <p v-if="fieldErrors.province" class="field-error">{{ fieldErrors.province }}</p>
            </div>
            <div class="form-group">
              <label for="city">城市</label>
              <select
                id="city"
                v-model="form.city"
                class="region-select"
                :disabled="!form.province"
                @change="handleCityChange"
                required
              >
                <option value="">请选择城市</option>
                <option
                  v-for="c in cities"
                  :key="c.name"
                  :value="c.name"
                >
                  {{ c.name }}
                </option>
              </select>
              <p v-if="fieldErrors.city" class="field-error">{{ fieldErrors.city }}</p>
            </div>
            <div class="form-group">
              <label for="district">区县</label>
              <select
                id="district"
                v-model="form.district"
                class="region-select"
                :disabled="!form.city"
                required
              >
                <option value="">请选择区县</option>
                <option
                  v-for="d in districts"
                  :key="d"
                  :value="d"
                >
                  {{ d }}
                </option>
              </select>
              <p v-if="fieldErrors.district" class="field-error">{{ fieldErrors.district }}</p>
            </div>
          </div>

          <!-- 详细地址 -->
          <div class="form-group">
            <label for="detailAddress">详细地址</label>
            <textarea
              id="detailAddress"
              v-model.trim="form.detailAddress"
              rows="2"
              placeholder="请输入街道、门牌等信息"
              required
              maxlength="200"
              @blur="validateField('detailAddress')"
            ></textarea>
            <p v-if="fieldErrors.detailAddress" class="field-error">{{ fieldErrors.detailAddress }}</p>
          </div>

          <!-- 设置为默认地址 -->
          <label class="checkbox-line">
            <input v-model="form.isDefault" type="checkbox" />
            <span>设为默认地址</span>
          </label>

          <!-- 按钮 -->
          <div class="form-actions">
            <button class="btn btn-primary" type="submit" :disabled="submitting">
              {{ submitting ? '保存中...' : editingId ? '保存修改' : '新增地址' }}
            </button>
            <button
              v-if="editingId"
              class="btn btn-secondary"
              type="button"
              @click="resetForm"
            >
              取消编辑
            </button>
          </div>

          <p v-if="error" class="form-error">{{ error }}</p>
        </form>
      </section>

      <!-- ================= 地址列表 ================= -->
      <section class="address-list-section">
        <div class="list-head">
          <h3 class="card-title">我的地址</h3>
          <span class="address-count">{{ addresses.length }} 条</span>
        </div>

        <div v-if="loading" class="state-tip">加载中...</div>
        <div v-else-if="error && addresses.length === 0" class="state-error">{{ error }}</div>
        <div v-else-if="addresses.length === 0" class="state-empty">
          还没有收货地址，快在左侧添加一个吧～
        </div>

        <ul v-else class="address-list">
          <li
            v-for="addr in addresses"
            :key="addr.id"
            class="address-item"
            :class="{ 'is-default': !!addr.isDefault }"
          >
            <div class="addr-main">
              <div class="addr-line1">
                <span class="addr-receiver">{{ addr.receiver }}</span>
                <span class="addr-phone">{{ addr.phone }}</span>
                <span v-if="addr.isDefault" class="default-badge">默认</span>
              </div>
              <p class="addr-detail">
                {{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.detailAddress }}
              </p>
            </div>
            <div class="addr-actions">
              <button
                v-if="!addr.isDefault"
                class="text-btn"
                type="button"
                @click="setDefault(addr.id)"
              >设为默认</button>
              <button class="text-btn" type="button" @click="editAddress(addr)">编辑</button>
              <button class="text-btn danger" type="button" @click="deleteAddress(addr.id)">删除</button>
            </div>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script>
/**
 * AddressBookView.vue —— 收货地址管理（新增/编辑/列表/删除/设默认）
 *
 * 设计要点：
 * 1. 省市区三级联动直接复用 utils/regions 的 REGION_DATA / getCities / getDistricts
 * 2. 后端接口（baseURL=/api）：
 *    - GET    /addresses            获取当前用户地址列表
 *    - POST   /addresses            新增地址
 *    - POST   /addresses/{id}       修改地址
 *    - DELETE /addresses/{id}       删除地址
 *    - PUT    /addresses/{id}/default  设为默认
 *    http 拦截器已经把响应体（Result 包装）直接返回，故取 res.data 拿业务数据
 * 3. 表单校验：单字段 @blur 校验 + 提交时整体校验（与 RegisterView 同模式）
 */
import { reactive, ref, computed, onMounted } from 'vue'
import http from '../utils/http'
import { alertMessage, confirmDialog } from '../utils/modal'
import { REGION_DATA, getCities, getDistricts } from '../utils/regions'

// 手机号正则：1 开头 + 10 位数字 = 11 位（与后端 AddressRequest 保持一致）
const phonePattern = /^1[3-9]\d{9}$/

export default {
  name: 'AddressBookView',

  props: {
    embedded: {
      type: Boolean,
      default: false,
      description: '是否以嵌入模式渲染（购物车页传入 true）',
    },
  },

  setup(props) {
    // 省份列表（REGION_DATA 本身就是省数组）
    const provinces = REGION_DATA

    // 列表与状态
    const addresses = ref([])
    const loading = ref(false)
    const error = ref('')
    const submitting = ref(false)
    const editingId = ref(null)

    // 表单数据
    const emptyForm = () => ({
      receiver: '',
      phone: '',
      province: '',
      city: '',
      district: '',
      detailAddress: '',
      isDefault: false,
    })
    const form = reactive(emptyForm())

    // 字段级错误信息
    const fieldErrors = reactive({
      receiver: '',
      phone: '',
      province: '',
      city: '',
      district: '',
      detailAddress: '',
    })

    // 联动：城市、区县随已选省份/城市变化
    const cities = computed(() => getCities(form.province))
    const districts = computed(() => getDistricts(form.province, form.city))

    // 切换省份：清空城市与区县，避免脏数据
    const handleProvinceChange = () => {
      form.city = ''
      form.district = ''
      fieldErrors.province = ''
    }
    // 切换城市：清空区县
    const handleCityChange = () => {
      form.district = ''
      fieldErrors.city = ''
    }

    // 获取地址列表
    const fetchAddresses = async () => {
      loading.value = true
      error.value = ''
      try {
        const res = await http.get('/addresses')
        addresses.value = res.data || []
      } catch (err) {
        error.value = err.message || '获取地址列表失败'
      } finally {
        loading.value = false
      }
    }

    // 单字段校验（@blur 调用）
    const validateField = (field) => {
      switch (field) {
        case 'receiver':
          fieldErrors.receiver = form.receiver ? '' : '请输入收件人姓名'
          break
        case 'phone':
          fieldErrors.phone = phonePattern.test(form.phone) ? '' : '请输入正确的 11 位手机号'
          break
        case 'province':
          fieldErrors.province = form.province ? '' : '请选择省份'
          break
        case 'city':
          fieldErrors.city = form.city ? '' : '请选择城市'
          break
        case 'district':
          fieldErrors.district = form.district ? '' : '请选择区县'
          break
        case 'detailAddress':
          fieldErrors.detailAddress = form.detailAddress ? '' : '请输入详细地址'
          break
      }
    }

    // 整体校验（提交前调用），返回第一个错误信息或空串
    const validateForm = () => {
      ;['receiver', 'phone', 'province', 'city', 'district', 'detailAddress'].forEach(validateField)
      return Object.values(fieldErrors).find((msg) => msg) || ''
    }

    // 构造提交给后端的数据
    const buildPayload = () => ({
      receiver: form.receiver,
      phone: form.phone,
      province: form.province,
      city: form.city,
      district: form.district,
      detailAddress: form.detailAddress,
      isDefault: form.isDefault,
    })

    // 提交：有 editingId 走修改，否则走新增
    const submitAddress = async () => {
      error.value = ''
      const validationMessage = validateForm()
      if (validationMessage) {
        error.value = validationMessage
        return
      }

      submitting.value = true
      try {
        if (editingId.value) {
          await http.post(`/addresses/${editingId.value}`, buildPayload())
        } else {
          await http.post('/addresses', buildPayload())
        }
        await fetchAddresses()
        resetForm()
        await alertMessage(editingId.value ? '地址已更新' : '地址已新增')
      } catch (err) {
        error.value = err.message || '保存失败，请稍后重试'
      } finally {
        submitting.value = false
      }
    }

    // 重置表单与编辑态
    const resetForm = () => {
      Object.assign(form, emptyForm())
      Object.keys(fieldErrors).forEach((key) => {
        fieldErrors[key] = ''
      })
      editingId.value = null
    }

    // 进入编辑态：把地址回填到表单
    const editAddress = (addr) => {
      editingId.value = addr.id
      Object.assign(form, {
        receiver: addr.receiver,
        phone: addr.phone,
        province: addr.province,
        city: addr.city,
        district: addr.district,
        detailAddress: addr.detailAddress,
        isDefault: !!addr.isDefault,
      })
      Object.keys(fieldErrors).forEach((key) => {
        fieldErrors[key] = ''
      })
    }

    // 删除地址（带确认）
    const deleteAddress = async (id) => {
      const confirmed = await confirmDialog('确认删除该地址吗？删除后无法恢复。')
      if (!confirmed) return
      try {
        await http.delete(`/addresses/${id}`)
        await fetchAddresses()
      } catch (err) {
        await alertMessage(err.message || '删除失败')
      }
    }

    // 设为默认
    const setDefault = async (id) => {
      try {
        await http.put(`/addresses/${id}/default`)
        await fetchAddresses()
      } catch (err) {
        await alertMessage(err.message || '设置默认地址失败')
      }
    }

    onMounted(fetchAddresses)

    return {
      embedded: props.embedded,
      provinces,
      addresses,
      loading,
      error,
      submitting,
      editingId,
      form,
      fieldErrors,
      cities,
      districts,
      handleProvinceChange,
      handleCityChange,
      validateField,
      submitAddress,
      resetForm,
      editAddress,
      deleteAddress,
      setDefault,
    }
  },
}
</script>

<style scoped>
/* ===== 布局容器 ===== */
.page-shell {
  max-width: 1080px;
  margin: 0 auto;
  padding: 28px 24px 48px;
}

.embedded-address-book {
  margin: 0;
  padding: 0;
}

.page-header {
  margin-bottom: 18px;
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 700;
  color: #232d42;
}

.page-subtitle {
  margin: 0;
  color: #687087;
  font-size: 15px;
}

/* 两栏：表单（左，加宽） + 列表（右，仅此列收窄），整体居中避免贴左 */
.address-layout {
  display: grid;
  grid-template-columns: minmax(440px, 540px) minmax(300px, 420px);
  gap: 18px;
  align-items: start;
  justify-content: center;
}

/* ===== 卡片通用 ===== */
.card {
  background: #fff;
  border-radius: 18px;
  padding: 22px;
  box-shadow: 0 18px 45px rgba(50, 77, 135, 0.08);
}

.card-head {
  margin-bottom: 18px;
}

.card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #232d42;
}

.card-desc {
  margin: 6px 0 0;
  color: #687087;
  font-size: 13px;
}

/* ===== 表单 ===== */
.address-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.region-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

label {
  font-size: 0.92rem;
  font-weight: 700;
  color: #232d42;
}

input,
select,
textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #dfe7ff;
  border-radius: 14px;
  background: #f8fbff;
  color: #232d42;
  font-size: 0.95rem;
  font-family: inherit;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

input:focus,
select:focus,
textarea:focus {
  outline: none;
  border-color: rgba(64, 158, 255, 0.55);
  box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.12);
  background: #fff;
}

select:disabled {
  background: #f2f4f8;
  color: #aeb6c2;
  cursor: not-allowed;
}

textarea {
  resize: vertical;
  line-height: 1.6;
}

.checkbox-line {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.92rem;
  font-weight: 600;
  color: #232d42;
  cursor: pointer;
}

.checkbox-line input {
  width: 16px;
  height: 16px;
  accent-color: #409eff;
}

.field-error {
  margin: 0;
  font-size: 13px;
  color: #e93f3f;
}

.form-error {
  margin: 0;
  padding: 11px 14px;
  border-radius: 12px;
  font-size: 0.92rem;
  text-align: center;
  border: 1px solid rgba(233, 63, 63, 0.12);
  background: rgba(233, 63, 63, 0.08);
  color: #e93f3f;
}

/* 按钮与全局按钮样式（与系统其它页面保持一致） */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  border-radius: 10px;
  padding: 12px 18px;
  font-size: 15px;
  font-weight: 600;
  border: 1px solid transparent;
  cursor: pointer;
  transition: transform 0.12s, background 0.12s, box-shadow 0.12s, filter 0.12s;
}

.btn:active {
  transform: translateY(1px);
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.btn-primary {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
  box-shadow: 0 10px 28px rgba(64, 158, 255, 0.18);
}

.btn-primary:hover {
  filter: brightness(0.94);
}

.btn-secondary {
  background: #fff;
  color: #232d42;
  border-color: #d9e2ef;
}

.btn-secondary:hover {
  background: #f5f7fb;
}

.form-actions {
  display: flex;
  gap: 12px;
}

.form-actions .btn {
  width: auto;
  flex: 1;
}

/* ===== 地址列表 ===== */
.list-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.address-count {
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
  font-size: 12px;
  font-weight: 700;
}

.state-tip,
.state-error,
.state-empty {
  padding: 28px 20px;
  text-align: center;
  border-radius: 14px;
  font-size: 0.95rem;
}

.state-tip {
  color: #687087;
  background: #f8fbff;
}

.state-error {
  color: #d12d2d;
  background: #fff3f3;
}

.state-empty {
  color: #687087;
  background: #f8fbff;
  border: 1px dashed #d9e2ef;
}

.address-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 12px;
}

.address-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 16px;
  background: #fbfbfd;
  border: 1px solid rgba(64, 158, 255, 0.1);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.address-item:hover {
  border-color: rgba(64, 158, 255, 0.35);
  box-shadow: 0 10px 26px rgba(64, 158, 255, 0.1);
}

.address-item.is-default {
  border-color: rgba(64, 158, 255, 0.55);
  background: #f3f8ff;
}

.addr-main {
  min-width: 0;
}

.addr-line1 {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 6px;
}

.addr-receiver {
  font-size: 16px;
  font-weight: 700;
  color: #232d42;
}

.addr-phone {
  font-size: 14px;
  color: #687087;
}

.default-badge {
  padding: 2px 9px;
  border-radius: 999px;
  background: #409eff;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.addr-detail {
  margin: 0;
  font-size: 14px;
  color: #4f566b;
  line-height: 1.6;
  word-break: break-all;
}

.addr-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.text-btn {
  border: none;
  background: none;
  color: #409eff;
  font-size: 14px;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 8px;
  transition: background 0.15s ease, color 0.15s ease;
}

.text-btn:hover {
  background: rgba(64, 158, 255, 0.1);
}

.text-btn.danger {
  color: #e93f3f;
}

.text-btn.danger:hover {
  background: rgba(233, 63, 63, 0.1);
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .address-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .page-shell {
    padding: 20px 16px 36px;
  }

  .card {
    padding: 18px 16px;
  }

  .form-grid-2,
  .region-row {
    grid-template-columns: 1fr;
  }

  .address-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .addr-actions {
    align-self: stretch;
    justify-content: flex-end;
  }
}
</style>
