<template>
  <!--
    占位组件（STUB）  -->
  <div :class="embedded ? 'embedded-address-book' : 'page-shell'">
    <div v-if="!embedded" class="page-header">
      <h2>收货地址管理</h2>
    </div>
    <div class="placeholder-tip"></div>
    <router-link to="/checkout"> 去结算页 </router-link>
  </div>
  <div>
    <!-- 显示地址表单 -->
    <section>
      <h2>{{ editingId ? "编辑地址" : "新增地址" }}</h2>
      <form @submit.prevent="submitAddress">
        <!-- 收件人和手机号 -->
        <div>
          <label>收件人</label>
          <input
            v-model.trim="form.receiver"
            type="text"
            placeholder="请输入收件人姓名"
            required
            maxlength="20"
            @blur="validateField('receiver')"
          />
          <p v-if="fieldErrors.receiver">{{ fieldErrors.receiver }}</p>
        </div>
        <div>
          <label>手机号</label>
          <input
            v-model.trim="form.phone"
            type="tel"
            placeholder="请输入收件人手机号"
            required
            maxlength="11"
            @blur="validateField('phone')"
          />
          <p v-if="fieldErrors.phone">{{ fieldErrors.phone }}</p>
        </div>
        <!-- 省市区三级联动 -->
        <div>
          <div>
            <label>省份</label>
            <select
              v-model="form.province"
              @change="handleProvinceChange"
              required
            >
              <option value="">请选择省份</option>
              <option
                v-for="province in provinces"
                :key="province.name"
                :value="province.name"
              >
                {{ province.name }}
              </option>
            </select>
            <p v-if="fieldErrors.province">{{ fieldErrors.province }}</p>
          </div>
          <div>
            <label>城市</label>
            <select v-model="form.city" @change="handleCityChange" required>
              <option value="">请选择城市</option>
              <option
                v-for="city in cities"
                :key="city.name"
                :value="city.name"
              >
                {{ city.name }}
              </option>
            </select>
            <p v-if="fieldErrors.city">{{ fieldErrors.city }}</p>
          </div>
          <div>
            <label>区县</label>
            <select v-model="form.district" required>
              <option value="">请选择区县</option>
              <option
                v-for="district in districts"
                :key="district.name"
                :value="district.name"
              >
                {{ district.name }}
              </option>
            </select>
            <p v-if="fieldErrors.district">{{ fieldErrors.district }}</p>
          </div>
        </div>

        <!-- 详细地址 -->
        <div>
          <label>详细地址</label>
          <textarea v-model.trim="form.detailAddress" rows="2"
           placeholder="请输入街道、门牌等信息" required maxlength="200"
           @blur="validateField('detailAddress')"/>
          <p v-if="fieldErrors.detailAddress">{{ fieldErrors.detailAddress }}</p> 
        </div>
        <!-- 设置为默认地址 -->
        <label>
          <input v-model="form.isDefault" type="checkbox">
          <span>设为默认地址</span>
        </label>
        <!-- 按钮 -->
        <div>
          <button class="btn btn-primary" type="submit" :disabled="submitting">
            {{ submitting ? '保存中...' : editingId ? '保存修改': '新增地址' }}
          </button>
          <button v-if="editingId" class="btn btn-secondary"
           type="button" @click="resetForm">
            取消编辑
          </button>
        </div>
        <p v-if="error"> {{ error }}</p>
      </form>
    </section>

    <!-- 地址列表：用AI帮助我们生成，并设置css样式 -->
  </div>
</template>

<script>
export default {
  name: "AddressBookView",

  props: {
    embedded: {
      type: Boolean,
      default: false,
      description: "是否以嵌入模式渲染（购物车页传入 true）",
    },
  },
};
</script>

<style scoped>
.placeholder-tip {
  padding: 16px;
  border: 1px dashed #d9a;
  border-radius: 8px;
  color: #8a6d3b;
  background: #fffaf0;
  line-height: 1.8;
}
.placeholder-tip code {
  background: #f4e9d8;
  padding: 1px 6px;
  border-radius: 4px;
}
</style>
