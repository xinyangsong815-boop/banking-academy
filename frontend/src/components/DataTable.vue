<script setup lang="ts" generic="T extends { id: string | number }">
defineProps<{
  rows: T[]
  columns: { key: string; label: string }[]
  loading?: boolean
  error?: string
}>()
defineEmits<{ retry: [] }>()
function cellValue(row: T, key: string) {
  return (row as Record<string, unknown>)[key]
}
</script>
<template>
  <div
    v-if="error"
    class="error-panel"
    role="alert"
  >
    {{ error }}
    <button
      class="text-button"
      @click="$emit('retry')"
    >
      重新加载
    </button>
  </div>
  <div
    class="table-scroll"
    :aria-busy="loading"
  >
    <table>
      <thead>
        <tr>
          <th
            v-for="column in columns"
            :key="column.key"
            scope="col"
          >
            {{ column.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="row in rows"
          :key="row.id"
        >
          <td
            v-for="column in columns"
            :key="column.key"
          >
            <slot
              :name="column.key"
              :row="row"
              >{{ cellValue(row, column.key) }}</slot
            >
          </td>
        </tr>
        <tr v-if="!rows.length">
          <td
            :colspan="columns.length"
            class="empty-state"
          >
            {{ loading ? '正在读取数据…' : '暂无记录，可以从一次业务操作开始。' }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
