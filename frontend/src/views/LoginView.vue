<template>
    <div class="login-page">
        <div class="login-card">
            <h2>用户登录</h2>
            <form>
                <div class="form-group">
                    <label>用户名</label>
                    <input type="text" placeholder="请输入用户名"/>
                </div>
                <div class="form-group">
                    <label>密码</label>
                    <input type="password" placeholder="请输入密码"/>
                </div>
                <button class="login-btn">
                    登录
                </button>
                <p class="register-link">
                    没有账号？<router-link to="/register">立即注册</router-link>
                </p>
            </form>
        </div>
    </div>
</template>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 100px);
}
.login-card {
  background: #fff;
  border-radius: 8px;
  padding: 40px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  width: 100%;
  max-width: 400px;
}
.login-card h2 { text-align: center; margin-bottom: 30px; }
.form-group { margin-bottom: 20px; }
.form-group label { display: block; margin-bottom: 8px; font-weight: bold; }
.form-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}
.login-btn {
  width: 100%;
  padding: 12px;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}
.login-btn:hover { background: #66b1ff; }
.login-btn:disabled { background: #a0cfff; cursor: not-allowed; }
.register-link { text-align: center; margin-top: 20px; color: #666; }
.register-link a { color: #409eff; }
.error-msg { color: #f56c6c; text-align: center; margin-top: 10px; }
</style>