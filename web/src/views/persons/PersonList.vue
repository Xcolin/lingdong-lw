<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { deletePerson, listPersons, savePerson, updatePersonEnabled, type PayeePerson } from '../../api/persons'
import PaginationBar from '../../components/PaginationBar.vue'
import { useAuthStore } from '../../stores/auth'

const loading = ref(false)
const auth = useAuthStore()
const keyword = ref('')
const enabledFilter = ref<boolean | undefined>()
const rows = ref<PayeePerson[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = computed(() => form.id ? '编辑人员信息' : '新增人员信息')
const bankTypeOptions = ['中国建设银行', '中国银行', '其他银行']
const form = reactive<PayeePerson>({
  name: '',
  idCardNo: '',
  phone: '',
  bankAccount: '',
  accountName: '',
  bankName: '',
  bankType: '',
  bankCategory: '',
  cnapsNo: ''
})

async function load() {
  loading.value = true
  try {
    const data: any = await listPersons({ keyword: keyword.value, enabled: enabledFilter.value, page: page.value, size: size.value })
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
  Object.assign(form, { id: undefined, name: '', idCardNo: '', phone: '', bankAccount: '', accountName: '', bankName: '', bankType: '', bankCategory: '', cnapsNo: '' })
  dialogVisible.value = true
}

function openEdit(row: PayeePerson) {
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    idCardNo: row.idCardNo || '',
    phone: row.phone || '',
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
  const requiredFields: Array<[keyof PayeePerson, string]> = [
    ['name', '姓名'],
    ['idCardNo', '身份证号'],
    ['bankAccount', '银行账号'],
    ['accountName', '户名']
  ]
  const missing = requiredFields.find(([key]) => !String(form[key] || '').trim())
  if (missing) {
    ElMessage.warning(`请填写${missing[1]}`)
    return
  }
  await savePerson(form)
  ElMessage.success(form.id ? '人员信息已修改' : '人员信息已保存')
  dialogVisible.value = false
  load()
}

async function remove(row: PayeePerson) {
  await ElMessageBox.confirm(`确认删除 ${row.name}？`, '删除人员')
  await deletePerson(row.id!)
  ElMessage.success('已删除')
  load()
}

async function toggleEnabled(row: PayeePerson) {
  const nextEnabled = row.enabled === false
  const actionText = nextEnabled ? '启用' : '停用'
  await ElMessageBox.confirm(`确认${actionText} ${row.name}？`, `${actionText}人员`)
  await updatePersonEnabled(row.id!, nextEnabled)
  ElMessage.success(`已${actionText}`)
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-fill">
    <div class="page-title">
      <h1>人员信息库</h1>
      <el-button v-if="auth.hasPermission('person:create')" type="primary" :icon="Plus" @click="openCreate">新增人员</el-button>
    </div>
    <div class="panel panel-fill">
      <div class="toolbar">
        <el-input v-model="keyword" clearable placeholder="姓名 / 身份证号 / 手机号 / 银行账号" style="width: 360px" @keyup.enter="search">
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
        <el-table-column prop="name" label="姓名" width="110" fixed />
        <el-table-column prop="idCardNo" label="身份证号" width="190" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="bankAccount" label="银行账号" width="190" />
        <el-table-column prop="accountName" label="户名" width="140" />
        <el-table-column prop="bankName" label="行名" min-width="220" />
        <el-table-column prop="bankType" label="银行类型" width="120" />
        <el-table-column prop="cnapsNo" label="联行行号" width="150" />
        <el-table-column label="状态" width="86" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled === false ? 'info' : 'success'" size="small">
              {{ row.enabled === false ? '停用' : '启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="176" fixed="right">
          <template #default="{ row }">
            <el-button v-if="auth.hasPermission('person:update')" :icon="Edit" link type="primary" @click="openEdit(row)" />
            <el-button v-if="auth.hasPermission('person:enable')" link :type="row.enabled === false ? 'success' : 'warning'" @click="toggleEnabled(row)">
              {{ row.enabled === false ? '启用' : '停用' }}
            </el-button>
            <el-button v-if="auth.hasPermission('person:delete')" :icon="Delete" link type="danger" @click="remove(row)" />
          </template>
        </el-table-column>
      </el-table>
      <PaginationBar v-model:page="page" v-model:size="size" :total="total" @change="load" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
      <el-form :model="form" label-width="92px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="身份证号" required><el-input v-model="form.idCardNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="银行账号" required><el-input v-model="form.bankAccount" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="户名" required><el-input v-model="form.accountName" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="银行类型">
              <el-select v-model="form.bankType" clearable style="width: 100%">
                <el-option v-for="option in bankTypeOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24"><el-form-item label="行名"><el-input v-model="form.bankName" /></el-form-item></el-col>
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
