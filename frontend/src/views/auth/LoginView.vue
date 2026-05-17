<template>
  <div class="login-page">
    <section class="login-card">
      <p class="login-card__tag">Frontend Shell</p>
      <h1>Web 高级编程管理平台</h1>
      <p>当前为前端空项目壳，后续可在此接入真实登录表单、权限逻辑和角色跳转。</p>
      <div class="login-card__actions">
        <RouterLink to="/teacher/course">进入教师端</RouterLink>
        <RouterLink to="/student/course">进入学生端</RouterLink>
      </div>
    </section>
  </div>
</template>

<script setup>
import { RouterLink } from 'vue-router'
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: radial-gradient(circle at top, #dce8ff, #eef3fb 45%, #f7f9fc 100%);
}

.login-card {
  width: min(100%, 720px);
  background: #ffffff;
  border-radius: 24px;
  padding: 36px;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.08);
}

.login-card__tag {
  margin: 0;
  color: #1d4ed8;
  font-weight: 600;
}

.login-card h1 {
  margin: 12px 0;
}

.login-card p {
  color: #51627d;
}

.login-card__actions {
  margin-top: 24px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.login-card__actions a {
  padding: 12px 18px;
  border-radius: 12px;
  background: #1d4ed8;
  color: #ffffff;
}
</style>
