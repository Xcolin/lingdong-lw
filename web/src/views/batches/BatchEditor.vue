<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download, OfficeBuilding, Plus, Select, UserFilled } from '@element-plus/icons-vue'
import { createBatch, getBatch, updateBatch, type BatchItem, type PayrollBatch } from '../../api/batches'
import { createExport } from '../../api/exports'
import { listUnits, type PayingUnit } from '../../api/units'
import type { PayeePerson } from '../../api/persons'
import PersonPickerDialog from './PersonPickerDialog.vue'
import UnitPickerDialog from './UnitPickerDialog.vue'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const batchId = computed(() => route.params.id ? Number(route.params.id) : undefined)
const pickerVisible = ref(false)
const unitPickerVisible = ref(false)
const exportVisible = ref(false)
const units = ref<PayingUnit[]>([])
const savedBatchId = ref<number>()
const bankTypeOptions = ['中国建设银行', '中国银行', '其他银行']
const form = reactive<PayrollBatch>({
  batchName: '',
  payDate: '',
  defaultSummary: '工资',
  remark: '',
  items: []
})
const exportForm = reactive<{ payingUnitId?: number; templateType: 'CCB' | 'BOC' }>({ templateType: 'CCB' })

const totalItems = computed(() => form.items?.length || 0)
const totalAmount = computed(() => (form.items || []).reduce((sum, item) => sum + Number(item.amount || 0), 0))
const missingBank = computed(() => (form.items || []).filter((item) => !item.bankAccount || !item.accountName).length)
const missingBankName = computed(() => (form.items || []).filter((item) => !item.bankName).length)
const existingIdCards = computed(() => (form.items || []).filter((item) => item.targetType !== 'UNIT').map((item) => item.idCardNo).filter(Boolean))
const existingUnitAccounts = computed(() => (form.items || []).filter((item) => item.targetType === 'UNIT').map((item) => item.bankAccount).filter(Boolean))
const manualAccountNameRows = new WeakSet<BatchItem>()
const previousNameRows = new WeakMap<BatchItem, string>()

function isBlank(value: unknown) {
  return value === undefined || value === null || String(value).trim() === ''
}

function addBlankRow() {
  form.items?.push({
    targetType: 'PERSON',
    name: '',
    idCardNo: '',
    phone: '',
    bankAccount: '',
    accountName: '',
    bankName: '',
    bankType: '',
    bankCategory: '',
    cnapsNo: '',
    amount: 0,
    summary: form.defaultSummary || '工资',
    remark: ''
  })
}

function addSelected(rows: PayeePerson[]) {
  rows.forEach((person) => {
    form.items?.push({
      targetType: 'PERSON',
      personId: person.id,
      name: person.name,
      idCardNo: person.idCardNo,
      phone: person.phone,
      bankAccount: person.bankAccount,
      accountName: person.accountName,
      bankName: person.bankName,
      bankType: person.bankType,
      bankCategory: person.bankCategory,
      cnapsNo: person.cnapsNo,
      amount: 0,
      summary: form.defaultSummary || '工资',
      remark: ''
    })
  })
}

function addSelectedUnits(rows: PayingUnit[]) {
  rows.forEach((unit) => {
    form.items?.push({
      targetType: 'UNIT',
      unitId: unit.id,
      name: unit.accountName,
      idCardNo: '',
      phone: '',
      bankAccount: unit.bankAccount,
      accountName: unit.accountName,
      bankName: unit.bankName,
      bankType: unit.bankType,
      bankCategory: unit.bankCategory,
      cnapsNo: unit.cnapsNo,
      amount: 0,
      summary: form.defaultSummary || '工资',
      remark: ''
    })
  })
}

function setSummary(value: string) {
  form.items?.forEach((item) => item.summary = value)
}

function removeRow(index: number) {
  form.items?.splice(index, 1)
}

function rememberName(row: BatchItem) {
  previousNameRows.set(row, row.name || '')
}

function syncAccountName(row: BatchItem, value: string | number) {
  const previousName = previousNameRows.get(row) || ''
  if (!manualAccountNameRows.has(row) && (isBlank(row.accountName) || row.accountName === previousName)) {
    row.accountName = String(value || '')
  }
  previousNameRows.set(row, String(value || ''))
}

function markAccountNameManual(row: BatchItem) {
  manualAccountNameRows.add(row)
}

async function load() {
  if (!batchId.value) return
  const data: any = await getBatch(batchId.value)
  savedBatchId.value = data.id
  const items = (data.items || []).map((item: BatchItem) => ({ ...item, targetType: item.targetType || 'PERSON' }))
  Object.assign(form, data, { items })
}

function validateBatch() {
  if (isBlank(form.batchName)) {
    ElMessage.warning('请填写批次名称')
    return false
  }
  if (!form.items || form.items.length === 0) {
    ElMessage.warning('请先添加工资明细')
    return false
  }
  const requiredFields: Array<[keyof BatchItem, string]> = [
    ['name', '姓名'],
    ['bankAccount', '银行账号'],
    ['accountName', '户名'],
    ['bankName', '行名']
  ]
  for (let index = 0; index < form.items.length; index += 1) {
    const item = form.items[index]
    item.targetType = item.targetType || 'PERSON'
    if (item.targetType === 'UNIT' && isBlank(item.name)) {
      item.name = item.accountName
    }
    const missing = requiredFields.find(([key]) => isBlank(item[key]))
    if (missing) {
      ElMessage.warning(`第 ${index + 1} 行请填写${missing[1]}`)
      return false
    }
    if (item.targetType !== 'UNIT' && isBlank(item.idCardNo)) {
      ElMessage.warning(`第 ${index + 1} 行请填写身份证号`)
      return false
    }
    if (!item.amount || Number(item.amount) <= 0) {
      ElMessage.warning(`第 ${index + 1} 行金额必须大于 0`)
      return false
    }
  }
  return true
}

async function save() {
  if (!validateBatch()) {
    return false
  }
  const payload = { ...form, totalPeople: undefined, totalAmount: undefined }
  if (batchId.value) {
    await updateBatch(batchId.value, payload)
    savedBatchId.value = batchId.value
  } else {
    const data: any = await createBatch(payload)
    savedBatchId.value = data.id
    router.replace(`/batches/${data.id}`)
  }
  ElMessage.success('批次已保存')
  return true
}

async function openExport() {
  const saved = await save()
  if (!saved) return
  const data: any = await listUnits({ page: 1, size: 200 })
  units.value = data.records || []
  exportVisible.value = true
}

async function submitExport() {
  const currentBatchId = batchId.value || savedBatchId.value
  if (!currentBatchId) {
    ElMessage.warning('请先保存工资批次')
    return
  }
  if (!exportForm.payingUnitId) {
    ElMessage.warning('请选择付款单位')
    return
  }
  if (!exportForm.templateType) {
    ElMessage.warning('请选择银行模板')
    return
  }
  const data: any = await createExport({ batchId: currentBatchId, payingUnitId: exportForm.payingUnitId, templateType: exportForm.templateType })
  ElMessage.success(`导出成功：${data.fileName}`)
  exportVisible.value = false
}

onMounted(load)
</script>

<template>
  <div class="page-fill">
    <div class="page-title">
      <h1>{{ batchId ? '编辑工资批次' : '新建工资批次' }}</h1>
      <div>
        <el-button @click="router.push('/batches')">返回</el-button>
        <el-button v-if="auth.hasPermission(batchId ? 'batch:update' : 'batch:create')" type="primary" :icon="Select" @click="save">保存</el-button>
        <el-button v-if="auth.hasPermission('batch:export')" type="success" :icon="Download" @click="openExport">导出</el-button>
      </div>
    </div>

    <div class="stats-grid">
      <div class="stat"><span>总笔数</span><strong>{{ totalItems }}</strong></div>
      <div class="stat"><span>总发金额</span><strong>{{ totalAmount.toFixed(2) }}</strong></div>
      <div class="stat"><span>缺银行信息</span><strong>{{ missingBank }}</strong></div>
      <div class="stat"><span>缺行名</span><strong>{{ missingBankName }}</strong></div>
    </div>

    <div class="panel panel-fill">
      <el-form :model="form" label-width="84px" class="batch-form">
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="批次名称" required><el-input v-model="form.batchName" /></el-form-item></el-col>
          <el-col :span="5"><el-form-item label="发放日期"><el-date-picker v-model="form.payDate" format="YYYY-MM-DD 00:00:00" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="5"><el-form-item label="默认摘要"><el-select v-model="form.defaultSummary"><el-option label="工资" value="工资" /><el-option label="奖金" value="奖金" /><el-option label="报销" value="报销" /><el-option label="劳务费" value="劳务费" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item></el-col>
        </el-row>
      </el-form>

      <div class="toolbar">
        <div>
          <el-button type="primary" :icon="UserFilled" @click="pickerVisible = true">批量选人员</el-button>
          <el-button type="primary" :icon="OfficeBuilding" @click="unitPickerVisible = true">批量选单位</el-button>
          <el-button :icon="Plus" @click="addBlankRow">新增空行</el-button>
          <el-button @click="setSummary(form.defaultSummary || '工资')">批量设置摘要</el-button>
        </div>
      </div>

      <el-table :data="form.items" border class="table-fill" height="100%">
        <el-table-column label="序号" type="index" width="70" fixed align="center" />
        <el-table-column label="类型" width="92" fixed>
          <template #default="{ row }">
            <el-select v-model="row.targetType" style="width: 74px">
              <el-option label="人员" value="PERSON" />
              <el-option label="单位" value="UNIT" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column width="140" fixed>
          <template #header><span class="required-label">收款对象</span></template>
          <template #default="{ row }"><el-input v-model="row.name" @focus="rememberName(row)" @input="syncAccountName(row, $event)" /></template>
        </el-table-column>
        <el-table-column width="190">
          <template #header><span class="required-label">银行账号</span></template>
          <template #default="{ row }"><el-input v-model="row.bankAccount" /></template>
        </el-table-column>
        <el-table-column width="130">
          <template #header><span class="required-label">户名</span></template>
          <template #default="{ row }"><el-input v-model="row.accountName" @input="markAccountNameManual(row)" /></template>
        </el-table-column>
        <el-table-column width="220">
          <template #header><span class="required-label">行名</span></template>
          <template #default="{ row }"><el-input v-model="row.bankName" /></template>
        </el-table-column>
        <el-table-column label="银行类型" width="150">
          <template #default="{ row }">
            <el-select v-model="row.bankType" clearable style="width: 132px">
              <el-option v-for="option in bankTypeOptions" :key="option" :label="option" :value="option" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column width="130" align="right">
          <template #header><span class="required-label">金额</span></template>
          <template #default="{ row }"><el-input-number v-model="row.amount" :min="0" :precision="2" controls-position="right" style="width: 112px" /></template>
        </el-table-column>
        <el-table-column label="摘要" width="120"><template #default="{ row }"><el-select v-model="row.summary"><el-option label="工资" value="工资" /><el-option label="奖金" value="奖金" /><el-option label="报销" value="报销" /><el-option label="劳务费" value="劳务费" /></el-select></template></el-table-column>
        <el-table-column label="备注" width="180"><template #default="{ row }"><el-input v-model="row.remark" /></template></el-table-column>
        <el-table-column label="联行行号" width="150"><template #default="{ row }"><el-input v-model="row.cnapsNo" /></template></el-table-column>
        <el-table-column width="190">
          <template #header>身份证号<span class="field-hint">人员必填</span></template>
          <template #default="{ row }"><el-input v-model="row.idCardNo" /></template>
        </el-table-column>
        <el-table-column label="手机号" width="130"><template #default="{ row }"><el-input v-model="row.phone" /></template></el-table-column>
        <el-table-column label="操作" width="80" fixed="right"><template #default="{ $index }"><el-button link type="danger" @click="removeRow($index)">删除</el-button></template></el-table-column>
      </el-table>
    </div>

    <PersonPickerDialog v-model="pickerVisible" :existing-id-cards="existingIdCards" @selected="addSelected" />
    <UnitPickerDialog v-model="unitPickerVisible" :existing-bank-accounts="existingUnitAccounts" @selected="addSelectedUnits" />

    <el-dialog v-model="exportVisible" title="导出银行模板" width="520px">
      <el-form label-width="90px">
        <el-form-item label="付款单位" required>
          <el-select v-model="exportForm.payingUnitId" filterable style="width: 100%">
            <el-option v-for="unit in units" :key="unit.id" :label="`${unit.accountName} / ${unit.bankAccount}`" :value="unit.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="银行模板" required>
          <el-segmented v-model="exportForm.templateType" :options="[{ label: '建设银行', value: 'CCB' }, { label: '中国银行', value: 'BOC' }]" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="primary" @click="submitExport">生成文件</el-button>
      </template>
    </el-dialog>
  </div>
</template>
