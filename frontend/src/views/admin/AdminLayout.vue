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
/* ============================================================
 * AdminLayout.vue 样式 - 管理后台整体框架
 * 设计方向：现代 SaaS 后台（深色渐变侧边栏 + 浅色内容区）
 * ============================================================ */
.admin-layout {
  /* 在 .admin-layout 上定义变量，避免 scoped 下 :root 不生效 */
  --admin-bg: #f4f6fb;
  --sidebar-bg-1: #0f172a;
  --sidebar-bg-2: #1e293b;
  --accent: #4f7cff;
  --accent-soft: rgba(79, 124, 255, 0.14);
  --text-main: #0f172a;
  --border: rgba(15, 23, 42, 0.06);

  display: flex;
  min-height: 100vh;
  font-family: Inter, "Helvetica Neue", Arial, "PingFang SC", "Microsoft YaHei", sans-serif;
  background: var(--admin-bg);
  color: var(--text-main);
}

/* ---------- 侧边栏 ---------- */
.admin-sidebar {
  width: 264px;
  flex-shrink: 0;
  background: linear-gradient(180deg, var(--sidebar-bg-1) 0%, var(--sidebar-bg-2) 100%);
  color: #fff;
  padding: 24px 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
  box-shadow: 2px 0 18px rgba(11, 18, 32, 0.10);
}

.admin-sidebar .brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 6px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.admin-sidebar img {
  width: 42px;
  height: 42px;
  object-fit: contain;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.30));
}

.admin-sidebar .brand-text { line-height: 1.3; }

.admin-sidebar strong {
  display: block;
  font-size: 16px;
  letter-spacing: 0.5px;
}

.admin-sidebar p {
  margin: 2px 0 0;
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

/* ---------- 导航 ---------- */
.admin-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1 1 auto;
  margin-top: 4px;
}

.admin-nav a {
  position: relative;
  color: rgba(255, 255, 255, 0.82);
  text-decoration: none;
  padding: 11px 14px 11px 18px;
  border-radius: 10px;
  font-weight: 500;
  font-size: 14px;
  transition: background-color .18s ease, color .18s ease;
}

.admin-nav a::before {
  content: "";
  position: absolute;
  left: 6px;
  top: 50%;
  transform: translateY(-50%) scaleY(0);
  width: 3px;
  height: 16px;
  border-radius: 999px;
  background: var(--accent);
  transition: transform .18s ease;
}

.admin-nav a:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.admin-nav a.router-link-active {
  background: linear-gradient(90deg, var(--accent-soft), rgba(79, 124, 255, 0.03));
  color: #fff;
  font-weight: 600;
}

.admin-nav a.router-link-active::before { transform: translateY(-50%) scaleY(1); }

/* ---------- 侧边栏底部操作 ---------- */
.admin-sidebar .sidebar-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.admin-sidebar .sidebar-actions a {
  color: #93b4ff;
  text-decoration: none;
  padding: 7px 12px;
  border-radius: 8px;
  font-size: 13px;
  background: rgba(255, 255, 255, 0.06);
  transition: background-color .18s ease, color .18s ease;
}

.admin-sidebar .sidebar-actions a:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.admin-sidebar button {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  border: none;
  color: #fff;
  padding: 9px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 13px;
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.28);
  transition: transform .15s ease, box-shadow .15s ease, filter .15s ease;
}

.admin-sidebar button:hover {
  filter: brightness(1.05);
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(239, 68, 68, 0.34);
}

/* ---------- 主内容区 ---------- */
.admin-main {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--admin-bg);
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 32px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 10;
}

.admin-header .header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-header .header-left span {
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  background: var(--accent-soft);
  padding: 4px 10px;
  border-radius: 999px;
}

.admin-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.2px;
}

.admin-header .user-badge {
  color: var(--text-main);
  font-weight: 600;
  background: #fff;
  border: 1px solid var(--border);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.05);
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 13px;
}

.admin-content {
  padding: 28px 32px 40px;
  flex: 1 1 auto;
}

/* 内容区嵌套 router-view 的呼吸感 */
.admin-content > * {
  max-width: 1240px;
  margin: 0 auto;
  width: 100%;
}

/* ---------- 响应式 ---------- */
@media (max-width: 900px) {
  .admin-sidebar { width: 80px; padding: 16px 12px; }
  .admin-sidebar .brand { justify-content: center; padding-bottom: 14px; }
  .admin-sidebar .brand-text { display: none; }
  .admin-nav a {
    padding: 12px 8px;
    font-size: 12px;
    text-align: center;
    line-height: 1.2;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .admin-nav a::before { display: none; }
  .admin-sidebar .sidebar-actions { flex-direction: column; gap: 6px; }
  .admin-header { padding: 14px 18px; }
  .admin-header h1 { font-size: 16px; }
  .admin-header .header-left span { display: none; }
  .admin-content { padding: 18px 16px 28px; }
}
</style>