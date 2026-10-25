<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { bankApi } from '../api/bank'
import { useResource } from '../composables/useResource'
import { money } from '../utils/format'
const props = defineProps<{ modelValue: string; id: string; customerId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [id: string] }>()
const page = ref(1)
const selectedLabel = ref('已选账户')
const { data, loading, error, reload } = useResource(() =>
  bankApi.accounts(page.value, props.customerId),
)
watch(page, reload)
watch(
  () => props.customerId,
  () => {
    if (page.value === 1) void reload()
    else page.value = 1
  },
)
onMounted(reload)
function choose(event: Event) {
  const element = event.target as HTMLSelectElement
  selectedLabel.value = element.selectedOptions[0]?.textContent ?? '已选账户'
  emit('update:modelValue', element.value)
}
</script>
<template>
  <div class="picker">
    <select
      :id="id"
      :value="modelValue"
      required
      :disabled="loading"
      @change="choose"
    >
      <option
        value=""
        disabled
      >
        请选择账户
      </option>
      <option
        v-if="modelValue && !data?.items.some((item) => item.id === modelValue)"
        :value="modelValue"
      >
        {{ selectedLabel }}
      </option>
      <option
        v-for="item in data?.items"
        :key="item.id"
        :value="item.id"
        :disabled="item.status !== 'ACTIVE'"
      >
        {{ item.name }} · ¥{{ money(item.balance)
        }}{{ item.status === 'ACTIVE' ? '' : '（不可用）' }}
      </option>
    </select>
    <div class="picker-pages">
      <span>{{ error || `第 ${page} 页 / 共 ${data?.total ?? 0} 个账户` }}</span
      ><button
        type="button"
        :disabled="loading || page <= 1"
        @click="page--"
      >
        上页</button
      ><button
        type="button"
        :disabled="loading || page * 10 >= (data?.total ?? 0)"
        @click="page++"
      >
        下页</button
      ><button
        v-if="error"
        type="button"
        @click="reload"
      >
        重试
      </button>
    </div>
  </div>
</template>
