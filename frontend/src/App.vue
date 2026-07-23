<template>
  <div id="app">
  <!--导航栏-->
  <nav class="navbar">
    <div class="nav-container">
      <router-link to="/" class="nav-logo">电商平台</router-link>
      <div class="nav-links">
        <router-link to="/">首页</router-link>
        <router-link to="/products">商品</router-link>
        <!--登录成功显示：购物车、订单、用户名、退出-->
        <template v-if="isLoggedIn">
          <router-link to="/cart">购物车</router-link>
          <router-link to="/orders">订单</router-link>
          <span class="nav-user">{{ username }}</span>
          <button class="nav-btn" @click="logout">退出</button>
        </template>

        <!--未登录显示：登录、注册-->
        <template v-else>
          <router-link to="/login">登录</router-link>
          <router-link to="/register">注册</router-link>
        </template>
      </div>
    </div>
  </nav>

  <!--路由出口-->
  <!--路由视图出口的作用是，匹配到的页面组件会渲染在这里-->
  <main class="main-content">
      <router-view></router-view>
    </main>
  </div>
</template>


<script>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { getToken, getUser, removeUser} from './utils/auth'

//创建路由实例
const router = useRouter()

//一、处理isLoggedIn变量
//获取Token，判断用户是否登录，获取到token表示有token，及为登录状态
const isLoggedIn = computed(()=> !!getToken())

//二、处理username变量
//vue中的计算属性 computed
const username = computed(()=>{
  //读取本地存储的用户对象
  const user = getUser()
  //如果用户对象存在，则返回用户名，否则返回空字符串
  return user ? user.username : ''
})

//三、处理退出按钮（定义logout方法）
const logout = ()=>{
    //删除本地存储的登录凭证token
    removeToken()
    //删除本地存储的用户信息
    removeUser()
    //使用路由跳转到登录页面
    router.push("/login")
}
</script>


