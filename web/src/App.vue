<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Coin, DataAnalysis, Document, Files, Notebook, OfficeBuilding, User, SwitchButton, Setting, Search, Wallet } from '@element-plus/icons-vue'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isStandalone = computed(() => route.path === '/login' || route.path === '/salary-query')

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <router-view v-if="isStandalone" />
  <el-container v-else class="app-shell">
    <el-aside width="232px" class="app-aside">
      <div class="brand">
        <div class="brand-mark">薪</div>
        <div>
          <strong>工资代发</strong>
          <span>模板生成系统</span>
        </div>
      </div>
      <el-menu :default-active="route.path" router class="side-menu">
        <el-menu-item v-if="auth.hasPermission('dashboard:view')" index="/dashboard"><el-icon><DataAnalysis /></el-icon><span>工作台</span></el-menu-item>
        <el-menu-item v-if="auth.hasPermission('batch:view')" index="/batches"><el-icon><Notebook /></el-icon><span>工资批次</span></el-menu-item>
        <el-menu-item v-if="auth.hasPermission('advance:view')" index="/advances"><el-icon><Wallet /></el-icon><span>平时预支</span></el-menu-item>
        <el-menu-item v-if="auth.hasPermission('person:view')" index="/persons"><el-icon><User /></el-icon><span>人员信息库</span></el-menu-item>
        <el-menu-item v-if="auth.hasPermission('unit:view')" index="/units"><el-icon><OfficeBuilding /></el-icon><span>单位信息库</span></el-menu-item>
        <el-menu-item v-if="auth.hasPermission('export:view')" index="/exports"><el-icon><Files /></el-icon><span>导出记录</span></el-menu-item>
        <el-menu-item v-if="auth.hasPermission('log:view')" index="/logs"><el-icon><Document /></el-icon><span>操作日志</span></el-menu-item>
        <el-menu-item v-if="auth.hasPermission('salary:view')" index="/salary-query"><el-icon><Search /></el-icon><span>工资查询</span></el-menu-item>
        <el-menu-item v-if="auth.hasPermission('user:view')" index="/users"><el-icon><Setting /></el-icon><span>用户管理</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <div class="header-title"><el-icon><Coin /></el-icon>劳务工资代发模板系统</div>
        <div class="header-user">
          <span>{{ auth.user?.displayName || '未登录' }}</span>
          <el-button :icon="SwitchButton" circle @click="logout" />
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
