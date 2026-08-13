<template>
    <div class="admin-layout">
        <!-- 侧边栏 -->
         <aside class="admin-sidebar">
            <!-- 品牌LOGO -->
            <div class="brand">
                <img src="/images/brand/admin-mark.svg" alt="管理后台">
                <div class="brand-text">
                    <strong>商城后台</strong>
                    <p>Admin Console</p>
                </div>
            </div>
            <!-- 导航栏 -->
            <nav class="admin-nav">
                <router-link v-for="item in navItems" :key="item.to" :to="item.to">
                    {{ item.label }}
                </router-link>
            </nav>

            <!-- 按钮 -->
            <div class="sidebar-actions">
                <a :href="shopHomeUrl">返回商城</a>
                <button @click="handleLogout">退出登录</button>
            </div>
         </aside>
         <!-- 主内容区域 -->
          <section class="admin-main">
            <header class="admin-header">
                <div class="header-left">
                    <span> Admin</span>
                    <h1>后台管理系统</h1>
                </div>
                <span class="user-badge"> {{ userDisplay }}</span>
            </header>
            <!-- 子路由 -->
             <main class="admin-content">
                <router-view />
             </main>
          </section>
    </div>
</template>

<script>
import { useRouter } from 'vue-router';
import { clearAuth, getCurrentUser } from '../../utils/auth';
import { computed } from 'vue';
import { buildShopUrl } from '../../utils/appLinks';

export default{
    name: 'AdminLayout',
    setup() {
        const router = useRouter()
        const currentUser = getCurrentUser()
        // 配置导航栏菜单
        const navItems = [
            {to: '/', label: '运营看板'},
            {to: '/showcase-strategy', label: '推荐策略'},
            {to: '/users', label: '用户管理'},
            {to: '/products', label: '商品管理'},
            {to: '/orders', label: '订单管理'},
        ]

        // 用户名显示
        const userDisplay = computed(() => {
            return currentUser?.nickname || currentUser?.username || '管理员'
        })

        const handleLogout = async () => {
            await clearAuth()
            router.push('/login')
        }
        return{
            navItems, userDisplay, handleLogout, 
            shopHomeUrl: buildShopUrl('/') // 构建商城首页的完整URL
        }
    }
}
</script>

<style scoped>
:root{
  --admin-bg: #f6f8fb;
  --sidebar-bg: #0b1220;
  --accent: #3b82f6; /* blue-500 */
  --muted: rgba(255,255,255,0.72);
}

/* Layout */
.admin-layout {
  display: flex;
  min-height: 100vh;
  font-family: Inter, "Helvetica Neue", Arial, sans-serif;
  background: var(--admin-bg);
  color: #0f172a;
}

/* Sidebar */
.admin-sidebar {
  width: 260px;
  background: var(--sidebar-bg);
  color: #fff;
  padding: 22px;
  box-shadow: 2px 0 10px rgba(11,18,32,0.08);
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.admin-sidebar .brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-sidebar img {
  width: 44px;
  height: 44px;
  object-fit: contain;
}

.admin-sidebar strong {
  font-size: 16px;
  line-height: 1;
}

.admin-sidebar p {
  margin: 0;
  font-size: 12px;
  color: var(--muted);
}

/* Navigation */
.admin-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 6px;
  flex: 1 1 auto;
}

.admin-nav a {
  color: rgba(255,255,255,0.92);
  text-decoration: none;
  padding: 10px 12px;
  border-radius: 8px;
  transition: background-color .15s ease, color .15s ease;
  font-weight: 500;
  display: inline-block;
}

/* router-link active state */
.admin-nav a.router-link-active {
  background: rgba(255,255,255,0.06);
  color: #fff;
  box-shadow: inset 3px 0 0 var(--accent);
}

.admin-nav a:hover {
  background: rgba(255,255,255,0.04);
}

/* Footer actions in the sidebar */
.admin-sidebar .sidebar-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
}

.admin-sidebar a {
  color: var(--accent);
  text-decoration: none;
  padding: 6px 10px;
  border-radius: 6px;
  background: transparent;
  font-size: 13px;
}

.admin-sidebar button {
  background: #ef4444; /* red-500 */
  border: none;
  color: #fff;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

.admin-sidebar button:hover { opacity: .95; }

/* Main area */
.admin-main {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #fff 0%, #fbfdff 100%);
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 28px;
  border-bottom: 1px solid rgba(15,23,42,0.04);
  background: rgba(255,255,255,0.6);
}

.admin-header .header-left span {
  display: block;
  color: #6b7280;
  font-size: 13px;
}

.admin-header h1 {
  margin: 0;
  font-size: 20px;
  color: #0f172a;
}

.admin-header .user-badge {
  color: #0f172a;
  font-weight: 600;
  background: #f1f5f9;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 13px;
}

.admin-content {
  padding: 20px 28px;
  flex: 1 1 auto;
  overflow: auto;
  background: transparent;
}

/* Responsive */
@media (max-width: 900px) {
  .admin-sidebar { width: 72px; padding: 14px; }
  .admin-nav a { padding: 8px 10px; font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .admin-header h1 { font-size: 16px; }
}

/* Small tweaks to ensure nested router-view content has breathing room */
.admin-content > * { max-width: 1200px; margin: 0 auto; }
</style>