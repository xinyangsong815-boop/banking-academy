<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { bankApi } from '../api/bank'
import { useResource } from '../composables/useResource'
defineProps<{ modelValue: number; id: string }>()
const emit = defineEmits<{ 'update:modelValue': [id: number] }>()
const page = ref(1)
const keyword = ref('')
const selectedLabel = ref('已选客户')
const { data, loading, error, reload } = useResource(() =>
  bankApi.customers(page.value, keyword.value),
)
watch(page, reload)
onMounted(reload)
function choose(event: Event) {
  const element = event.target as HTMLSelectElement
  selectedLabel.value = element.selectedOptions[0]?.textContent ?? '已选客户'
  emit('update:modelValue', Number(element.value))
}
function search() {
  if (page.value === 1) void reload()
  else page.value = 1
}
</script>
<template>
  <div class="picker">
    <div class="picker-search">
      <input
        v-model="keyword"
        :aria-label="`${id} 客户搜索`"
        placeholder="姓名或手机号"
        @keydown.enter.prevent="search"
      /><button
        type="button"
        class="button secondary small"
        @click="search"
      >
        搜索
      </button>
    </div>
    <select
      :id="id"
      :value="modelValue"
      required
      :disabled="loading"
      @change="choose"
    >
      <option
        :value="0"
        disabled
      >
        请选择客户
      </option>
      <option
        v-if="modelValue && !data?.items.some((item) => item.id === modelValue)"
        :value="modelValue"
      >
        {{ selectedLabel }} #{{ modelValue }}
      </option>
      <option
        v-for="item in data?.items"
        :key="item.id"
        :value="item.id"
      >
        {{ item.name }} · {{ item.phone }}
      </option>
    </select>
    <div class="picker-pages">
      <span>{{ error || `第 ${page} 页 / 共 ${data?.total ?? 0} 位客户` }}</span
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
