<script setup lang="ts">
import PageHeader from '../components/PageHeader.vue'
const steps = [
  {
    n: '01',
    title: '先运行，再阅读',
    text: '登录三个角色；创建客户、开户、转账，比较可见按钮与后端权限。',
    file: 'docs/01-快速开始.md',
    tags: ['环境', '角色', '业务体验'],
  },
  {
    n: '02',
    title: '沿一条请求走完整个系统',
    text: '从 TransfersView 到 API 适配器，再到 Controller、应用服务与数据库。',
    file: 'docs/03-一笔转账的全链路.md',
    tags: ['调用链', 'DTO', '事务'],
  },
  {
    n: '03',
    title: '理解设计模式为什么出现',
    text: '比较手续费策略、规则流水线、贷款模板方法和通知观察者解决的不同问题。',
    file: 'docs/04-设计模式实战.md',
    tags: ['策略', '模板方法', '观察者'],
  },
  {
    n: '04',
    title: '把异常路径当成学习入口',
    text: '测试余额不足、并发扣款、重复提交、状态冲突与越权访问。',
    file: 'docs/08-测试与调试.md',
    tags: ['并发', '幂等', '测试'],
  },
  {
    n: '05',
    title: '自己完成一次功能扩展',
    text: '增加转账渠道、贷款规则和产品赎回，先写验收条件再实现。',
    file: 'docs/10-练习与参考思路.md',
    tags: ['扩展', '重构', '工程规范'],
  },
]
</script>
<template>
  <PageHeader
    title="学习地图"
    description="代码可以读很多遍，每一遍都带着一个更具体的问题。"
    eyebrow="LEARNING ROADMAP"
  />
  <section class="learning-hero">
    <p class="eyebrow">A PROJECT THAT EXPLAINS ITSELF</p>
    <h2>从“能够运行”走向“能够解释”。</h2>
    <p>配套教学文档在项目 docs 目录。按下面的顺序学习，并用自己的话解释每个设计选择。</p>
  </section>
  <section class="roadmap">
    <article
      v-for="step in steps"
      :key="step.n"
      class="panel roadmap-card"
    >
      <span class="roadmap-number">{{ step.n }}</span>
      <div>
        <h2>{{ step.title }}</h2>
        <p class="muted">{{ step.text }}</p>
        <div class="button-row">
          <span
            v-for="tag in step.tags"
            :key="tag"
            class="badge"
            >{{ tag }}</span
          >
        </div>
        <code>{{ step.file }}</code>
      </div>
    </article>
  </section>
  <section class="panel">
    <h2>动手之前，先问三个问题</h2>
    <div class="question-grid">
      <div>
        <span>业务规则在哪里？</span>
        <p>控制器做参数边界；领域对象维护规则；应用服务编排用例。</p>
      </div>
      <div>
        <span>失败后会留下什么？</span>
        <p>资金写入和通知都在同一事务中；任何一步失败都应回滚。</p>
      </div>
      <div>
        <span>新需求应该改哪里？</span>
        <p>增加算法优先扩展策略，新增用例优先增加独立应用服务。</p>
      </div>
    </div>
  </section>
</template>
