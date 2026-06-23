<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ username: 'admin', password: '' })

async function submit() {
  if (!form.username.trim()) {
    ElMessage.warning('请输入账号')
    return
  }
  if (!form.password.trim()) {
    ElMessage.warning('请输入密码')
    return
  }
  await auth.login(form.username, form.password)
  ElMessage.success('登录成功')
  router.push('/')
}
</script>

<template>
  <div class="login-page">
    <div class="login-box">
      <h1>劳务工资代发模板系统</h1>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="账号" required>
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="form.password" type="password" show-password @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" @click="submit">登录</el-button>
      </el-form>
    </div>
  </div>
</template>
