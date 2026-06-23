<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Search } from '@element-plus/icons-vue'
import { listUnits, type PayingUnit } from '../../api/units'
import PaginationBar from '../../components/PaginationBar.vue'

const props = defineProps<{ modelValue: boolean; existingBankAccounts: string[] }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; selected: [rows: PayingUnit[]] }>()

const keyword = ref('')
const loading = ref(false)
const rows = ref<PayingUnit[]>([])
const leftTableRef = ref()
const leftChecked = ref<PayingUnit[]>([])
const selected = ref<PayingUnit[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const existingAccountSet = computed(() => new Set(props.existingBankAccounts.filter(Boolean)))
const selectedKeys = computed(() => new Set(selected.value.map((row) => unitKey(row))))
const availableRows = computed(() => rows.value.filter((row) => !existingAccountSet.value.has(row.bankAccount) && !selectedKeys.value.has(unitKey(row))))

watch(() => props.modelValue, (visible) => {
  if (visible) {
    page.value = 1
    selected.value = []
    clearLeftChecked()
    load()
  }
})

function unitKey(row: PayingUnit) {
  return String(row.id || row.bankAccount || row.accountName)
}

async function load() {
  loading.value = true
  try {
    const data: any = await listUnits({ keyword: keyword.value, enabled: true, page: page.value, size: size.value })
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
    ElMessage.warning('请先选择要加入的单位')
    return
  }
  const nextRows = leftChecked.value.filter((row) => !selectedKeys.value.has(unitKey(row)))
  selected.value = [...selected.value, ...nextRows]
  clearLeftChecked()
}

function removeSelected(row: PayingUnit) {
  selected.value = selected.value.filter((item) => unitKey(item) !== unitKey(row))
}

function confirm() {
  if (selected.value.length === 0) {
    ElMessage.warning('请先加入单位')
    return
  }
  emit('selected', selected.value)
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="批量选择单位"
    width="min(1280px, calc(100vw - 32px))"
    class="picker-dialog"
    align-center
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="picker-dual">
      <div class="picker-pane">
        <div class="picker-pane-header">
          <div class="picker-pane-title">未加入单位</div>
        </div>
        <div class="toolbar picker-toolbar">
          <el-input v-model="keyword" placeholder="户名 / 银行账号 / 行名" clearable @keyup.enter="search">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button @click="search">查询</el-button>
        </div>
        <el-table
          ref="leftTableRef"
          v-loading="loading"
          :data="availableRows"
          :row-key="unitKey"
          border
          height="clamp(340px, calc(100vh - 360px), 500px)"
          @selection-change="leftChecked = $event"
        >
          <el-table-column label="序号" width="70" align="center">
            <template #default="{ $index }">{{ (page - 1) * size + $index + 1 }}</template>
          </el-table-column>
          <el-table-column type="selection" width="44" reserve-selection />
          <el-table-column prop="accountName" label="户名" width="160" />
          <el-table-column prop="bankAccount" label="银行账号" width="190" />
          <el-table-column prop="bankName" label="行名" min-width="220" />
          <el-table-column prop="bankType" label="银行类型" width="120" />
        </el-table>
        <PaginationBar v-model:page="page" v-model:size="size" :total="total" compact class="picker-pagination" @change="load" />
      </div>

      <div class="picker-actions">
        <el-button type="primary" @click="addChecked">&gt;&gt;</el-button>
      </div>

      <div class="picker-pane picker-selected-pane">
        <div class="picker-pane-header">
          <div class="picker-pane-title">已加入单位</div>
          <span class="picker-count">{{ selected.length }} 个</span>
        </div>
        <el-table :data="selected" border height="clamp(340px, calc(100vh - 360px), 500px)" empty-text="请从左侧选择单位加入">
          <el-table-column label="序号" type="index" width="64" align="center" />
          <el-table-column prop="accountName" label="户名" width="160" />
          <el-table-column prop="bankAccount" label="银行账号" min-width="190" show-overflow-tooltip />
          <el-table-column label="操作" width="72" fixed="right" align="center">
            <template #default="{ row }">
              <el-button :icon="Close" link type="danger" @click="removeSelected(row)" />
            </template>
          </el-table-column>
        </el-table>
        <div class="picker-selected-footer">本次已加入 {{ selected.length }} 个</div>
      </div>
    </div>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" @click="confirm">加入明细（{{ selected.length }}）</el-button>
    </template>
  </el-dialog>
</template>
