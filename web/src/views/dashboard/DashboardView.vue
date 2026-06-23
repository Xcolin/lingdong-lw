<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { DataAnalysis, Money, OfficeBuilding, Refresh, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getDashboardSummary, type DashboardSummary } from '../../api/dashboard'

const loading = ref(false)
const range = ref<[string, string]>()
const summary = ref<DashboardSummary>()

const stats = computed(() => {
  const data = summary.value
  return [
    { label: '工资批次', value: data?.payroll.batchCount || 0, unit: '个', icon: DataAnalysis, tone: 'blue' },
    { label: '发放/预支笔数', value: data?.payroll.itemCount || 0, unit: '笔', icon: Money, tone: 'green' },
    { label: '发放/预支金额', value: money(data?.payroll.totalAmount || 0), unit: '元', icon: Money, tone: 'amber' },
    { label: '导出次数', value: data?.payroll.exportCount || 0, unit: '次', icon: Refresh, tone: 'slate' }
  ]
})

function defaultRange() {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 29)
  range.value = [toDateValue(start), toDateValue(end)]
}

function toDateValue(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function money(value: number) {
  return Number(value || 0).toFixed(2)
}

async function load() {
  if (!range.value) {
    ElMessage.warning('请选择统计时间')
    return
  }
  loading.value = true
  try {
    summary.value = await getDashboardSummary({ startDate: range.value[0], endDate: range.value[1] })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  defaultRange()
  load()
})
</script>

<template>
  <div class="page-fill">
    <div class="page-title">
      <h1>工作台</h1>
      <div class="dashboard-actions">
        <el-date-picker
          v-model="range"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 280px"
        />
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div v-loading="loading" class="dashboard-content">
      <div class="dashboard-grid">
        <div v-for="item in stats" :key="item.label" class="metric-card" :class="`metric-${item.tone}`">
          <div class="metric-icon"><el-icon><component :is="item.icon" /></el-icon></div>
          <div>
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.unit }}</small>
          </div>
        </div>
      </div>

      <div class="analysis-grid">
        <section class="analysis-panel">
          <div class="analysis-title">
            <el-icon><User /></el-icon>
            <span>员工情况</span>
          </div>
          <div class="analysis-row">
            <span>新增人员</span>
            <strong>{{ summary?.person.createdCount || 0 }}</strong>
          </div>
          <div class="analysis-row">
            <span>启用人员</span>
            <strong>{{ summary?.person.enabledCount || 0 }}</strong>
          </div>
          <div class="analysis-row muted">
            <span>停用人员</span>
            <strong>{{ summary?.person.disabledCount || 0 }}</strong>
          </div>
        </section>

        <section class="analysis-panel">
          <div class="analysis-title">
            <el-icon><OfficeBuilding /></el-icon>
            <span>单位情况</span>
          </div>
          <div class="analysis-row">
            <span>新增单位</span>
            <strong>{{ summary?.unit.createdCount || 0 }}</strong>
          </div>
          <div class="analysis-row">
            <span>启用单位</span>
            <strong>{{ summary?.unit.enabledCount || 0 }}</strong>
          </div>
          <div class="analysis-row muted">
            <span>停用单位</span>
            <strong>{{ summary?.unit.disabledCount || 0 }}</strong>
          </div>
        </section>

        <section class="analysis-panel payroll-panel">
          <div class="analysis-title">
            <el-icon><DataAnalysis /></el-icon>
            <span>工资发放情况</span>
          </div>
          <div class="payroll-summary">
            <div>
              <span>统计周期</span>
              <strong>{{ summary?.startDate }} 至 {{ summary?.endDate }}</strong>
            </div>
            <div>
              <span>平均每批金额</span>
              <strong>{{ money((summary?.payroll.totalAmount || 0) / Math.max(summary?.payroll.batchCount || 0, 1)) }}</strong>
            </div>
            <div>
              <span>平均每笔金额</span>
              <strong>{{ money((summary?.payroll.totalAmount || 0) / Math.max(summary?.payroll.itemCount || 0, 1)) }}</strong>
            </div>
            <div>
              <span>预支笔数</span>
              <strong>{{ summary?.payroll.advanceCount || 0 }}</strong>
            </div>
            <div>
              <span>预支金额</span>
              <strong>{{ money(summary?.payroll.advanceAmount || 0) }}</strong>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dashboard-content {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card,
.analysis-panel {
  background: #fff;
  border: 1px solid var(--payroll-border);
  border-radius: 8px;
}

.metric-card {
  min-height: 112px;
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.metric-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #fff;
}

.metric-card span,
.analysis-row span,
.payroll-summary span {
  color: var(--payroll-muted);
  font-size: 13px;
}

.metric-card strong {
  display: inline-block;
  margin-top: 6px;
  font-size: 26px;
  color: var(--payroll-text);
}

.metric-card small {
  margin-left: 6px;
  color: var(--payroll-muted);
}

.metric-blue .metric-icon { background: #1677a3; }
.metric-green .metric-icon { background: #2f9e75; }
.metric-amber .metric-icon { background: #c9831f; }
.metric-slate .metric-icon { background: #52616f; }

.analysis-grid {
  flex: 1 1 auto;
  min-height: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: auto 1fr;
  gap: 14px;
}

.analysis-panel {
  padding: 16px;
}

.analysis-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  margin-bottom: 12px;
}

.analysis-row {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #edf1f5;
}

.analysis-row:last-child {
  border-bottom: 0;
}

.analysis-row strong {
  font-size: 20px;
}

.analysis-row.muted strong {
  color: var(--payroll-muted);
}

.payroll-panel {
  grid-column: 1 / span 2;
}

.payroll-summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.payroll-summary div {
  padding: 14px;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  background: #fbfdff;
}

.payroll-summary strong {
  display: block;
  margin-top: 8px;
  font-size: 20px;
}

@media (max-width: 960px) {
  .dashboard-grid,
  .analysis-grid,
  .payroll-summary {
    grid-template-columns: 1fr;
  }

  .payroll-panel {
    grid-column: auto;
  }

  .dashboard-content {
    overflow: auto;
  }
}
</style>
