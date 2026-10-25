<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { bankApi } from '../api/bank'
import { useResource } from '../composables/useResource'
import { useCommand } from '../composables/useCommand'
import { useAuthStore } from '../stores/auth'
import { money, dateTime } from '../utils/format'
import type { Account, Entry, Page } from '../types/models'
import PageHeader from '../components/PageHeader.vue'
import DataTable from '../components/DataTable.vue'
import Pagination from '../components/Pagination.vue'
import StatusBadge from '../components/StatusBadge.vue'
import AppModal from '../components/AppModal.vue'
import CustomerSelect from '../components/CustomerSelect.vue'
const auth = useAuthStore()
const page = ref(1)
const { data, loading, error, reload } = useResource(() => bankApi.accounts(page.value))
const { pending, run } = useCommand()
const showOpen = ref(false)
const form = reactive({ customerId: 0, name: '' })
const selected = ref<Account>()
const entries = ref<Page<Entry>>()
const entryPage = ref(1)
const confirmState = ref<{ account: Account; status: Account['status'] }>()
watch(page, reload)
onMounted(reload)
async function openAccount() {
  if (!form.customerId) return
  const result = await run(() => bankApi.openAccount(form.customerId, form.name), '账户已开立')
  if (result) {
    showOpen.value = false
    form.name = ''
    await reload()
  }
}
async function viewEntries(account: Account, nextPage = 1) {
  const result = await run(() => bankApi.entries(account.id, nextPage))
  if (result) {
    entries.value = result
    entryPage.value = nextPage
    selected.value = account
  }
}
async function changeStatus() {
  if (!confirmState.value) return
  const { account, status } = confirmState.value
  const result = await run(() => bankApi.accountStatus(account.id, status), '账户状态已更新')
  if (result) {
    confirmState.value = undefined
    await reload()
  }
}
const columns = [
  { key: 'name', label: '账户名称' },
  { key: 'accountNo', label: '账户编号' },
  { key: 'balance', label: '可用余额' },
  { key: 'status', label: '账户状态' },
  { key: 'actions', label: '操作' },
]
const entryColumns = [
  { key: 'description', label: '业务摘要' },
  { key: 'amount', label: '变动金额' },
  { key: 'balanceAfter', label: '变动后余额' },
  { key: 'createdAt', label: '时间' },
]
</script>
<template>
  <PageHeader
    title="账户中心"
    description="管理账户生命周期，按资金流水理解每一次余额变化。"
    ><button
      v-if="auth.can('account:write')"
      class="button"
      @click="showOpen = true"
    >
      ＋ 开立账户
    </button></PageHeader
  >
  <div class="info-strip">新账户余额为零。演示账户含期初记账资金，可通过转账向新账户转入余额。</div>
  <section class="panel">
    <DataTable
      :rows="data?.items ?? []"
      :columns="columns"
      :loading="loading"
      :error="error"
      @retry="reload"
      ><template #name="{ row }"
        ><strong>{{ row.name }}</strong
        ><small class="cell-sub">客户 #{{ row.customerId }}</small></template
      ><template #accountNo="{ row }"
        ><code>{{ row.accountNo }}</code></template
      ><template #balance="{ row }"
        ><strong class="amount">¥ {{ money(row.balance) }}</strong></template
      ><template #status="{ row }"><StatusBadge :value="row.status" /></template
      ><template #actions="{ row }"
        ><div class="button-row">
          <button
            class="text-button"
            :disabled="pending"
            @click="viewEntries(row)"
          >
            流水</button
          ><template v-if="auth.can('account:write')"
            ><button
              v-if="row.status !== 'CLOSED'"
              class="text-button"
              @click="
                confirmState = {
                  account: row,
                  status: row.status === 'FROZEN' ? 'ACTIVE' : 'FROZEN',
                }
              "
            >
              {{ row.status === 'FROZEN' ? '解冻' : '冻结' }}</button
            ><button
              v-if="row.status === 'ACTIVE'"
              class="text-button danger-text"
              @click="confirmState = { account: row, status: 'CLOSED' }"
            >
              销户
            </button></template
          >
        </div></template
      ></DataTable
    ><Pagination
      :page="page"
      :total="data?.total ?? 0"
      :loading="loading"
      @change="page = $event"
    />
  </section>
  <AppModal
    v-if="showOpen"
    title="开立客户账户"
    @close="!pending && (showOpen = false)"
    ><form
      class="form-grid"
      @submit.prevent="openAccount"
    >
      <label for="account-customer">账户归属客户</label
      ><CustomerSelect
        id="account-customer"
        v-model="form.customerId"
      /><label for="account-name">账户名称</label
      ><input
        id="account-name"
        v-model="form.name"
        required
        maxlength="60"
      />
      <div class="form-actions">
        <button
          class="button"
          :disabled="pending || !form.customerId"
        >
          确认开户
        </button>
      </div>
    </form></AppModal
  >
  <AppModal
    v-if="confirmState"
    title="确认账户状态变更"
    @close="!pending && (confirmState = undefined)"
    ><p>账户：{{ confirmState.account.name }}</p>
    <p>目标状态：<StatusBadge :value="confirmState.status" /></p>
    <p class="muted">冻结账户无法收付款；销户需要余额为零，完成后不能恢复。</p>
    <div class="form-actions">
      <button
        class="button secondary"
        :disabled="pending"
        @click="confirmState = undefined"
      >
        取消</button
      ><button
        class="button"
        :disabled="pending"
        @click="changeStatus"
      >
        确认变更
      </button>
    </div></AppModal
  >
  <AppModal
    v-if="selected"
    :title="`${selected.name} · 账户流水`"
    @close="selected = undefined"
    ><DataTable
      :rows="entries?.items ?? []"
      :columns="entryColumns"
      ><template #description="{ row }"
        >{{ row.description
        }}<small class="cell-sub">{{ row.bookingId.slice(0, 8) }}</small></template
      ><template #amount="{ row }"
        ><span
          class="amount"
          :class="row.amount.startsWith('-') ? 'danger-text' : 'positive-text'"
          >{{ row.amount.startsWith('-') ? '' : '+' }}{{ money(row.amount) }}</span
        ></template
      ><template #balanceAfter="{ row }">{{ money(row.balanceAfter) }}</template
      ><template #createdAt="{ row }">{{ dateTime(row.createdAt) }}</template></DataTable
    ><Pagination
      :page="entryPage"
      :total="entries?.total ?? 0"
      :loading="pending"
      @change="viewEntries(selected!, $event)"
  /></AppModal>
</template>
