<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Check, Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteBatch, listBatches, markBatchActualPaid, type PayrollBatch } from '../../api/batches'
import PaginationBar from '../../components/PaginationBar.vue'
import { formatDateTime } from '../../utils/date'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const keyword = ref('')
const createdRange = ref<[string, string]>()
const loading = ref(false)
const rows = ref<PayrollBatch[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const data: any = await listBatches({
      keyword: keyword.value,
      startDate: createdRange.value?.[0],
      endDate: createdRange.value?.[1],
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

async function remove(row: PayrollBatch) {
  await ElMessageBox.confirm(`确认删除批次 ${row.batchName}？`, '删除批次')
  await deleteBatch(row.id!)
  ElMessage.success('已删除')
  load()
}

async function markPaid(row: PayrollBatch) {
  await ElMessageBox.confirm(`确认将批次 ${row.batchName} 标记为实际已发？标记后员工可查询，工作台会纳入统计。`, '实际已发')
  await markBatchActualPaid(row.id!)
  ElMessage.success('已标记实际已发')
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-fill">
    <div class="page-title">
      <h1>工资批次</h1>
      <el-button v-if="auth.hasPermission('batch:create')" type="primary" :icon="Plus" @click="router.push('/batches/new')">新建批次</el-button>
    </div>
    <div class="panel panel-fill">
      <div class="toolbar">
        <el-input v-model="keyword" clearable placeholder="批次名称" style="width: 320px" @keyup.enter="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-date-picker
          v-model="createdRange"
          type="daterange"
          range-separator="至"
          start-placeholder="创建开始"
          end-placeholder="创建结束"
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
        <el-table-column prop="batchName" label="批次名称" min-width="220" fixed />
        <el-table-column label="发放日期" width="180">
          <template #default="{ row }">{{ formatDateTime(row.payDate) }}</template>
        </el-table-column>
        <el-table-column prop="defaultSummary" label="默认摘要" width="120" />
        <el-table-column prop="totalPeople" label="总笔数" width="100" />
        <el-table-column prop="totalAmount" label="总发金额" width="140" align="right" />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="creatorName" label="创建人" width="120" />
        <el-table-column label="实际已发" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.actualPaid ? 'success' : 'info'" size="small">
              {{ row.actualPaid ? '已发' : '未发' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button v-if="auth.hasPermission('batch:update')" :icon="Edit" link type="primary" @click="router.push(`/batches/${row.id}`)" />
            <el-button v-if="auth.hasPermission('batch:update') && !row.actualPaid" :icon="Check" link type="success" @click="markPaid(row)">实际已发</el-button>
            <el-button v-if="auth.hasPermission('batch:delete') && !row.actualPaid" :icon="Delete" link type="danger" @click="remove(row)" />
          </template>
        </el-table-column>
      </el-table>
      <PaginationBar v-model:page="page" v-model:size="size" :total="total" @change="load" />
    </div>
  </div>
</template>
