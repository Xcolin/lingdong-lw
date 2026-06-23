<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Edit, Search } from '@element-plus/icons-vue'
import { listPersons, savePerson, type PayeePerson } from '../../api/persons'
import PaginationBar from '../../components/PaginationBar.vue'

const props = defineProps<{ modelValue: boolean; existingIdCards: string[] }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; selected: [rows: PayeePerson[]] }>()

const keyword = ref('')
const loading = ref(false)
const rows = ref<PayeePerson[]>([])
const leftTableRef = ref()
const leftChecked = ref<PayeePerson[]>([])
const selected = ref<PayeePerson[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const editVisible = ref(false)
const bankTypeOptions = ['中国建设银行', '中国银行', '其他银行']
const editForm = reactive<PayeePerson>({
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
const existingIdCardSet = computed(() => new Set(props.existingIdCards.filter(Boolean)))
const selectedKeys = computed(() => new Set(selected.value.map((row) => personKey(row))))
const availableRows = computed(() => rows.value.filter((row) => !existingIdCardSet.value.has(row.idCardNo) && !selectedKeys.value.has(personKey(row))))

watch(() => props.modelValue, (visible) => {
  if (visible) {
    page.value = 1
    selected.value = []
    clearLeftChecked()
    load()
  }
})

function personKey(row: PayeePerson) {
  return String(row.id || row.idCardNo || row.bankAccount || row.name)
}

async function load() {
  loading.value = true
  try {
    const data: any = await listPersons({ keyword: keyword.value, enabled: true, page: page.value, size: size.value })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  clearLeftChecked()
  load()
}

function clearLeftChecked() {
  leftChecked.value = []
  leftTableRef.value?.clearSelection?.()
}

function addChecked() {
  if (leftChecked.value.length === 0) {
    ElMessage.warning('请先选择要加入的人员')
    return
  }
  const nextRows = leftChecked.value.filter((row) => !selectedKeys.value.has(personKey(row)))
  selected.value = [...selected.value, ...nextRows]
  clearLeftChecked()
}

function removeSelected(row: PayeePerson) {
  selected.value = selected.value.filter((item) => personKey(item) !== personKey(row))
}

function openEdit(row: PayeePerson) {
  Object.assign(editForm, {
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
  editVisible.value = true
}

async function submitEdit() {
  const requiredFields: Array<[keyof PayeePerson, string]> = [
    ['name', '姓名'],
    ['idCardNo', '身份证号'],
    ['bankAccount', '银行账号'],
    ['accountName', '户名']
  ]
  const missing = requiredFields.find(([key]) => !String(editForm[key] || '').trim())
  if (missing) {
    ElMessage.warning(`请填写${missing[1]}`)
    return
  }
  await savePerson(editForm)
  const updated = { ...editForm }
  rows.value = rows.value.map((row) => personKey(row) === personKey(updated) ? { ...row, ...updated } : row)
  selected.value = selected.value.map((row) => personKey(row) === personKey(updated) ? { ...row, ...updated } : row)
  leftChecked.value = leftChecked.value.map((row) => personKey(row) === personKey(updated) ? { ...row, ...updated } : row)
  editVisible.value = false
  ElMessage.success('人员信息已修改')
  load()
}

function confirm() {
  if (selected.value.length === 0) {
    ElMessage.warning('请先加入人员')
    return
  }
  emit('selected', selected.value)
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="批量选择人员"
    width="min(1280px, calc(100vw - 32px))"
    class="picker-dialog"
    align-center
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="picker-dual">
      <div class="picker-pane">
        <div class="picker-pane-header">
          <div class="picker-pane-title">未加入人员</div>
        </div>
        <div class="toolbar picker-toolbar">
          <el-input v-model="keyword" placeholder="姓名 / 身份证号 / 手机号 / 银行账号" clearable @keyup.enter="search">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button @click="search">查询</el-button>
        </div>
        <el-table
          ref="leftTableRef"
          v-loading="loading"
          :data="availableRows"
          :row-key="personKey"
          border
          height="clamp(340px, calc(100vh - 360px), 500px)"
          @selection-change="leftChecked = $event"
        >
          <el-table-column type="selection" width="44" reserve-selection />
          <el-table-column label="序号" width="64" align="center">
            <template #default="{ $index }">{{ (page - 1) * size + $index + 1 }}</template>
          </el-table-column>
          <el-table-column prop="bankAccount" label="银行账号" width="180" />
          <el-table-column prop="accountName" label="户名" width="120" />
          <el-table-column prop="bankName" label="行名" min-width="190" />
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="idCardNo" label="身份证号" width="180" />
          <el-table-column label="操作" width="58" fixed="right" align="center">
            <template #default="{ row }">
              <el-button :icon="Edit" link type="primary" @click="openEdit(row)" />
            </template>
          </el-table-column>
        </el-table>
        <PaginationBar v-model:page="page" v-model:size="size" :total="total" compact class="picker-pagination" @change="load" />
      </div>

      <div class="picker-actions">
        <el-button type="primary" @click="addChecked">&gt;&gt;</el-button>
      </div>

      <div class="picker-pane picker-selected-pane">
        <div class="picker-pane-header">
          <div class="picker-pane-title">已加入人员</div>
          <span class="picker-count">{{ selected.length }} 人</span>
        </div>
        <el-table :data="selected" border height="clamp(340px, calc(100vh - 360px), 500px)" empty-text="请从左侧选择人员加入">
          <el-table-column label="序号" type="index" width="64" align="center" />
          <el-table-column prop="bankAccount" label="银行账号" width="180" show-overflow-tooltip />
          <el-table-column prop="accountName" label="户名" width="120" />
          <el-table-column prop="bankName" label="行名" min-width="190" show-overflow-tooltip />
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="idCardNo" label="身份证号" width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="92" fixed="right" align="center">
            <template #default="{ row }">
              <el-button :icon="Edit" link type="primary" @click="openEdit(row)" />
              <el-button :icon="Close" link type="danger" @click="removeSelected(row)" />
            </template>
          </el-table-column>
        </el-table>
        <div class="picker-selected-footer">本次已加入 {{ selected.length }} 人</div>
      </div>
    </div>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" @click="confirm">加入明细（{{ selected.length }}）</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="editVisible" title="编辑人员信息" width="720px" append-to-body align-center>
    <el-form :model="editForm" label-width="92px">
      <el-row :gutter="12">
        <el-col :span="12"><el-form-item label="姓名" required><el-input v-model="editForm.name" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="身份证号" required><el-input v-model="editForm.idCardNo" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="手机号"><el-input v-model="editForm.phone" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="银行账号" required><el-input v-model="editForm.bankAccount" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="户名" required><el-input v-model="editForm.accountName" /></el-form-item></el-col>
        <el-col :span="12">
          <el-form-item label="银行类型">
            <el-select v-model="editForm.bankType" clearable style="width: 100%">
              <el-option v-for="option in bankTypeOptions" :key="option" :label="option" :value="option" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24"><el-form-item label="行名"><el-input v-model="editForm.bankName" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="联行行号"><el-input v-model="editForm.cnapsNo" /></el-form-item></el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="editVisible = false">取消</el-button>
      <el-button type="primary" @click="submitEdit">保存</el-button>
    </template>
  </el-dialog>
</template>
