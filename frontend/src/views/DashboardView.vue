<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { bankApi } from '../api/bank'
import { useResource } from '../composables/useResource'
import { useAuthStore } from '../stores/auth'
import { money, dateTime } from '../utils/format'
import PageHeader from '../components/PageHeader.vue'
const auth = useAuthStore()
const { data, loading, error, reload } = useResource(bankApi.dashboard)
const recent = useResource(() => bankApi.transfers())
const maximum = computed(() =>
  Math.max(...(data.value?.dailyVolumes.map((day) => day.count) ?? []), 1),
)
onMounted(() => {
  void reload()
  void recent.reload()
})
</script>
<template>
  <PageHeader
    title="每一笔业务，都清晰可见。"
    description="从这里了解客户、资金与待办，开始今天的业务学习。"
    eyebrow="OVERVIEW / 业务总览"
    ><button
      class="button secondary"
      :disabled="loading"
      @click="reload"
    >
      ↻ 刷新总览
    </button></PageHeader
  >
  <div
    v-if="error"
    class="error-panel"
  >
    {{ error }}
  </div>
  <section class="welcome-banner">
    <div>
      <span class="banner-label">YOUR LEARNING WORKSPACE</span>
      <h2>{{ auth.user?.displayName }}，欢迎回来</h2>
      <p>先完成一笔转账，再沿着业务号查看账户流水与操作审计。</p>
      <RouterLink
        to="/transfers"
        class="button cream"
        >进入转账工作台 <span>↗</span></RouterLink
      >
    </div>
    <div
      class="banner-art"
      aria-hidden="true"
    >
      <div class="art-orbit orbit-one"></div>
      <div class="art-orbit orbit-two"></div>
      <div class="art-card">
        <span>BANKING ACADEMY</span><strong>学以致用</strong>
        <div>◈ <span>业务 · 设计 · 实践</span></div>
      </div>
      <span class="art-spark">✦</span>
    </div>
  </section>
  <section
    class="metric-grid"
    aria-label="核心指标"
  >
    <article class="metric-card">
      <span class="metric-icon">♧</span>
      <p>客户总数</p>
      <strong>{{ data?.customers ?? '—' }}<small>位</small></strong
      ><span class="metric-caption">完整客户档案</span>
    </article>
    <article class="metric-card">
      <span class="metric-icon">▣</span>
      <p>客户账户余额</p>
      <strong class="metric-money">¥ {{ money(data?.customerBalance) }}</strong
      ><span class="metric-caption">{{ data?.accounts ?? 0 }} 个客户账户</span>
    </article>
    <article class="metric-card">
      <span class="metric-icon">⇄</span>
      <p>累计转账金额</p>
      <strong class="metric-money">¥ {{ money(data?.transferredAmount) }}</strong
      ><span class="metric-caption">已完成 {{ data?.transfers ?? 0 }} 笔转账</span>
    </article>
    <article class="metric-card accent">
      <span class="metric-icon">◇</span>
      <p>待审批贷款</p>
      <strong>{{ data?.pendingLoans ?? '—' }}<small>笔</small></strong
      ><RouterLink
        to="/loans"
        class="metric-caption"
        >进入贷款工作台 →</RouterLink
      >
    </article>
  </section>
  <div class="dashboard-grid">
    <section class="panel">
      <div class="panel-heading">
        <div>
          <h2>近七日转账活跃度</h2>
          <p class="muted">按北京时间统计 · 单位：笔</p>
        </div>
        <span class="badge active">最近 7 天</span>
      </div>
      <div
        class="bar-chart"
        role="img"
        aria-label="近七日每日转账笔数"
      >
        <div
          v-for="day in data?.dailyVolumes"
          :key="day.date"
          class="chart-column"
        >
          <span>{{ day.count }}</span>
          <div class="bar-track">
            <div
              class="bar"
              :style="{ height: `${Math.max((day.count / maximum) * 100, 2)}%` }"
            ></div>
          </div>
          <small>{{ day.date.slice(5).replace('-', '/') }}</small>
        </div>
      </div>
      <p
        v-if="data?.transfers === 0"
        class="chart-note"
      >
        完成第一笔转账后，这里会出现真实业务数据。
      </p>
    </section>
    <section class="panel learning-panel">
      <p class="eyebrow">TODAY'S PRACTICE</p>
      <h2>今天，从一条请求开始</h2>
      <ol>
        <li>
          <span>01</span>
          <div>
            <strong>体验业务</strong>
            <p>选择账户，完成一笔标准转账。</p>
          </div>
        </li>
        <li>
          <span>02</span>
          <div>
            <strong>追踪实现</strong>
            <p>阅读 TransferService 与 PostingService。</p>
          </div>
        </li>
        <li>
          <span>03</span>
          <div>
            <strong>验证边界</strong>
            <p>重复提交、冻结账户、观察余额。</p>
          </div>
        </li>
      </ol>
      <RouterLink
        to="/learning"
        class="text-button"
        >查看完整学习路径 ↗</RouterLink
      >
    </section>
  </div>
  <section class="panel">
    <div class="panel-heading">
      <div>
        <h2>最近转账</h2>
        <p class="muted">业务结果会同步体现在账户流水中</p>
      </div>
      <RouterLink
        to="/transfers"
        class="text-button"
        >查看全部 →</RouterLink
      >
    </div>
    <div
      v-if="recent.error.value"
      class="error-panel"
    >
      {{ recent.error.value }}
      <button
        class="text-button"
        @click="recent.reload"
      >
        重试
      </button>
    </div>
    <div
      v-if="!recent.data.value?.items.length"
      class="empty-state"
    >
      {{ recent.loading.value ? '正在读取…' : '还没有转账记录，开始你的第一笔业务实践。' }}
    </div>
    <div
      v-for="item in recent.data.value?.items.slice(0, 5)"
      :key="item.id"
      class="transaction-row"
    >
      <span class="transaction-icon">⇄</span>
      <div>
        <strong>{{ item.sourceId }} → {{ item.targetId }}</strong
        ><small
          >{{ dateTime(item.createdAt) }} ·
          {{ item.channel === 'STANDARD' ? '标准转账' : '即时转账' }}</small
        >
      </div>
      <strong class="amount">¥ {{ money(item.amount) }}</strong
      ><span class="badge active">已完成</span>
    </div>
  </section>
</template>
