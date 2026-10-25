<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCommand } from '../composables/useCommand'
import { roleName } from '../utils/format'
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)
const { run, pending } = useCommand()
const navigation = [
  { path: '/', label: '业务总览', icon: '◫', group: '工作空间' },
  { path: '/customers', label: '客户管理', icon: '♧' },
  { path: '/accounts', label: '账户中心', icon: '▣' },
  { path: '/transfers', label: '转账结算', icon: '⇄' },
  { path: '/loans', label: '贷款工作台', icon: '◇' },
  { path: '/products', label: '产品与申购', icon: '▤' },
  { path: '/notifications', label: '消息中心', icon: '◷', group: '管理与学习' },
  { path: '/audits', label: '操作审计', icon: '≡', permission: 'audit:read' },
  { path: '/learning', label: '学习地图', icon: '↗' },
]
async function logout() {
  await run(async () => {
    await auth.logout()
    await router.replace('/login')
  })
}
</script>
<template>
  <div class="workspace">
    <aside
      class="sidebar"
      :class="{ open: menuOpen }"
    >
      <RouterLink
        to="/"
        class="brand"
        ><span class="brand-symbol">B<span>·</span></span>
        <div>Banking<span>ACADEMY / 银行业务学堂</span></div></RouterLink
      >
      <div class="workspace-label">
        <span class="live-dot"></span> 教学演示工作空间 <span class="version">V1.0</span>
      </div>
      <nav aria-label="主导航">
        <template
          v-for="item in navigation"
          :key="item.path"
        >
          <p
            v-if="item.group"
            class="nav-group"
          >
            {{ item.group }}
          </p>
          <RouterLink
            v-if="!item.permission || auth.can(item.permission)"
            :to="item.path"
            class="nav-item"
            :class="{ selected: route.path === item.path }"
            @click="menuOpen = false"
            ><span aria-hidden="true">{{ item.icon }}</span
            >{{ item.label
            }}<span
              v-if="route.path === item.path"
              class="nav-arrow"
              >›</span
            ></RouterLink
          >
        </template>
      </nav>
      <div class="sidebar-note">
        <span class="note-icon">✦</span><strong>从业务理解代码</strong>
        <p>完成一次转账，追踪一条请求，理解一个设计决策。</p>
        <RouterLink to="/learning">打开学习地图 ↗</RouterLink>
      </div>
      <div class="sidebar-footer">教学模拟数据 · 不接入真实银行</div>
    </aside>
    <div
      v-if="menuOpen"
      class="menu-overlay"
      @click="menuOpen = false"
    ></div>
    <div class="main-area">
      <header class="topbar">
        <div class="breadcrumb">
          <button
            class="icon-button mobile-menu"
            aria-label="切换菜单"
            @click="menuOpen = !menuOpen"
          >
            ☰</button
          ><span>工作空间</span><span>/</span><strong>{{ route.meta.title }}</strong>
        </div>
        <div class="topbar-right">
          <span class="demo-tag">DEMO 环境</span
          ><span class="avatar">{{ auth.user?.displayName.slice(0, 1) }}</span>
          <div class="user-meta">
            <strong>{{ auth.user?.displayName }}</strong
            ><span>{{ auth.user ? roleName[auth.user.role] : '' }}</span>
          </div>
          <button
            class="text-button"
            :disabled="pending"
            @click="logout"
          >
            退出
          </button>
        </div>
      </header>
      <main class="page-content"><RouterView /></main>
      <footer class="main-footer">
        <span>Banking Academy · 让每一行业务代码都有来处</span
        ><span>Java / Spring Boot / Vue / TypeScript</span>
      </footer>
    </div>
  </div>
</template>
