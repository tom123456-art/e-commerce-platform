/**
 * 前端页面路由配置
 */

import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/auth'

//定义路由规则 []数组类型
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
//to目标路由（你要跳转到哪里） from当前路由（从哪个页面跳转）
//next放行函数，必须调用，控制是否允许跳转
router.beforeEach((to, from, next) => {
    //判断：目标页面需要登录权限并且没有token（未登录）
    if(to.meta.requiresAuth && !getToken()){
        //跳转到登录页面
        next('/login')
    }else{
        //放行
        next()
    }
})
//导出路由实例
export default router