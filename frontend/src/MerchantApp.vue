<template>
  <div id="merchant-app">
    <nav class="merchant-nav">
      <div class="nav-brand">
        <router-link to="/dashboard">商家中心</router-link>
      </div>
      <div class="nav-links">
        <router-link to="/dashboard">数据看板</router-link>
        <router-link to="/products">商品管理</router-link>
        <router-link to="/reviews">评论管理</router-link>
        <router-link to="/store">店铺信息</router-link>
      </div>
      <div class="nav-user">
        <span v-if="user">{{ user.username }}</span>
        <button v-if="user" @click="logout" class="btn-logout">退出</button>
        <router-link v-else to="/login" class="btn-login">登录</router-link>
      </div>
    </nav>
    <main class="merchant-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
// 商家后台应用根组件
// 职责：
// 1. 作为商家后台的顶层容器组件
// 2. 集成顶部导航栏，包含商家菜单
// 3. 通过 router-view 渲染商家后台的各个子页面

import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()
const { user } = storeToRefs(authStore)        // ★ 从 Pinia store 解构用户信息（响应式）

async function logout() {
  await authStore.logout()
  router.push('/login')
}
</script>
