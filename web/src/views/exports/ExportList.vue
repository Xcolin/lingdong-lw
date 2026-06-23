<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { downloadExport, listExports } from '../../api/exports'
import PaginationBar from '../../components/PaginationBar.vue'
import { formatDateTime } from '../../utils/date'

interface ExportRecord {
  id: number
  batchId: number
  payingUnitId: number
  templateType: string
  fileName: string
  totalPeople: number
  totalAmount: number
  createdAt: string
}

const loading = ref(false)
const rows = ref<ExportRecord[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const data: any = await listExports({ page: page.value, size: size.value })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function download(row: ExportRecord) {
  const blob = await downloadExport(row.id)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = row.fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

onMounted(load)
</script>

<template>
  <div class="page-fill">
    <div class="page-title">
      <h1>导出记录</h1>
      <el-button @click="load">刷新</el-button>
    </div>
    <div class="panel panel-fill">
      <el-table v-loading="loading" :data="rows" border class="table-fill" height="100%">
        <el-table-column label="序号" width="70" fixed align="center">
          <template #default="{ $index }">{{ (page - 1) * size + $index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" min-width="280" fixed />
        <el-table-column prop="templateType" label="银行模板" width="120" />
        <el-table-column prop="totalPeople" label="总笔数" width="100" />
        <el-table-column prop="totalAmount" label="总金额" width="140" align="right" />
        <el-table-column label="导出时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button :icon="Download" link type="primary" @click="download(row)">下载</el-button>
          </template>
        </el-table-column>
      </el-table>
      <PaginationBar v-model:page="page" v-model:size="size" :total="total" @change="load" />
    </div>
  </div>
</template>
