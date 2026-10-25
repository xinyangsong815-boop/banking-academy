# 教学文档：从业务到工程

这套文档与项目当前源码对应，建议一边运行一边阅读。先掌握 Java 集合、异常、接口、基础 SQL、HTML、JavaScript 和 HTTP，再学习 Spring 与 Vue；不要求先掌握微服务或容器。

也可直接打开 [完整教学手册](完整教学手册.md)，连续阅读全部章节。

## 课程目录

| 顺序 | 章节 | 学完应该能做什么 |
| --- | --- | --- |
| 1 | [快速开始](01-快速开始.md) | 独立启动系统，切换角色，走通业务 |
| 2 | [架构与模块边界](02-架构与模块边界.md) | 解释分层职责与实际依赖关系 |
| 3 | [一笔转账的全链路](03-一笔转账的全链路.md) | 从页面追踪到事务、锁与分录 |
| 4 | [设计模式实战](04-设计模式实战.md) | 判断策略、模板方法和观察者的适用位置 |
| 5 | [注解与代理机制](05-注解与代理机制.md) | 解释校验、授权、事务和审计何时生效 |
| 6 | [数据库与资金一致性](06-数据库与资金一致性.md) | 分析余额快照、流水、锁、幂等与回滚 |
| 7 | [前端工程与组件设计](07-前端工程与组件设计.md) | 拆分页面、组件、Store、请求和用例状态 |
| 8 | [测试与调试](08-测试与调试.md) | 用正常、异常和并发测试验证业务不变量 |
| 9 | [接口参考](09-接口参考.md) | 根据请求与响应结构独立联调 |
| 10 | [练习与参考思路](10-练习与参考思路.md) | 完成有验收标准的扩展，而非只复制代码 |
| 11 | [工程规范与设计决策](11-工程规范与设计决策.md) | 评估方案的收益、成本与实现边界 |
| 12 | [术语与参考资料](12-术语与参考资料.md) | 理解术语并继续阅读官方文档 |
| 13 | [交付验证记录](13-交付验证记录.md) | 区分已验证结果与待验证环境 |

## 建议的四周安排

第一周完成启动、Java record、依赖注入、HTTP、客户与账户模块。每天尝试亲自画出一个请求的调用关系。

第二周阅读转账与记账，重点理解 BigDecimal、事务、行锁、幂等。不要一次跳过整个 PostingService；逐段解释它维护的不变量。

第三周研究贷款算法、产品申购、事件与审计，再学习前端 useResource、useCommand 与 Pinia。尝试把一段业务说明映射到具体文件。

第四周完成至少三个练习：一个小规则扩展、一个设计模式扩展、一个跨模块事务扩展。先写验收标准，再编码和测试。

## 阅读源码的起点

- 第一次读后端：`customer/web/CustomerController.java` → `customer/application/CustomerService.java` → `customer/domain/CustomerRepository.java` → `customer/infrastructure/JdbcCustomerRepository.java`。
- 第二次读后端：`transfer/application/TransferService.java` → `shared/infrastructure/IdempotencyService.java` → `ledger/application/PostingService.java`。
- 第一次读前端：`views/TransfersView.vue` → `api/bank.ts` → `api/http.ts` → `composables/useCommand.ts`。
- 第一次读测试：`MoneyAndPolicyTest` → `BankWorkflowTest` → `ArchitectureTest`。

文档中的源码路径均相对于 `backend/src/main/java/cn/academy/bank/` 或 `frontend/src/`；完整顶层结构见项目 README。

## 如何确认自己真的学懂

关闭示例代码，口述一次转账的处理过程；解释为什么禁用前端按钮不能阻止重复扣款；写出两个并发请求读取相同余额时可能发生的问题；描述增加手续费算法时哪些文件需要改、哪些不需要改。能解释这些，比背诵类名更有价值。
