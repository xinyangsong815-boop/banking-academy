<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCommand } from '../composables/useCommand'
const auth = useAuthStore()
const router = useRouter()
const form = reactive({ username: 'admin', password: 'Learn@123456' })
const { pending, run } = useCommand()
async function submit() {
  await run(async () => {
    await auth.login(form.username, form.password)
    await router.replace('/')
  })
}
</script>
<template>
  <main class="login-page">
    <section class="login-story">
      <div class="brand light">
        <span class="brand-symbol">B<span>·</span></span>
        <div>Banking<span>ACADEMY / 银行业务学堂</span></div>
      </div>
      <div>
        <p class="eyebrow">LEARN THROUGH BUILDING</p>
        <h1>理解业务。<br />构建可靠的系统。</h1>
        <p>从一次账户查询，到一笔完整的转账。<br />把架构、模式与工程实践，放进可以运行的项目。</p>
        <div class="login-topics">
          <span>模块化架构</span><span>事务与并发</span><span>前后端协作</span>
        </div>
      </div>
      <small>面向学习者的银行业务模拟平台 · Version 1.0</small>
    </section>
    <section class="login-form-area">
      <div class="login-form">
        <p class="eyebrow">WELCOME BACK</p>
        <h2>进入学习工作空间</h2>
        <p class="muted">选择一个角色，体验不同权限下的业务流程。</p>
        <form @submit.prevent="submit">
          <label for="username">登录账号</label
          ><input
            id="username"
            v-model="form.username"
            required
            maxlength="40"
            autocomplete="username"
          /><label for="password">登录密码</label
          ><input
            id="password"
            v-model="form.password"
            type="password"
            required
            maxlength="100"
            autocomplete="current-password"
          /><button
            class="button full"
            :disabled="pending"
          >
            {{ pending ? '正在验证…' : '登录工作空间 →' }}
          </button>
        </form>
        <div class="demo-accounts">
          <strong>演示账户</strong>
          <div class="button-row">
            <button
              v-for="name in ['admin', 'teller', 'auditor']"
              :key="name"
              class="button secondary small"
              @click="form.username = name"
            >
              {{ name }}
            </button>
          </div>
          <p>统一密码 <code>Learn@123456</code><br />管理员 / 柜员 / 只读审计员</p>
        </div>
      </div>
    </section>
  </main>
</template>
