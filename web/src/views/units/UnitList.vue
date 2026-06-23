<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { deleteUnit, listUnits, saveUnit, updateUnitEnabled, type PayingUnit } from '../../api/units'
import PaginationBar from '../../components/PaginationBar.vue'
import { useAuthStore } from '../../stores/auth'

const loading = ref(false)
const auth = useAuthStore()
const keyword = ref('')
const enabledFilter = ref<boolean | undefined>()
const rows = ref<PayingUnit[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const form = reactive<PayingUnit>({ bankAccount: '', accountName: '', bankName: '', bankType: '', bankCategory: '', cnapsNo: '' })
const dialogTitle = computed(() => form.id ? '编辑单位信息' : '新增单位信息')
const bankTypeOptions = ['中国建设银行', '中国银行', '其他银行']

async function load() {
  loading.value = true
  try {
    const data: any = await listUnits({ keyword: keyword.value, enabled: enabledFilter.value, page: page.value, size: size.value })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function openCreate() {
  Object.assign(form, { id: undefined, bankAccount: '', accountName: '', bankName: '', bankType: '', bankCategory: '', cnapsNo: '' })
  dialogVisible.value = true
}

function openEdit(row: PayingUnit) {
  Object.assign(form, {
    id: row.id,
    bankAccount: row.bankAccount || '',
    accountName: row.accountName || '',
    bankName: row.bankName || '',
    bankType: row.bankType || '',
    bankCategory: row.bankCategory || '',
    cnapsNo: row.cnapsNo || ''
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.bankAccount.trim()) {
    ElMessage.warning('请填写银行账号')
    return
  }
  if (!form.accountName.trim()) {
    ElMessage.warning('请填写户名')
    return
  }
  await saveUnit(form)
  ElMessage.success(form.id ? '单位信息已修改' : '单位信息已保存')
  dialogVisible.value = false
  load()
}

async function remove(row: PayingUnit) {
  await ElMessageBox.confirm(`确认删除 ${row.accountName}？`, '删除计发单位')
  await deleteUnit(row.id!)
  ElMessage.success('已删除')
  load()
}

async function toggleEnabled(row: PayingUnit) {
  const nextEnabled = row.enabled === false
  const actionText = nextEnabled ? '启用' : '停用'
  await ElMessageBox.confirm(`确认${actionText} ${row.accountName}？`, `${actionText}单位`)
  await updateUnitEnabled(row.id!, nextEnabled)
  ElMessage.success(`已${actionText}`)
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-fill">
    <div class="page-title">
      <h1>单位信息库</h1>
      <el-button v-if="auth.hasPermission('unit:create')" type="primary" :icon="Plus" @click="openCreate">新增单位</el-button>
    </div>
    <div class="panel panel-fill">
      <div class="toolbar">
        <el-input v-model="keyword" clearable placeholder="户名 / 银行账号 / 行名" style="width: 360px" @keyup.enter="search">
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
        <el-table-column prop="accountName" label="户名" width="220" fixed />
        <el-table-column prop="bankAccount" label="银行账号" width="200" />
        <el-table-column prop="bankName" label="行名" min-width="240" />
        <el-table-column prop="bankType" label="银行类型" width="130" />
        <el-table-column prop="cnapsNo" label="联行行号" width="160" />
        <el-table-column label="状态" width="86" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled === false ? 'info' : 'success'" size="small">
              {{ row.enabled === false ? '停用' : '启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="176" fixed="right">
          <template #default="{ row }">
            <el-button v-if="auth.hasPermission('unit:update')" :icon="Edit" link type="primary" @click="openEdit(row)" />
            <el-button v-if="auth.hasPermission('unit:enable')" link :type="row.enabled === false ? 'success' : 'warning'" @click="toggleEnabled(row)">
              {{ row.enabled === false ? '启用' : '停用' }}
            </el-button>
            <el-button v-if="auth.hasPermission('unit:delete')" :icon="Delete" link type="danger" @click="remove(row)" />
          </template>
        </el-table-column>
      </el-table>
      <PaginationBar v-model:page="page" v-model:size="size" :total="total" @change="load" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px">
      <el-form :model="form" label-width="92px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="银行账号" required><el-input v-model="form.bankAccount" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="户名" required><el-input v-model="form.accountName" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="行名"><el-input v-model="form.bankName" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="银行类型">
              <el-select v-model="form.bankType" clearable style="width: 100%">
                <el-option v-for="option in bankTypeOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="联行行号"><el-input v-model="form.cnapsNo" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
