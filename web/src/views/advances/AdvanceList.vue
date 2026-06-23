<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { deleteAdvance, listAdvances, saveAdvance, type AdvancePayment } from '../../api/advances'
import PaginationBar from '../../components/PaginationBar.vue'
import { useAuthStore } from '../../stores/auth'
import { formatDateTime } from '../../utils/date'

const auth = useAuthStore()
const loading = ref(false)
const keyword = ref('')
const range = ref<[string, string]>()
const rows = ref<AdvancePayment[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = computed(() => form.id ? '编辑预支记录' : '新增预支记录')
const methodOptions = ['微信', '支付宝', '现金', '其他']
const form = reactive<AdvancePayment>({
  name: '',
  idCardNo: '',
  phone: '',
  amount: 0,
  advanceTime: '',
  advanceMethod: '微信',
  reason: '',
  remark: ''
})

async function load() {
  loading.value = true
  try {
    const data: any = await listAdvances({
      keyword: keyword.value,
      startDate: range.value?.[0],
      endDate: range.value?.[1],
      page: page.value,
      size: size.value
    })
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

function defaultAdvanceTime() {
  const now = new Date()
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}:00`
}

function openCreate() {
  Object.assign(form, {
    id: undefined,
    personId: undefined,
    name: '',
    idCardNo: '',
    phone: '',
    amount: 0,
    advanceTime: defaultAdvanceTime(),
    advanceMethod: '微信',
    reason: '',
    remark: ''
  })
  dialogVisible.value = true
}

function normalizeTime(value?: string) {
  return value ? value.replace(' ', 'T').slice(0, 19) : defaultAdvanceTime()
}

function openEdit(row: AdvancePayment) {
  Object.assign(form, {
    id: row.id,
    personId: row.personId,
    name: row.name || '',
    idCardNo: row.idCardNo || '',
    phone: row.phone || '',
    amount: Number(row.amount || 0),
    advanceTime: normalizeTime(row.advanceTime),
    advanceMethod: row.advanceMethod || '微信',
    reason: row.reason || '',
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

async function submit() {
  const required: Array<[keyof AdvancePayment, string]> = [
    ['name', '预支人员'],
    ['idCardNo', '身份证号'],
    ['amount', '预支金额'],
    ['advanceTime', '预支时间'],
    ['advanceMethod', '预支方式'],
    ['reason', '预支原因']
  ]
  const missing = required.find(([key]) => {
    const value = form[key]
    return value === undefined || value === null || !String(value).trim() || (key === 'amount' && Number(value) <= 0)
  })
  if (missing) {
    ElMessage.warning(`请填写${missing[1]}`)
    return
  }
  await saveAdvance({ ...form, amount: Number(form.amount) })
  ElMessage.success(form.id ? '预支记录已修改' : '预支记录已保存')
  dialogVisible.value = false
  load()
}

async function remove(row: AdvancePayment) {
  await ElMessageBox.confirm(`确认删除 ${row.name} 的预支记录？`, '删除预支记录')
  await deleteAdvance(row.id!)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-fill">
    <div class="page-title">
      <h1>平时预支</h1>
      <el-button v-if="auth.hasPermission('advance:create')" type="primary" :icon="Plus" @click="openCreate">新增预支</el-button>
    </div>
    <div class="panel panel-fill">
      <div class="toolbar">
        <el-input v-model="keyword" clearable placeholder="姓名 / 身份证号 / 手机号 / 原因" style="width: 320px" @keyup.enter="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-date-picker
          v-model="range"
          type="daterange"
          range-separator="至"
          start-placeholder="预支开始日期"
          end-placeholder="预支结束日期"
          value-format="YYYY-MM-DD"
          style="width: 280px"
          @change="search"
        />
        <el-button @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border class="table-fill" height="100%">
        <el-table-column label="序号" width="70" fixed align="center">
          <template #default="{ $index }">{{ (page - 1) * size + $index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="name" label="预支人员" width="110" fixed />
        <el-table-column prop="amount" label="预支金额" width="120" align="right" />
        <el-table-column label="预支时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.advanceTime) }}</template>
        </el-table-column>
        <el-table-column prop="advanceMethod" label="方式" width="90" />
        <el-table-column prop="reason" label="预支原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="idCardNo" label="身份证号" width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="creatorName" label="创建人" width="110" />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="128" fixed="right">
          <template #default="{ row }">
            <el-button v-if="auth.hasPermission('advance:update')" :icon="Edit" link type="primary" @click="openEdit(row)" />
            <el-button v-if="auth.hasPermission('advance:delete')" :icon="Delete" link type="danger" @click="remove(row)" />
          </template>
        </el-table-column>
      </el-table>
      <PaginationBar v-model:page="page" v-model:size="size" :total="total" @change="load" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px">
      <el-form :model="form" label-width="96px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="预支人员" required><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="身份证号" required><el-input v-model="form.idCardNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="预支金额" required><el-input-number v-model="form.amount" :min="0" :precision="2" :step="100" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="预支时间" required>
              <el-date-picker v-model="form.advanceTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预支方式" required>
              <el-select v-model="form.advanceMethod" style="width: 100%">
                <el-option v-for="item in methodOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24"><el-form-item label="预支原因" required><el-input v-model="form.reason" maxlength="120" show-word-limit /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="200" show-word-limit /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
