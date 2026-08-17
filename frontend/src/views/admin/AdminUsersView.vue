<template>
  <!--
    用户管理页 —— 展示用户列表，支持修改角色/状态、删除用户。
    使用原生 <table> + <select> 实现，未引入 Element Plus 表格组件（教学简化）。
  -->
  <div class="page-shell">
    <div class="page-header">
      <div>
        <span class="eyebrow">Users</span>
        <h1>用户管理</h1>
        <p class="page-subtitle">查看所有用户，调整角色与启用状态。</p>
      </div>
      <button class="btn btn-secondary" @click="loadUsers">刷新</button>
    </div>

    <!-- 三态渲染：错误 → 加载中 → 数据表格 -->
    <p v-if="error" class="state error">{{ error }}</p>
    <p v-else-if="loading" class="state">加载中...</p>

    <div v-else class="table-card">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>昵称</th>
            <th>邮箱</th>
            <th>手机</th>
            <th>角色</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <!-- v-for 遍历用户列表，:key 用 id 保证 DOM 高效更新 -->
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td>{{ user.nickname }}</td>
            <td>{{ user.email }}</td>
            <td>{{ user.phone }}</td>
            <td>
              <!--
                角色下拉框：v-model 双向绑定
                @change 触发 updateUser() 立即提交修改
              -->
              <select v-model="user.role" @change="updateUser(user)">
                <option value="USER">普通用户</option>
                <option value="ADMIN">管理员</option>
                <!-- <option value="MERCHANT">商户</option> --><!-- 商户角色暂未实现，后续补充时取消注释 -->
              </select>
            </td>
            <td>
              <!-- 状态下拉框：1=启用, 0=禁用 -->
              <select v-model.number="user.status" @change="updateUser(user)">
                <option :value="1">启用</option>
                <option :value="0">禁用</option>
              </select>
            </td>
            <td>
              <button @click="deleteUser(user.id)" class="btn btn-danger btn-sm">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
/**
 * AdminUsersView 脚本 —— 使用 Options API 的 setup() 函数
 *
 * 【数据加载模式】
 *   onMounted 钩子中调用 loadUsers() 发起 GET /api/users 请求
 *   响应数据赋值给 users ref，模板自动重新渲染
 */
import { onMounted, ref } from 'vue'
import http from '../../utils/http'
import { alertMessage } from '../../utils/modal'

export default {
  name: 'AdminUsersView',
  setup() {
    // 响应式状态：ref 创建响应式引用，值存储在 .value 中
    const loading = ref(false)
    const error = ref('')
    const users = ref([])

    /**
     * 加载用户列表
     * GET /api/users → 后端 UserController.getAll() → 返回 List<UserDTO>
     */
    const loadUsers = async () => {
        loading.value = true
        error.value = ''
        try {
            const response = await http.get("/users")
            users.value = response.data || []
        } catch (err) {
            error.value = err.message || '加载用户失败'
        } finally {
            loading.value = false
        }
    }  
    /**
     * 更新用户（修改角色或状态时立即调用）
     * PUT /api/users → 后端 UserController.update()
     */
    const updateUser  = async (user) => {
        try {
            await http.put('/users', user)
            await alertMessage('用户信息已更新')
        } catch (err) {
            await alertMessage(err.message || '用户信息更新失败')
            // 如果更新失败，重新加载，恢复界面数据一致性
            await loadUsers()
        }
    }
    /**
     * 删除用户
     * DELETE /api/users/{id}
     * 删除前用 confirm 弹窗二次确认，防止误操作
     */
    const deleteUser = async (id) => {
        if (!confirm('确认删除此用户？此操作不可以撤销')) return
        try {
            await http.delete(`/users/${id}`)
            await loadUsers()
        } catch (err) {
            await alertMessage(err.message || '删除失败')
        }
    }
    // 生命周期钩子：组件挂载后自动加载用户列表
    onMounted(loadUsers)

    return { loading, error, users, loadUsers, updateUser, deleteUser }
  }
}
</script>

<style scoped>
/* ============================================================
 * 管理后台通用样式 - 页面头部 / 按钮 / 状态 / 表格卡片
 * ============================================================ */

/* ---------- 页面头部 ---------- */
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

/* ---------- 按钮 ---------- */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 18px;
  border: none;
  border-radius: 10px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  text-decoration: none;
  transition: transform .15s ease, box-shadow .15s ease, background-color .15s ease, color .15s ease;
}

.btn-primary {
  background: linear-gradient(135deg, #4f7cff 0%, #3558d3 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(79, 124, 255, 0.30);
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(79, 124, 255, 0.38);
}

.btn-secondary {
  background: #eef1f6;
  color: #334155;
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.btn-secondary:hover { background: #e2e8f0; }

.btn-danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  box-shadow: 0 3px 10px rgba(239, 68, 68, 0.25);
}

.btn-danger:hover {
  filter: brightness(1.05);
  transform: translateY(-1px);
}

.btn-sm { padding: 6px 12px; font-size: 13px; border-radius: 8px; }
.btn.compact { padding: 8px 14px; font-size: 13px; }

/* ---------- 状态文本 ---------- */
.state { padding: 48px 20px; text-align: center; color: #64748b; font-size: 14px; }
.state.error { color: #ef4444; font-weight: 600; }

/* ---------- 表格卡片 ---------- */
.table-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.05);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.data-table { width: 100%; border-collapse: collapse; }

.data-table th,
.data-table td {
  padding: 14px 18px;
  text-align: left;
  border-bottom: 1px solid #eef2f7;
}

.data-table th {
  background: #f8fafc;
  font-weight: 600;
  font-size: 12.5px;
  letter-spacing: 0.3px;
  text-transform: uppercase;
  color: #64748b;
}

.data-table tbody tr { transition: background-color .12s ease; }
.data-table tbody tr:hover { background: #f5f8ff; }
.data-table tbody tr:last-child td { border-bottom: none; }

.data-table select {
  padding: 6px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  font-size: 13px;
  outline: none;
  transition: border-color .15s ease, box-shadow .15s ease;
}

.data-table select:focus {
  border-color: #4f7cff;
  box-shadow: 0 0 0 3px rgba(79, 124, 255, 0.15);
}

/* ---------- 状态标签 ---------- */
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-tag::before {
  content: "";
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}
</style>
