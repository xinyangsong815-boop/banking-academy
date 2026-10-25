# Banking Academy · 银行业务学堂

一个可运行、前后端分离、以业务理解为主线的综合教学项目。后端采用 Java 17、Spring Boot 3.5.14、Spring Security、Spring JDBC、Flyway；前端采用 Vue 3、TypeScript、Pinia、Vue Router、Vite。默认使用持久化 H2，无需先安装数据库。

这里的金额、客户、贷款和产品均为虚构教学数据。项目提供完整的业务用例和异常路径，适合从 CRUD 过渡到事务、并发、权限与架构设计的学习者。

## 从这里开始

1. 阅读 [快速开始](docs/01-快速开始.md)，运行项目。
2. 打开 [教学文档总目录](docs/README.md)，按章节学习。
3. 完成一次转账，再阅读 [一笔转账的全链路](docs/03-一笔转账的全链路.md)。

Windows 启动：在此目录打开 PowerShell，运行：

```powershell
.\scripts\Start-Learning.ps1
```

首次运行需要联网安装依赖；如 PowerShell 的既有执行策略阻止脚本，可按快速开始中的两个终端方式启动。脚本不会修改机器的执行策略。

访问 [本地学习平台](http://127.0.0.1:5173)。演示账号 `admin` / `teller` / `auditor`，统一密码 `Learn@123456`。默认 demo 配置才会创建演示账号。

## 已实现的业务

| 模块 | 可以实际操作的功能 | 核心学习点 |
| --- | --- | --- |
| 身份权限 | 登录、退出、会话过期、三种角色 | 不透明令牌、哈希存储、方法权限 |
| 客户 | 新建、编辑、姓名/手机号搜索、分页 | DTO 校验、仓储端口、参数化 SQL |
| 账户 | 开户、冻结、解冻、零余额销户、流水 | 状态机、不可变对象、行锁 |
| 转账 | 标准/即时渠道、手续费、限额、回执 | 策略、规则流水线、幂等、事务 |
| 记账 | 期初余额、转账、产品申购、贷款放款 | 平衡分录、余额快照、统一记账入口 |
| 贷款 | 申请、双人复核、批准/拒绝、放款 | 状态约束、模板方法、金额尾差 |
| 产品 | 产品展示、风险适配、额度校验、申购记录 | 跨模块用例、库存式额度锁定 |
| 通知 | 当前员工通知、标记已读 | 同步领域事件、数据归属检查 |
| 审计 | 成功操作记录、人员、请求追踪号 | 自定义注解、AOP、切面顺序 |
| 总览与学习 | 统计、七日趋势、最近转账、学习地图 | 查询投影、组件化、状态管理 |

贷款实际还款、逾期处理、产品赎回/兑付、用户管理、多网点数据隔离、跨行清算等不在当前实现范围，作为进阶练习明确列在文档中。

## 目录

```text
banking-academy/
├── backend/
│   ├── src/main/java/cn/academy/bank/
│   │   ├── identity / customer / account
│   │   ├── transfer / ledger / loan / product
│   │   ├── notification / audit / report
│   │   ├── shared                  # 通用协议、异常、金额、幂等
│   │   └── config                  # 框架装配、安全、教学数据
│   ├── src/main/resources/db/migration/
│   ├── src/test/                   # 领域、集成与架构测试
│   └── http/learning.http          # 可执行请求示例
├── frontend/src/
│   ├── api / types / stores / composables
│   ├── components / layouts / router / views
│   └── styles.css
├── docs/                          # 中文课程、接口与设计决策
├── scripts/                       # 启动、停止、验证
├── compose.yml                    # 可选 PostgreSQL 容器化练习
└── .github/workflows/verify.yml
```

采用按业务模块组织的单体后端，前端独立部署。模块共享进程和数据库，不需要先掌握微服务基础设施。重要业务对象使用领域模型与端口；简单查询直接使用 JDBC 投影。详细取舍见 [架构与模块边界](docs/02-架构与模块边界.md)。

## 验证

```powershell
.\scripts\Verify.ps1
```

后端使用 JUnit、MockMvc、ArchUnit；前端使用 Vitest、Vue Test Utils，构建时执行 TypeScript 检查。验证范围与尚未验证的环境见 [交付验证记录](docs/13-交付验证记录.md)。

停止启动脚本管理的服务：

```powershell
.\scripts\Stop-Learning.ps1
```

## 学习方法

每学一个模块，都回答：规则由谁维护？事务在哪里开始和结束？并发时锁住了什么？失败会留下什么数据？增加需求会影响哪些类？能否用一个测试证明判断？

官方资料入口见 [术语与参考资料](docs/12-术语与参考资料.md)。
