import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('../views/LoginView.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/',
      component: () => import('../layouts/WorkspaceLayout.vue'),
      children: [
        {
          path: '',
          component: () => import('../views/DashboardView.vue'),
          meta: { title: '业务总览' },
        },
        {
          path: 'customers',
          component: () => import('../views/CustomersView.vue'),
          meta: { title: '客户管理' },
        },
        {
          path: 'accounts',
          component: () => import('../views/AccountsView.vue'),
          meta: { title: '账户中心' },
        },
        {
          path: 'transfers',
          component: () => import('../views/TransfersView.vue'),
          meta: { title: '转账结算' },
        },
        {
          path: 'loans',
          component: () => import('../views/LoansView.vue'),
          meta: { title: '贷款工作台' },
        },
        {
          path: 'products',
          component: () => import('../views/ProductsView.vue'),
          meta: { title: '产品与申购' },
        },
        {
          path: 'notifications',
          component: () => import('../views/NotificationsView.vue'),
          meta: { title: '消息中心' },
        },
        {
          path: 'audits',
          component: () => import('../views/AuditsView.vue'),
          meta: { title: '操作审计', permission: 'audit:read' },
        },
        {
          path: 'learning',
          component: () => import('../views/LearningView.vue'),
          meta: { title: '学习地图' },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.initialize()
  if (!to.meta.public && !auth.loggedIn) return { path: '/login' }
  if (to.path === '/login' && auth.loggedIn) return '/'
  if (to.meta.permission && !auth.can(String(to.meta.permission))) return '/'
  document.title = `${String(to.meta.title ?? '银行业务学堂')} · Banking Academy`
})
