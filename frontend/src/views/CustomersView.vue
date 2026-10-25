<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { bankApi } from '../api/bank'
import { useResource } from '../composables/useResource'
import { useCommand } from '../composables/useCommand'
import { useAuthStore } from '../stores/auth'
import { dateTime, riskName } from '../utils/format'
import type { Customer, RiskLevel } from '../types/models'
import PageHeader from '../components/PageHeader.vue'
import DataTable from '../components/DataTable.vue'
import Pagination from '../components/Pagination.vue'
import AppModal from '../components/AppModal.vue'
const auth = useAuthStore()
const page = ref(1)
const keyword = ref('')
const appliedKeyword = ref('')
const { data, loading, error, reload } = useResource(() =>
  bankApi.customers(page.value, appliedKeyword.value),
)
const { pending, run } = useCommand()
const showForm = ref(false)
const editId = ref<number>()
const form = reactive({ name: '', phone: '', email: '', riskLevel: 'LOW' as RiskLevel })
watch(page, reload)
onMounted(reload)
function search() {
  appliedKeyword.value = keyword.value
  if (page.value === 1) void reload()
  else page.value = 1
}
function open(customer?: Customer) {
  editId.value = customer?.id
  Object.assign(
    form,
    customer
      ? {
          name: customer.name,
          phone: customer.phone,
          email: customer.email,
          riskLevel: customer.riskLevel,
        }
      : { name: '', phone: '', email: '', riskLevel: 'LOW' },
  )
  showForm.value = true
}
async function save() {
  const result = await run(() => bankApi.saveCustomer({ ...form }, editId.value), '客户档案已保存')
  if (result) {
    showForm.value = false
    await reload()
  }
}
const columns = [
  { key: 'name', label: '客户姓名' },
  { key: 'phone', label: '联系电话' },
  { key: 'email', label: '邮箱' },
  { key: 'riskLevel', label: '风险承受能力' },
  { key: 'createdAt', label: '创建时间' },
  { key: 'actions', label: '操作' },
]
</script>
<template>
  <PageHeader
    title="客户管理"
    description="维护客户档案，了解每位客户的风险承受能力。"
    ><button
      v-if="auth.can('customer:write')"
      class="button"
      @click="open()"
    >
      ＋ 新建客户
    </button></PageHeader
  >
  <section class="panel">
    <form
      class="toolbar"
      @submit.prevent="search"
    >
      <input
        v-model="keyword"
        aria-label="搜索客户"
        placeholder="搜索客户姓名或手机号"
        maxlength="60"
      /><button class="button secondary">搜索</button
      ><span class="muted">{{ data?.total ?? 0 }} 位客户</span>
    </form>
    <DataTable
      :rows="data?.items ?? []"
      :columns="columns"
      :loading="loading"
      :error="error"
      @retry="reload"
      ><template #name="{ row }"
        ><strong>{{ row.name }}</strong
        ><small class="cell-sub">客户 #{{ row.id }}</small></template
      ><template #riskLevel="{ row }"
        ><span class="badge">{{ riskName[row.riskLevel] }}</span></template
      ><template #createdAt="{ row }">{{ dateTime(row.createdAt) }}</template
      ><template #actions="{ row }"
        ><button
          v-if="auth.can('customer:write')"
          class="text-button"
          @click="open(row)"
        >
          编辑档案</button
        ><span
          v-else
          class="muted"
          >只读</span
        ></template
      ></DataTable
    ><Pagination
      :page="page"
      :total="data?.total ?? 0"
      :loading="loading"
      @change="page = $event"
    />
  </section>
  <AppModal
    v-if="showForm"
    :title="editId ? '编辑客户档案' : '新建客户档案'"
    @close="!pending && (showForm = false)"
    ><form
      class="form-grid"
      @submit.prevent="save"
    >
      <label for="customer-name">客户姓名</label
      ><input
        id="customer-name"
        v-model="form.name"
        required
        maxlength="60"
      /><label for="phone">手机号码</label
      ><input
        id="phone"
        v-model="form.phone"
        required
        pattern="1[3-9][0-9]{9}"
        maxlength="11"
      /><label for="email">电子邮箱</label
      ><input
        id="email"
        v-model="form.email"
        required
        type="email"
        maxlength="120"
      /><label for="risk">风险承受能力</label
      ><select
        id="risk"
        v-model="form.riskLevel"
      >
        <option value="LOW">稳健型</option>
        <option value="MEDIUM">平衡型</option>
        <option value="HIGH">进取型</option>
      </select>
      <p class="form-help">风险承受能力用于产品适配校验，不代表客户信用评级。</p>
      <div class="form-actions">
        <button
          type="button"
          class="button secondary"
          :disabled="pending"
          @click="showForm = false"
        >
          取消</button
        ><button
          class="button"
          :disabled="pending"
        >
          {{ pending ? '保存中…' : '保存档案' }}
        </button>
      </div>
    </form></AppModal
  >
</template>
