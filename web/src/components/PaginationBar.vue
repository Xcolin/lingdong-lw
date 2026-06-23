<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  page: number
  size: number
  total: number
  compact?: boolean
}>()

const layout = computed(() => props.compact ? 'total, sizes, prev, pager, next' : 'total, sizes, prev, pager, next, jumper')

const emit = defineEmits<{
  'update:page': [value: number]
  'update:size': [value: number]
  change: []
}>()

function changePage(value: number) {
  emit('update:page', value)
  emit('change')
}

function changeSize(value: number) {
  emit('update:size', value)
  emit('update:page', 1)
  emit('change')
}
</script>

<template>
  <div class="pagination-bar">
    <el-pagination
      background
      :current-page="page"
      :page-size="size"
      :page-sizes="[10, 20, 50, 100, 200]"
      :total="total"
      :layout="layout"
      @current-change="changePage"
      @size-change="changeSize"
    />
  </div>
</template>
