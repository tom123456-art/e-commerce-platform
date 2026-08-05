/**
 * 前端页面路由配置
 */

import { createRouter, createWebHistory } from 'vue-router'
import { getToken, isLoggedIn } from '../utils/auth'

//定义路由规则  []是数据类型
const routes = [
    //首页
    {
        path: '/',
        name: 'Home',
        component: ()=> import('../views/HomeView.vue')
    },
    //登录
    {
        path: '/login',
        name: 'Login',
        component: ()=> import('../views/LoginView.vue')
    },
    //注册
    {
        path: '/register',
        name: 'Register',
        component: ()=> import('../views/RegisterView.vue'),
        meta: { gusetOnly: true } // 仅限游客访问
    }
    ,
    // 商品浏览
    // {
    //     path: '/products',
    //     name: 'Products',
    //     component: ()=> import('../views/ProductsView.vue')
    // },
    // {
    //     path: '/products/:id',
    //     name: 'Product',
    //     component: ()=> import('../views/ProductDetailView.vue')
    // },
    // // AI功能
    // {
    //     path: '/ai-chat',
    //     name: 'AIChat',
    //     component: ()=> import('../views/AIChatView.vue'),
    //     meta: { requiresAuth: true } // 需要登录权限
    // },
    // {
    //     path: '/cart',
    //     name: 'Cart',
    //     component: ()=> import('../views/CartView.vue'),
    //     meta: { requiresAuth: true } // 需要登录权限
    // },
    // {   
    //     path: '/orders',
    //     name: 'Orders',
    //     component: ()=> import('../views/OrdersView.vue'),
    //     meta: { requiresAuth: true } // 需要登录权限
    // },
    {
        path: '/:pathMatch(.*)*', // 匹配所有未定义的路由
        redirect: '/' // 重定向到首页
    }
]

//创建路由实例
const router = createRouter({
    //使用HTML5 history模式（地址栏不带#）
    history: createWebHistory(),
    //传入上面定义的路由规则数组
    routes
})

//全局前置路由守卫 -- 每次路由跳转之前自动执行
//to目标路由（你要跳转到哪里）   from当前路由（从哪个页面跳转）  
//next放行函数，必须调用，控制是否允许跳转
router.beforeEach((to,from,next)=>{
    const loggedIn = isLoggedIn() // 检查用户是否已登录

    //判断：需要登录的页面，如果没有登录的话就跳转到登录页面，
    // 并且携带Redirect参数，登录成功后可以跳转回原页面
    if(to.meta.requiresAuth && !loggedIn){
        //不允许进入，强制跳转到登录页面
        next({path: '/login', query: { redirect: to.fullPath }})
        return
    }
    // 游客专属的页面（登录注册），如果已登录则跳转到首页
    if (to.meta.gusetOnly && loggedIn) {
        next({ path: '/' })
        return
    }
    // 默认放行
    next()
})

//导出路由实例
export default router