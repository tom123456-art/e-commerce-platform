<template>
  <div id="app" class="app-shell">
    <!-- 品牌 Logo + 导航菜单（略） -->
    <nav class="navbar">
      <div class="nav-container">
          <router-link to="/" class="nav-logo">电商平台</router-link> 
          <div class="nav-links">
            <router-link to="/" class="nav-link">首页</router-link>
            <router-link to="/products" class="nav-link">
              商品
            </router-link>
            <template v-if="loggedIn">
              <router-link to="/cart" class="nav-link">
                购物车
              </router-link>
              <router-link to="/orders" class="nav-link">
                我的订单
              </router-link>
              <span class="nav-user">欢迎，{{ username }}</span>
              <button class="nav-btn" @click="logout">退出</button>
            </template>
            <template v-else>
              <router-link to="/login" class="nav-link">
                登录
              </router-link>
              <router-link to="/register" class="nav-link">
                注册
              </router-link>
            </template>
          </div>
      </div>
    </nav>
      


    <!-- 路由出口：当前页面组件渲染在这里 -->
    <main class="content-shell">
      <router-view />
    </main>
  </div>
</template>

<script>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// 导入认证工具函数（来自 utils/auth.js）
import { clearAuth, getCurrentUser, isLoggedIn } from './utils/auth'

export default {
  name: 'App',
  setup() {
    const route = useRoute()
    const router = useRouter()

    // 响应式状态：登录状态和当前用户
    const loggedIn = ref(false)
    const currentUser = ref(null)

    /**
     * syncAuthState：将 localStorage 中的登录态同步到响应式变量
     *
     * 为什么需要这个函数？
     * - localStorage 变化不会自动触发 Vue 视图更新
     * - 需要手动读取并更新 ref，导航栏才会刷新
     *
     * 调用时机：组件挂载时、路由变化时、登出后
     */
    const syncAuthState = () => {
      loggedIn.value = isLoggedIn()
      currentUser.value = getCurrentUser()
    }

    /**
     * username：计算属性，优先显示昵称，其次用户名
     */
    const username = computed(() => {
      if (!currentUser.value) return ''
      return currentUser.value.nickname || currentUser.value.username || '已登录'
    })

    /**
     * logout：退出登录
     * 1. await clearAuth() -- 清除 localStorage + 通知后端注销
     * 2. syncAuthState() -- 同步状态，导航栏立即变回"登录/注册"
     * 3. router.push('/login') -- 跳转到登录页
     */
    const logout = async () => {
      await clearAuth()
      syncAuthState()
      router.push('/login')
    }

    // 路由变化时重新同步认证状态（确保导航栏显示正确的登录信息）
    watch(() => route.fullPath, syncAuthState)

    // 组件挂载时同步认证状态（页面刷新后从 localStorage 恢复登录态）
    onMounted(syncAuthState)

    return {
      loggedIn,
      username,
      logout
    }
  }
}
</script>