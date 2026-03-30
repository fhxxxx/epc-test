# Demo 模块结构说明

该模块用于演示脚手架中已集成的各类能力，既用于功能验证，也作为新员工快速上手的学习示例。

---

## 目录结构

```text
demo
├─ capability            // 脚手架内建通用能力（重点）
│  ├─ cache               // Redis 缓存
│  ├─ event               // Spring 事件机制
│  ├─ filetype           // 文件类型校验 
│  ├─ idempotent          // 幂等校验
│  ├─ lock                // 分布式锁（ShedLock）
│  ├─ validation          // 参数校验
│  ├─ locale              // 国际化 / 语言切换
│  ├─ response            // 统一返回体
│  └─ notice              // 消息通知（企微 / 电话 / 邮件 / 异步）
│
├─ integration           // 外部系统 / 中间件集成
│  ├─ elasticsearch       // Elasticsearch 操作示例
│  ├─ excel               // EasyExcel
│  ├─ doccenter           // 文档中心
│  ├─ log                 // 操作日志
│  ├─ ip                  // IP校验
│  ├─ plt                 // 数据湖
│  ├─ retry               // 重试机制
│  ├─ rest                // HTTP / REST 调用
│  ├─ sensitive           // 敏感信息过滤
│  └─ tree                // hutool的TreeUtil示例
│
├─ support               // Demo 公用支撑代码（非重点）
│  └─ model               // 示例实体、Mapper、Service
│
├─ scenario              // 多能力组合场景（可选，但很加分）
│  └─ service             // 原 ServiceDemo
│
└─ README.md              // Demo 使用说明
