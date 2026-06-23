import { createRouter, createWebHistory } from 'vue-router'

function hasPermission(permission?: string) {
  if (!permission) return true
  const user = JSON.parse(localStorage.getItem('payroll_user') || 'null')
  return user?.roleCodes?.includes('ADMIN') || user?.permissions?.includes(permission) || false
}

function firstAllowedPath() {
  const candidates = [
    { path: '/dashboard', permission: 'dashboard:view' },
    { path: '/batches', permission: 'batch:view' },
    { path: '/advances', permission: 'advance:view' },
    { path: '/persons', permission: 'person:view' },
    { path: '/units', permission: 'unit:view' },
    { path: '/exports', permission: 'export:view' },
    { path: '/logs', permission: 'log:view' },
    { path: '/salary-query', permission: 'salary:view' },
    { path: '/users', permission: 'user:view' }
  ]
  return candidates.find((item) => hasPermission(item.permission))?.path || '/login'
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', component: () => import('../views/login/LoginView.vue') },
    { path: '/salary-query', component: () => import('../views/query/SalaryQuery.vue'), meta: { public: true } },
    { path: '/', redirect: () => firstAllowedPath() },
    { path: '/dashboard', component: () => import('../views/dashboard/DashboardView.vue'), meta: { permission: 'dashboard:view' } },
    { path: '/persons', component: () => import('../views/persons/PersonList.vue'), meta: { permission: 'person:view' } },
    { path: '/units', component: () => import('../views/units/UnitList.vue'), meta: { permission: 'unit:view' } },
    { path: '/batches', component: () => import('../views/batches/BatchList.vue'), meta: { permission: 'batch:view' } },
    { path: '/batches/new', component: () => import('../views/batches/BatchEditor.vue'), meta: { permission: 'batch:create' } },
    { path: '/batches/:id', component: () => import('../views/batches/BatchEditor.vue'), meta: { permission: 'batch:update' } },
    { path: '/advances', component: () => import('../views/advances/AdvanceList.vue'), meta: { permission: 'advance:view' } },
    { path: '/exports', component: () => import('../views/exports/ExportList.vue'), meta: { permission: 'export:view' } },
    { path: '/logs', component: () => import('../views/logs/OperationLogList.vue'), meta: { permission: 'log:view' } },
    { path: '/users', component: () => import('../views/users/UserList.vue'), meta: { permission: 'user:view' } }
  ]
})

router.beforeEach((to) => {
  if (!to.meta.public && to.path !== '/login' && !localStorage.getItem('payroll_token')) {
    return '/login'
  }
  if (!to.meta.public && to.path !== '/login' && !hasPermission(to.meta.permission as string | undefined)) {
    return firstAllowedPath()
  }
})

export default router
