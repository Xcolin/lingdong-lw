<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { listLogs } from '../../api/logs'
import PaginationBar from '../../components/PaginationBar.vue'
import { formatDateTime } from '../../utils/date'

interface OperationLog {
  id: number
  operatorName: string
  action: string
  module: string
  businessId: number
  beforeValue?: string
  afterValue?: string
  operatedAt: string
}

const loading = ref(false)
const rows = ref<OperationLog[]>([])
const filters = reactive({ module: '', action: '' })
const page = ref(1)
const size = ref(20)
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const data: any = await listLogs({ ...filters, page: page.value, size: size.value })
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

onMounted(load)
</script>

<template>
  <div class="page-fill">
    <div class="page-title">
      <h1>操作日志</h1>
    </div>
    <div class="panel panel-fill">
      <div class="toolbar">
        <div style="display: flex; gap: 10px">
          <el-input v-model="filters.module" placeholder="模块" clearable style="width: 180px" />
          <el-select v-model="filters.action" clearable placeholder="操作类型" style="width: 160px">
            <el-option label="新增" value="CREATE" />
            <el-option label="修改" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
            <el-option label="导出" value="EXPORT" />
          </el-select>
        </div>
        <el-button :icon="Search" @click="search">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" border class="table-fill" height="100%">
        <el-table-column label="序号" width="70" fixed align="center">
          <template #default="{ $index }">{{ (page - 1) * size + $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="操作时间" width="180" fixed>
          <template #default="{ row }">{{ formatDateTime(row.operatedAt) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="130" />
        <el-table-column prop="action" label="操作" width="100" />
        <el-table-column prop="module" label="模块" width="150" />
        <el-table-column prop="businessId" label="业务ID" width="100" />
        <el-table-column prop="beforeValue" label="修改前" min-width="240" show-overflow-tooltip />
        <el-table-column prop="afterValue" label="修改后" min-width="240" show-overflow-tooltip />
      </el-table>
      <PaginationBar v-model:page="page" v-model:size="size" :total="total" @change="load" />
    </div>
  </div>
</template>
