<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Key, Plus, Search, Setting } from '@element-plus/icons-vue'
import {
  listPermissions,
  listUsers,
  resetUserPassword,
  saveUser,
  updateUserEnabled,
  updateUserPermissions,
  type PermissionSummary,
  type UserSummary
} from '../../api/users'
import PaginationBar from '../../components/PaginationBar.vue'
import { formatDateTime } from '../../utils/date'

const loading = ref(false)
const keyword = ref('')
const enabledFilter = ref<boolean | undefined>()
const rows = ref<UserSummary[]>([])
const permissions = ref<PermissionSummary[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const passwordVisible = ref(false)
const permissionVisible = ref(false)
const currentUser = ref<UserSummary>()
const form = reactive({
  id: undefined as number | undefined,
  username: '',
  displayName: '',
  password: '',
  enabled: true,
  permissionCodes: [] as string[]
})
const passwordForm = reactive({ password: '' })
const permissionForm = reactive({ permissionCodes: [] as string[] })
const dialogTitle = computed(() => form.id ? '编辑用户' : '新增用户')
const formPermissionTreeRef = ref()
const permissionTreeRef = ref()

interface PermissionTreeNode {
  code: string
  name: string
  type: 'MENU' | 'BUTTON'
  children?: PermissionTreeNode[]
}

const permissionTreeData = computed(() => {
  const sorted = [...permissions.value].sort((a, b) => a.sortNo - b.sortNo)
  const menus = sorted.filter((item) => item.type === 'MENU')
  const buttons = sorted.filter((item) => item.type === 'BUTTON')
  const usedButtonCodes = new Set<string>()
  const nodes: PermissionTreeNode[] = menus.map((menu) => {
    const key = permissionModule(menu.code)
    const children = buttons.filter((button) => permissionModule(button.code) === key)
    children.forEach((button) => usedButtonCodes.add(button.code))
    return {
      code: menu.code,
      name: menu.name,
      type: menu.type,
      children: children.map((button) => ({
        code: button.code,
        name: button.name,
        type: button.type
      }))
    }
  })
  const orphanButtons = buttons.filter((button) => !usedButtonCodes.has(button.code))
  if (orphanButtons.length > 0) {
    nodes.push({
      code: 'other-permissions',
      name: '其他权限',
      type: 'MENU',
      children: orphanButtons.map((button) => ({
        code: button.code,
        name: button.name,
        type: button.type
      }))
    })
  }
  return nodes
})

function permissionModule(code: string) {
  return code.split(':')[0]
}

function treeTypeLabel(node: PermissionTreeNode) {
  return node.type === 'MENU' ? '菜单' : '按钮'
}

function treeType(node: PermissionTreeNode) {
  return node.type === 'MENU' ? 'success' : 'info'
}

function collectTreeCodes(treeRef: any) {
  if (!treeRef) {
    return []
  }
  const checked = treeRef.getCheckedKeys(false) as string[]
  const halfChecked = treeRef.getHalfCheckedKeys() as string[]
  return Array.from(new Set([...checked, ...halfChecked])).filter((code) => code !== 'other-permissions')
}

function syncFormPermissionCodes() {
  form.permissionCodes = collectTreeCodes(formPermissionTreeRef.value)
}

function syncPermissionCodes() {
  permissionForm.permissionCodes = collectTreeCodes(permissionTreeRef.value)
}

async function setTreeCheckedKeys(treeRef: any, codes: string[]) {
  await nextTick()
  treeRef.value?.setCheckedKeys(codes, false)
}

async function load() {
  loading.value = true
  try {
    const data: any = await listUsers({ keyword: keyword.value, enabled: enabledFilter.value, page: page.value, size: size.value })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadPermissions() {
  const data: any = await listPermissions()
  permissions.value = data || []
}

function search() {
  page.value = 1
  load()
}

function openCreate() {
  Object.assign(form, { id: undefined, username: '', displayName: '', password: '', enabled: true, permissionCodes: [] })
  dialogVisible.value = true
  setTreeCheckedKeys(formPermissionTreeRef, [])
}

function openEdit(row: UserSummary) {
  Object.assign(form, {
    id: row.id,
    username: row.username,
    displayName: row.displayName,
    password: '',
    enabled: row.enabled !== false,
    permissionCodes: [...(row.permissions || [])]
  })
  dialogVisible.value = true
  setTreeCheckedKeys(formPermissionTreeRef, form.permissionCodes)
}

async function submit() {
  if (!form.username.trim()) {
    ElMessage.warning('请填写账号')
    return
  }
  if (!form.displayName.trim()) {
    ElMessage.warning('请填写姓名')
    return
  }
  if (!form.id && !form.password.trim()) {
    ElMessage.warning('请填写密码')
    return
  }
  if (form.password && form.password.length < 6) {
    ElMessage.warning('密码至少6位')
    return
  }
  syncFormPermissionCodes()
  await saveUser({
    username: form.username,
    displayName: form.displayName,
    password: form.password || undefined,
    enabled: form.enabled,
    permissionCodes: form.permissionCodes
  }, form.id)
  ElMessage.success(form.id ? '用户已修改' : '用户已新增')
  dialogVisible.value = false
  load()
}

async function toggleEnabled(row: UserSummary) {
  const nextEnabled = row.enabled === false
  const actionText = nextEnabled ? '启用' : '停用'
  await ElMessageBox.confirm(`确认${actionText} ${row.displayName}？`, `${actionText}用户`)
  await updateUserEnabled(row.id!, nextEnabled)
  ElMessage.success(`已${actionText}`)
  load()
}

function openPassword(row: UserSummary) {
  currentUser.value = row
  passwordForm.password = ''
  passwordVisible.value = true
}

async function submitPassword() {
  if (!passwordForm.password || passwordForm.password.length < 6) {
    ElMessage.warning('密码至少6位')
    return
  }
  await resetUserPassword(currentUser.value!.id!, passwordForm.password)
  ElMessage.success('密码已重置')
  passwordVisible.value = false
}

function openPermission(row: UserSummary) {
  currentUser.value = row
  permissionForm.permissionCodes = [...(row.permissions || [])]
  permissionVisible.value = true
  setTreeCheckedKeys(permissionTreeRef, permissionForm.permissionCodes)
}

async function submitPermissions() {
  syncPermissionCodes()
  await updateUserPermissions(currentUser.value!.id!, permissionForm.permissionCodes)
  ElMessage.success('授权已保存')
  permissionVisible.value = false
  load()
}

onMounted(async () => {
  await loadPermissions()
  await load()
})
</script>

<template>
  <div class="page-fill">
    <div class="page-title">
      <h1>用户管理</h1>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
    </div>

    <div class="panel panel-fill">
      <div class="toolbar">
        <el-input v-model="keyword" clearable placeholder="账号 / 姓名" style="width: 300px" @keyup.enter="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="enabledFilter" clearable placeholder="状态" style="width: 120px" @change="search">
          <el-option label="启用" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
        <el-button @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border class="table-fill" height="100%">
        <el-table-column label="序号" width="70" fixed align="center">
          <template #default="{ $index }">{{ (page - 1) * size + $index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="username" label="账号" width="150" fixed />
        <el-table-column prop="displayName" label="姓名" width="150" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">{{ row.roleCodes?.includes('ADMIN') ? '管理员' : '录入人员' }}</template>
        </el-table-column>
        <el-table-column label="授权数量" width="110" align="center">
          <template #default="{ row }">{{ row.permissions?.length || 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="86" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled === false ? 'info' : 'success'" size="small">
              {{ row.enabled === false ? '停用' : '启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button :icon="Edit" link type="primary" @click="openEdit(row)" />
            <el-button :icon="Setting" link type="primary" @click="openPermission(row)">授权</el-button>
            <el-button :icon="Key" link type="primary" @click="openPassword(row)">密码</el-button>
            <el-button link :type="row.enabled === false ? 'success' : 'warning'" @click="toggleEnabled(row)">
              {{ row.enabled === false ? '启用' : '停用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <PaginationBar v-model:page="page" v-model:size="size" :total="total" @change="load" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px">
      <el-form :model="form" label-width="92px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="账号" required>
              <el-input v-model="form.username" :disabled="!!form.id" />
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="姓名" required><el-input v-model="form.displayName" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item :label="form.id ? '新密码' : '密码'" :required="!form.id">
              <el-input v-model="form.password" type="password" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="权限配置">
              <el-tree
                ref="formPermissionTreeRef"
                class="permission-tree"
                :data="permissionTreeData"
                node-key="code"
                show-checkbox
                default-expand-all
                :expand-on-click-node="false"
              >
                <template #default="{ data }">
                  <span class="permission-node">
                    <span>{{ data.name }}</span>
                    <el-tag size="small" :type="treeType(data)">{{ treeTypeLabel(data) }}</el-tag>
                  </span>
                </template>
              </el-tree>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordVisible" title="重置密码" width="420px">
      <el-form label-width="80px">
        <el-form-item label="用户">{{ currentUser?.displayName }}</el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="passwordForm.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPassword">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permissionVisible" title="用户授权" width="760px">
      <el-form label-width="92px">
        <el-form-item label="用户">{{ currentUser?.displayName }}</el-form-item>
        <el-form-item label="权限配置">
          <el-tree
            ref="permissionTreeRef"
            class="permission-tree"
            :data="permissionTreeData"
            node-key="code"
            show-checkbox
            default-expand-all
            :expand-on-click-node="false"
          >
            <template #default="{ data }">
              <span class="permission-node">
                <span>{{ data.name }}</span>
                <el-tag size="small" :type="treeType(data)">{{ treeTypeLabel(data) }}</el-tag>
              </span>
            </template>
          </el-tree>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="permissionVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPermissions">保存授权</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.permission-tree {
  width: 100%;
  border: 1px solid #dfe7ef;
  border-radius: 8px;
  padding: 8px 10px;
  background: #fbfdff;
  max-height: 420px;
  overflow: auto;
}

.permission-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  white-space: nowrap;
}

.permission-tree :deep(.el-tree-node__content) {
  height: 32px;
}

@media (max-width: 860px) {
  .permission-tree {
    max-height: 360px;
  }
}
</style>
