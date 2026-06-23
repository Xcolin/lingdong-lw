<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import { adminSalaryQuery, publicSalaryQuery, type SalaryQueryRecord } from '../../api/salary-query'
import PaginationBar from '../../components/PaginationBar.vue'
import { formatDateTime } from '../../utils/date'

const auth = useAuthStore()
const publicLoading = ref(false)
const adminLoading = ref(false)
const publicRows = ref<SalaryQueryRecord[]>([])
const adminRows = ref<SalaryQueryRecord[]>([])
const adminTotal = ref(0)
const adminPage = ref(1)
const adminSize = ref(20)
const publicForm = reactive({ name: '', idCardNo: '' })
const adminForm = reactive({ name: '' })
const canAdminQuery = computed(() => auth.hasPermission('salary:view'))

async function queryPublic() {
  if (!publicForm.name.trim()) {
    ElMessage.warning('请输入姓名')
    return
  }
  if (!publicForm.idCardNo.trim()) {
    ElMessage.warning('请输入身份证号')
    return
  }
  publicLoading.value = true
  try {
    const data: any = await publicSalaryQuery({ name: publicForm.name.trim(), idCardNo: publicForm.idCardNo.trim() })
    publicRows.value = data.records || []
    if (publicRows.value.length === 0) {
      ElMessage.info('未查询到工资或预支明细')
    }
  } catch {
    publicRows.value = []
  } finally {
    publicLoading.value = false
  }
}

async function queryAdmin() {
  adminLoading.value = true
  try {
    const data: any = await adminSalaryQuery({ name: adminForm.name, page: adminPage.value, size: adminSize.value })
    adminRows.value = data.records || []
    adminTotal.value = data.total || 0
  } catch {
    adminRows.value = []
    adminTotal.value = 0
  } finally {
    adminLoading.value = false
  }
}

function searchAdmin() {
  adminPage.value = 1
  queryAdmin()
}

function maskIdCard(value: string) {
  if (!value || value.length < 10) return value
  return `${value.slice(0, 6)}********${value.slice(-4)}`
}

function sourceLabel(row: SalaryQueryRecord) {
  return row.sourceType === 'ADVANCE' ? '预支' : '工资'
}
</script>

<template>
  <div class="salary-query-page">
    <section class="salary-query-shell">
      <div class="salary-query-header">
        <div class="brand-mini">薪</div>
        <div>
          <h1>工资发放查询</h1>
          <p>输入姓名和身份证号查询本人工资明细</p>
        </div>
      </div>

      <div class="query-card">
        <el-form :model="publicForm" label-position="top" class="query-form" @submit.prevent>
          <el-form-item label="姓名" required>
            <el-input v-model="publicForm.name" size="large" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="身份证号" required>
            <el-input v-model="publicForm.idCardNo" size="large" placeholder="请输入身份证号" @keyup.enter="queryPublic" />
          </el-form-item>
          <el-button type="primary" size="large" :icon="Search" :loading="publicLoading" @click="queryPublic">查询</el-button>
        </el-form>
      </div>

      <div v-if="publicRows.length > 0" class="result-list">
        <div v-for="row in publicRows" :key="row.itemId" class="salary-record">
          <div class="record-top">
            <strong><el-tag size="small" :type="row.sourceType === 'ADVANCE' ? 'warning' : 'success'">{{ sourceLabel(row) }}</el-tag>{{ row.batchName }}</strong>
            <span>{{ Number(row.amount || 0).toFixed(2) }}</span>
          </div>
          <div class="record-grid">
            <label>姓名</label><span>{{ row.name }}</span>
            <label>身份证号</label><span>{{ maskIdCard(row.idCardNo) }}</span>
            <label>{{ row.sourceType === 'ADVANCE' ? '预支时间' : '做表时间' }}</label><span>{{ formatDateTime(row.batchCreatedAt) }}</span>
            <label>发放日期</label><span>{{ row.payDate ? formatDateTime(row.payDate) : '-' }}</span>
            <label>摘要</label><span>{{ row.summary || '-' }}</span>
            <label>备注</label><span>{{ row.remark || '-' }}</span>
          </div>
        </div>
      </div>

      <div v-if="canAdminQuery" class="admin-panel">
        <div class="admin-panel-title">
          <el-icon><User /></el-icon>
          <span>管理员查询</span>
        </div>
        <div class="admin-toolbar">
          <el-input v-model="adminForm.name" clearable placeholder="姓名模糊查询" style="width: 240px" @keyup.enter="searchAdmin" />
          <el-button type="primary" @click="searchAdmin">查询</el-button>
        </div>
        <el-table v-loading="adminLoading" :data="adminRows" border height="320px">
          <el-table-column label="序号" width="70" align="center">
            <template #default="{ $index }">{{ (adminPage - 1) * adminSize + $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="类型" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.sourceType === 'ADVANCE' ? 'warning' : 'success'">{{ sourceLabel(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="batchName" label="批次" min-width="180" show-overflow-tooltip />
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="idCardNo" label="身份证号" width="180" show-overflow-tooltip />
          <el-table-column prop="amount" label="金额" width="120" align="right" />
          <el-table-column prop="summary" label="摘要" width="100" />
          <el-table-column label="做表时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.batchCreatedAt) }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        </el-table>
        <PaginationBar v-model:page="adminPage" v-model:size="adminSize" :total="adminTotal" compact @change="queryAdmin" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.salary-query-page {
  height: 100vh;
  min-height: 0;
  background: #edf5f8;
  padding: 24px;
  overflow: auto;
}

.salary-query-shell {
  width: min(1080px, 100%);
  min-height: 100%;
  margin: 0 auto;
  padding-bottom: 24px;
}

.salary-query-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 10px 0 20px;
}

.brand-mini {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  background: #1677a3;
  font-weight: 700;
}

.salary-query-header h1 {
  margin: 0;
  font-size: 24px;
}

.salary-query-header p {
  margin: 4px 0 0;
  color: #6b7785;
}

.query-card,
.salary-record,
.admin-panel {
  background: #fff;
  border: 1px solid #dfe7ef;
  border-radius: 8px;
  padding: 16px;
}

.query-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr) auto;
  gap: 12px;
  align-items: end;
}

.query-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.result-list,
.admin-panel {
  margin-top: 16px;
}

.salary-record + .salary-record {
  margin-top: 12px;
}

.record-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf1f5;
}

.record-top strong {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-top span {
  color: #1677a3;
  font-size: 20px;
  font-weight: 700;
}

.record-grid {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr) 76px minmax(0, 1fr);
  gap: 10px 12px;
  margin-top: 12px;
}

.record-grid label {
  color: #6b7785;
}

.record-grid span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-panel-title,
.admin-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-panel-title {
  font-weight: 700;
  margin-bottom: 12px;
}

.admin-toolbar {
  margin-bottom: 12px;
}

@media (max-width: 720px) {
  .salary-query-page {
    padding: 16px;
  }

  .query-form,
  .record-grid {
    grid-template-columns: 1fr;
  }

  .query-form .el-button {
    width: 100%;
  }

  .record-grid {
    gap: 4px;
  }

  .record-grid label {
    margin-top: 8px;
  }

  .admin-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-toolbar .el-input,
  .admin-toolbar .el-button {
    width: 100% !important;
  }
}
</style>
