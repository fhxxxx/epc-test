# EPC进项税报表系统 - 详细设计文档

> 版本：v1.2  
> 日期：2026-04-08  
> 状态：评审中

---

## 一、项目概述

### 1.1 项目背景

财务团队每月需要手工整理大量税务数据并生成台账Excel。该项目旨在构建一个Web系统，通过用户上传文件+数据湖API拉取，自动计算并生成包含多Sheet页的税务台账，减少人工操作、降低出错率。

### 1.2 核心目标

| 目标 | 说明 |
|------|------|
| 文件管理 | 按公司+月份维度管理上传文件和数据湖拉取文件 |
| 自动生成 | 基于上传数据和配置规则，自动生成税务台账Excel |
| 历史累积 | 台账支持月份间数据依赖，支持无限期历史留存 |
| 权限控制 | 超级管理员 + 公司用户映射（公司维度） |

**V1上线策略补充**：
- 交付顺序采用“后端核心能力优先、前端并行跟进、最后统一联调”。
- 首版重点打通主链路：`上传/拉取 -> 校验 -> 生成 -> 下载`。
- 生成失败采用“部分成功可见”策略：阶段产物可下载，但未完成关键批次时不发布最终台账。

### 1.3 技术栈

| 层级 | 技术选型 |
|------|---------|
| 后端框架 | Java Spring Boot 3.5.9 |
| 数据库 | MySQL |
| 文件存储 | Azure Blob Storage（现成实现，本地仅存path） |
| 认证 | Okta（现成实现） |
| 前端 | React + Ant Design + Vite |
| Excel操作 | Aspose.Cells |

### 1.4 业务规模

| 维度 | 预估值 |
|------|--------|
| 公司数量 | 10~50家 |
| 用户数量 | 1~5人 |
| 台账Sheet页 | 最多26个 |
| 数据湖科目拆分 | 5类（收入/销项/进项/所得税/其他） |

### 1.5 最近决策同步（2026-04-08）

- 公司主数据统一：`company_code` 只以 `t_tax_company_code_config` 为唯一来源，`t_tax_company` 退出模型。
- 数据库迁移：新增迁移脚本删除 `t_tax_company`（`DROP TABLE IF EXISTS t_tax_company`）。
- 前端交互约束：除“公司代码配置”页外，所有弹窗中的“公司代码”字段均改为下拉选择，数据实时查询 `t_tax_company_code_config`，禁止手工输入。

---

## 二、系统架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                      前端（Browser）                      │
│  ┌──────┐  ┌──────────┐  ┌───────────┐  ┌───────────┐  │
│  │首页   │  │公司详情页 │  │月份详情页  │  │配置管理页  │  │
│  └──────┘  └──────────┘  └───────────┘  └───────────┘  │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP/REST API
┌────────────────────────┴────────────────────────────────┐
│              Spring Boot 3.5.9 后端                       │
│  ┌──────────┐  ┌──────────────┐  ┌─────────────────┐    │
│  │Auth模块   │  │文件管理模块    │  │台账生成模块      │    │
│  │(Okta)    │  │(上传/下载/    │  │(Aspose模板填充/公式重算/Excel生成/      │    │
│  │          │  │ Blob存储)     │  │ 数据湖拉取)      │    │
│  └──────────┘  └──────────────┘  └─────────────────┘    │
│  ┌──────────┐  ┌──────────────┐  ┌─────────────────┐    │
│  │配置管理   │  │数据湖集成模块  │  │权限管理模块      │    │
│  │(5张配置表) │  │(API调用+     │  │(超级管理员+     │    │
│  │          │  │ 数据拆分)     │  │ 公司用户映射)     │    │
│  └──────────┘  └──────────────┘  └─────────────────┘    │
└────────┬───────────────┬────────────────┬───────────────┘
         │               │                │
    ┌────┴────┐   ┌─────┴─────┐   ┌─────┴─────┐
    │ MySQL   │   │Azure Blob  │   │ 数据湖API  │
    │ (数据库)  │   │(文件存储)   │   │(外部API)   │
    └─────────┘   └───────────┘   └───────────┘
```

### 2.2 模块划分

| 模块 | 职责 | 核心类 |
|------|------|--------|
| `auth` | Okta认证、用户信息管理 | `AuthController`, `UserService` |
| `company-code` | 公司代码主数据维护（统一来源） | `TaxConfigController`, `TaxConfigService` |
| `file` | 文件上传、下载、Azure Blob管理 | `FileController`, `FileService`, `BlobStorageService` |
| `datalake` | 数据湖API调用、科目拆分、Aspose模板填充/公式重算/Excel生成 | `DataLakeController`, `DataLakeService`, `AccountSplitService` |
| `ledger` | 台账生成核心逻辑（run驱动，含重试/续跑/失效） | `LedgerController`, `LedgerService` |
| `config` | 5张配置表的CRUD管理 | `ConfigController`, `ConfigService` |
| `permission` | 权限管理（超级管理员+公司用户映射） | `PermissionController`, `PermissionService` |

### 2.3 目录结构

```
src/main/java/com/xxx/taxledger/
├── controller/
│   ├── AuthController.java
│   ├── FileController.java
│   ├── DataLakeController.java
│   ├── LedgerController.java
│   ├── ConfigController.java
│   └── PermissionController.java
├── service/
│   ├── auth/
│   │   └── AuthService.java
│   ├── file/
│   │   ├── FileService.java
│   │   └── BlobStorageService.java
│   ├── datalake/
│   │   ├── DataLakeService.java
│   │   └── AccountSplitService.java
│   ├── ledger/
│   │   ├── LedgerService.java
│   │   ├── generator/                    # 各Sheet页生成器
│   │   │   ├── LedgerGenerator.java      # 生成器接口
│   │   │   ├── BsPlGenerator.java        # BS表/PL表拼接
│   │   │   ├── VatChangeGenerator.java   # 增值税变动表
│   │   │   ├── UnbilledMonitorGenerator.java  # 未开票数监控
│   │   │   ├── ProjectCumulativeGenerator.java # 项目累计申报
│   │   │   ├── ProjectPaymentGenerator.java   # 项目累计缴纳
│   │   │   ├── TaxSummaryGenerator.java       # 累计税金汇总表
│   │   │   ├── VatOutputGenerator.java        # 增值税表-累计销项
│   │   │   ├── AccountTaxDiffGenerator.java   # 账税差异监控
│   │   │   ├── StampTaxGenerator.java         # 印花税明细
│   │   │   ├── SummaryGenerator.java          # Summary表
│   │   │   ├── RefSummaryGenerator.java       # Ref Summary
│   │   │   └── PreBilledGenerator.java        # 预开票收入计提冲回统计
│   │   └── assembler/
│   │       └── LedgerAssembler.java     # 台账组装器（合并所有Sheet）
│   ├── config/
│   │   └── ConfigService.java
│   └── permission/
│       └── PermissionService.java
├── entity/
│   ├── FileRecord.java
│   ├── LedgerRecord.java
│   ├── CompanyCodeConfig.java
│   ├── TaxCategoryConfig.java
│   ├── ProjectConfig.java
│   ├── VatBasicItemConfig.java
│   ├── VatSpecialItemConfig.java
│   └── UserPermission.java
├── dto/
│   ├── AccountingDocumentDTO.java     # 数据湖API返回对象
│   ├── DataLakeQueryDTO.java          # 数据湖查询参数
│   ├── LedgerGenerateDTO.java         # 台账生成请求
│   ├── FileUploadDTO.java             # 文件上传请求
│   └── ...
├── enums/
│   ├── FileCategoryEnum.java          # 文件类别枚举
│   ├── FileSourceEnum.java            # 文件来源枚举（上传/数据湖）
│   ├── AccountTypeEnum.java           # 科目分类枚举
│   └── PermissionScopeEnum.java       # 权限范围枚举（平台/公司）
├── repository/
│   ├── FileRecordRepository.java
│   ├── LedgerRecordRepository.java
│   ├── CompanyCodeConfigRepository.java
│   ├── TaxCategoryConfigRepository.java
│   ├── ProjectConfigRepository.java
│   ├── VatBasicItemConfigRepository.java
│   ├── VatSpecialItemConfigRepository.java
│   └── UserPermissionRepository.java
└── exception/
    ├── LedgerException.java
    ├── DataLakeException.java
    └── FileNotFoundException.java
```

---

## 三、数据库设计

### 3.1 ER关系

```
CompanyCodeConfig (1) ──< FileRecord (N)
CompanyCodeConfig (1) ──< LedgerRecord (N)
CompanyCodeConfig (1) ──< UserPermission (N)

TaxCategoryConfig (N)
ProjectConfig (N)
VatBasicItemConfig (N)
VatSpecialItemConfig (N)
```

### 3.2 表结构

#### 3.2.1 公司代码主数据表 `t_tax_company_code_config`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| company_code | VARCHAR(20) | 公司代码 | UNIQUE, NOT NULL |
| company_name | VARCHAR(200) | 公司名称 | NOT NULL |
| finance_bp_ad | VARCHAR(100) | 财务BP AD号 | |
| finance_bp_name | VARCHAR(100) | 财务BP姓名 | |
| finance_bp_email | VARCHAR(200) | 财务BP邮箱 | |
| created_by | VARCHAR(100) | 创建人 | |
| created_at | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| updated_by | VARCHAR(100) | 更新人 | |
| updated_at | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE |
| deleted | TINYINT | 逻辑删除 | DEFAULT 0 |

#### 3.2.2 文件记录表 `t_file_record`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| company_code | VARCHAR(20) | 公司代码 | NOT NULL, IDX |
| year_month | VARCHAR(7) | 月份(格式:2026-01) | NOT NULL, IDX |
| file_name | VARCHAR(300) | 原始文件名 | NOT NULL |
| file_category | VARCHAR(50) | 文件类别 | NOT NULL |
| file_source | VARCHAR(20) | 文件来源 | NOT NULL |
| blob_path | VARCHAR(500) | Azure Blob存储路径 | NOT NULL |
| file_size | BIGINT | 文件大小(字节) | |
| upload_user | VARCHAR(100) | 上传人 | |
| created_at | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| deleted | TINYINT | 逻辑删除 | DEFAULT 0 |

**file_category 枚举值**：

| 值 | 说明 | 来源 |
|----|------|------|
| BS | 资产负债表 | 用户上传 |
| PL | 利润表 | 用户上传 |
| BS_APPENDIX_TAX_PAYABLE | BS附表-应交税费科目余额表 | 用户上传 |
| PL_APPENDIX_2320 | PL附表(2320/2355公司) | 用户上传 |
| PL_APPENDIX_PROJECT | PL附表(项目公司) | 用户上传 |
| STAMP_TAX | 印花税明细 | 用户上传 |
| VAT_OUTPUT | 增值税销项 | 用户上传 |
| VAT_INPUT_CERT | 增值税进项认证清单 | 用户上传 |
| CUMULATIVE_PROJECT_TAX | 累计项目税收明细表 | 用户上传 |
| DL_INCOME | 收入明细 | 数据湖 |
| DL_OUTPUT | 销项明细 | 数据湖 |
| DL_INPUT | 进项明细 | 数据湖 |
| DL_INCOME_TAX | 所得税明细 | 数据湖 |
| DL_OTHER | 其他科目明细 | 数据湖 |


**唯一约束**：`UNIQUE(company_code, year_month, file_category, file_source)`，同键重复上传时覆盖旧记录（逻辑删除旧记录）。

#### 3.2.3 台账记录表 `t_ledger_record`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| company_code | VARCHAR(20) | 公司代码 | NOT NULL, IDX |
| year_month | VARCHAR(7) | 台账月份 | NOT NULL, IDX |
| ledger_name | VARCHAR(300) | 台账文件名 | NOT NULL |
| blob_path | VARCHAR(500) | Azure Blob存储路径 | NOT NULL |
| generate_user | VARCHAR(100) | 生成人 | |
| generate_status | VARCHAR(20) | 生成状态 | DEFAULT PENDING |
| status_msg | VARCHAR(500) | 状态描述/错误信息 | |
| generated_at | DATETIME | 生成完成时间 | |
| created_at | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| deleted | TINYINT | 逻辑删除 | DEFAULT 0 |

**generate_status 枚举值**：`PENDING`(生成中) / `SUCCESS`(成功) / `FAILED`(失败)

**唯一约束**：`UNIQUE(company_code, year_month)`，每个公司每月只能有一条台账记录（重新生成则覆盖）

#### 3.2.4 公司主数据来源约束（实现补充）

- 历史 `t_tax_company` 已退役，主数据不再双轨维护。
- 所有 `company_code` 相关查询、校验、下拉选项均以 `t_tax_company_code_config` 为唯一来源。
- 删除公司代码配置时采用软删，避免对历史文件、台账、权限记录产生物理级联影响。

#### 3.2.5 税目配置表 `t_tax_category_config`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| seq_no | VARCHAR(20) | 序号（如"1"、"1.1"） | NOT NULL |
| company_code | VARCHAR(20) | 公司代码（可为空，表示通用） | IDX |
| tax_type | VARCHAR(50) | 税种 | NOT NULL |
| tax_category | VARCHAR(50) | 税目 | |
| tax_basis | VARCHAR(200) | 计税依据 | |
| collection_ratio | DECIMAL(5,2) | 征收比例 | |
| tax_rate | DECIMAL(10,6) | 税率 | |
| account_subject | VARCHAR(200) | 会计科目 | |
| created_at | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| updated_at | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE |

#### 3.2.6 项目配置表 `t_project_config`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| company_code | VARCHAR(20) | 公司代码 | NOT NULL |
| tax_type | VARCHAR(50) | 税种 | NOT NULL |
| tax_category | VARCHAR(50) | 税目 | |
| project_name | VARCHAR(200) | 项目名称 | |
| preferential_period | VARCHAR(100) | 所属优惠期 | NOT NULL |
| created_at | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| updated_at | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE |

#### 3.2.7 增值税变动表基础条目配置表 `t_vat_basic_item_config`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| item_seq | INT | 条目序号 | NOT NULL |
| company_code | VARCHAR(20) | 公司代码（可为空，表示通用） | IDX |
| basic_item | VARCHAR(200) | 基础条目 | NOT NULL |
| is_split | VARCHAR(1) | 是否拆分(Y/N) | NOT NULL |
| is_display | VARCHAR(1) | 是否显示(Y/N) | |
| created_at | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| updated_at | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE |

#### 3.2.8 睿景景程增值税变动表特殊条目配置表 `t_vat_special_item_config`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| item_seq | INT | 条目序号 | NOT NULL |
| company_code | VARCHAR(20) | 公司代码（必填，多选） | NOT NULL |
| special_item | VARCHAR(200) | 特殊条目 | NOT NULL |
| is_display | VARCHAR(1) | 是否显示(Y/N) | NOT NULL |
| created_at | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| updated_at | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE |

#### 3.2.9 用户权限表 `t_user_permission`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| user_id | VARCHAR(100) | 用户ID（Okta userId或工号） | NOT NULL, IDX |
| user_name | VARCHAR(100) | 用户姓名 | NOT NULL |
| employee_id | VARCHAR(50) | 工号 | NOT NULL, UNI |
| company_code | VARCHAR(20) | 公司代码 | NOT NULL, IDX |
| granted_by | VARCHAR(100) | 授权人工号 | |
| created_at | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |

**模型说明**：
- 本表仅保存“用户-公司”访问映射，不再区分 `permission_level`。
- 表中存在一条记录，表示该用户可访问该公司的业务页面（不含5个配置页），并可给他人分配该公司的访问权限。
- 超级管理员不落表，统一由 `application.yml` 配置维护。

**约束**：
- `(employee_id, company_code)` 联合唯一，同一用户对同一公司只有一条权限记录
- `company_code` 必填
- `UNIQUE(user_id, company_code)`

---

#### 3.2.10 台账运行实例表 `t_ledger_run`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| ledger_id | BIGINT | 关联主记录ID（t_ledger_record.id） | NOT NULL, IDX |
| run_no | INT | 同一ledger下的运行序号 | NOT NULL |
| trigger_type | VARCHAR(20) | 触发类型（MANUAL/RETRY/RESUME） | NOT NULL |
| mode_snapshot | VARCHAR(20) | 本次运行模式（AUTO/GATED） | NOT NULL |
| status | VARCHAR(20) | 运行状态（PENDING/RUNNING/PAUSED/SUCCESS/FAILED/CANCELED/INVALIDATED） | NOT NULL, IDX |
| current_batch | INT | 当前批次（1~4） | |
| input_fingerprint | VARCHAR(128) | 输入文件与配置摘要哈希 | NOT NULL |
| error_code | VARCHAR(100) | 失败错误码 | |
| error_msg | VARCHAR(1000) | 失败错误信息 | |
| started_at | DATETIME | 运行开始时间 | |
| ended_at | DATETIME | 运行结束时间 | |
| created_at | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| updated_at | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE |

**约束建议**：`UNIQUE(ledger_id, run_no)`，同一ledger按运行序号递增。

#### 3.2.11 台账运行阶段表 `t_ledger_run_stage`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| run_id | BIGINT | 关联运行实例ID | NOT NULL, IDX |
| batch_no | INT | 批次号（1~4） | NOT NULL |
| status | VARCHAR(20) | 阶段状态（PENDING/RUNNING/SUCCESS/FAILED/CONFIRMED/SKIPPED/INVALIDATED） | NOT NULL, IDX |
| sheet_count_total | INT | 该批应生成sheet数 | |
| sheet_count_success | INT | 该批成功sheet数 | |
| depends_on | VARCHAR(100) | 依赖批次描述（如1,2） | |
| error_msg | VARCHAR(1000) | 阶段失败信息 | |
| confirm_user | VARCHAR(100) | 人工确认人（GATED模式） | |
| confirm_time | DATETIME | 人工确认时间 | |
| started_at | DATETIME | 阶段开始时间 | |
| ended_at | DATETIME | 阶段结束时间 | |
| created_at | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| updated_at | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE |

**约束建议**：`UNIQUE(run_id, batch_no)`。

#### 3.2.12 台账运行产物表 `t_ledger_run_artifact`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| run_id | BIGINT | 关联运行实例ID | NOT NULL, IDX |
| batch_no | INT | 产物所属批次（1~4，最终文件可记为4） | NOT NULL |
| artifact_type | VARCHAR(20) | 产物类型（INTERMEDIATE/FINAL/DEBUG） | NOT NULL |
| file_name | VARCHAR(300) | 文件名 | NOT NULL |
| blob_path | VARCHAR(500) | Blob路径 | NOT NULL |
| file_size | BIGINT | 文件大小（字节） | |
| checksum | VARCHAR(128) | 文件校验值（建议SHA-256） | |
| is_latest | TINYINT | 是否该批最新快照 | DEFAULT 1 |
| created_at | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |

**快照保留策略**：中间快照与最终快照永久保留（不做TTL清理）。

**说明**：`t_ledger_record` 作为“主记录（ledger）”，仍保持 `UNIQUE(company_code, year_month)`；`t_ledger_run*` 用于记录每次运行、分批状态和快照。

### 3.3 软删与唯一约束并存策略（实现补充）

为避免“逻辑删除后重复创建同业务键”引发唯一键冲突，软删表建议采用“虚拟生成列 + 唯一索引”：

- 原则：唯一性仅约束 `is_deleted=0` 的有效数据。
- 方案：新增虚拟列 `uk_xxx_active = CASE WHEN is_deleted=0 THEN business_key ELSE NULL END`，并对虚拟列建唯一索引。
- 适用：公司代码配置、文件唯一键、台账主记录唯一键、权限唯一键等软删表。
## 四、API接口设计

### 4.1 接口规范

| 规范项 | 说明 |
|--------|------|
| 基础路径 | `/api/v1/tax-ledger` |
| 认证方式 | Okta Bearer Token（已有实现） |
| 响应格式 | 统一JSON `{code, msg, data}` |
| 分页 | `page`, `size` 参数，返回 `{list, total, page, size}` |

**响应处理补充约束**：
- 业务异常可能返回 HTTP 200，但 `code != 0`。
- 前端必须按 `code` 判定成功失败，`code != 0` 时展示 `msg`，不得提示“操作成功”。

### 4.2 接口清单

#### 4.2.1 认证模块

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/auth/current-user` | 获取当前登录用户信息 | 登录即可 |
| GET | `/auth/permissions` | 获取当前用户权限列表 | 登录即可 |

#### 4.2.2 公司代码主数据模块

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/tax-ledger/config/company-code` | 获取公司代码列表 | 所有已认证用户 |
| POST | `/tax-ledger/config/company-code` | 新增/更新公司代码 | SUPER_ADMIN |
| DELETE | `/tax-ledger/config/company-code/{id}` | 删除公司代码 | SUPER_ADMIN |

**设计约束补充**：
- 不再提供独立 `/companies` 写接口，避免与配置表双写导致主数据不一致。
- 业务页面读取公司列表时，统一调用 `/tax-ledger/config/company-code`。

#### 4.2.3 文件管理模块

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/companies/{companyCode}/months/{yearMonth}/files/upload` | 上传文件到指定公司月份 | 该公司权限用户 |
| GET | `/companies/{companyCode}/months/{yearMonth}/files` | 查询指定公司月份的文件列表 | 该公司权限用户 |
| DELETE | `/companies/{companyCode}/months/{yearMonth}/files/{fileId}` | 删除文件记录 | 已具备该公司权限用户或SUPER_ADMIN |
| GET | `/files/{fileId}/download` | 下载文件 | 该公司权限用户 |

**上传接口详细设计**：

```
POST /api/v1/tax-ledger/companies/{companyCode}/months/{yearMonth}/files/upload
Content-Type: multipart/form-data

参数：
- file: MultipartFile（必填）
- fileCategory: String（必填，枚举值见file_category）
- fileSource: String（必填，UPLOAD）
```

#### 4.2.4 数据湖模块

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/companies/{companyCode}/months/{yearMonth}/datalake/pull` | 拉取数据湖数据并生成文件 | 该公司权限用户 |

**拉取接口详细设计**：

```
POST /api/v1/tax-ledger/companies/{companyCode}/months/{yearMonth}/datalake/pull
Content-Type: application/json

请求体：
{
  "fiscalYearPeriod": "2026-01",  // 会计期间
  "postingDateStart": "2026-01-01",  // 可选：凭证日期起始
  "postingDateEnd": "2026-01-31"     // 可选：凭证日期结束
}

处理流程：
1. 调用数据湖API获取全量数据
2. 按account字段拆分为5类明细
3. 生成5个Excel文件
4. 上传到Azure Blob
5. 在t_file_record中创建5条记录
6. 返回拉取结果摘要
```

#### 4.2.5 台账生成模块

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/companies/{companyCode}/months/{yearMonth}/ledger/generate` | 生成台账 | 该公司权限用户 |
| GET | `/companies/{companyCode}/months/{yearMonth}/ledger` | 获取台账状态/信息 | 该公司权限用户 |
| GET | `/companies/{companyCode}/months/{yearMonth}/ledger/download` | 下载台账Excel | 该公司权限用户 |
| GET | `/companies/{companyCode}/ledgers` | 获取该公司所有台账列表 | 该公司权限用户 |

**台账生成接口详细设计**：

```
POST /api/v1/tax-ledger/companies/{companyCode}/months/{yearMonth}/ledger/generate

处理流程：
1. 检查前置条件（BS表、PL表是否存在）
2. 收集所有数据源文件
3. 逐个生成各Sheet页
4. 组装为完整台账Excel
5. 上传到Azure Blob
6. 记录台账生成记录
7. 返回生成结果

前置条件检查：
- BS表：t_file_record中 company_code=X, year_month=Y, file_category=BS
- PL表：t_file_record中 company_code=X, year_month=Y, file_category=PL
- 如不存在，返回缺失文件列表
```

#### 4.2.6 配置管理模块

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/tax-ledger/config/company-code` | 查询公司代码配置表 | 所有已认证用户 |
| POST | `/tax-ledger/config/company-code` | 新增/更新公司代码配置 | SUPER_ADMIN |
| DELETE | `/tax-ledger/config/company-code/{id}` | 删除公司代码配置 | SUPER_ADMIN |
| GET | `/tax-ledger/config/category` | 查询税目配置表 | 所有已认证用户 |
| POST | `/tax-ledger/config/category` | 新增/更新税目配置 | SUPER_ADMIN |
| DELETE | `/tax-ledger/config/category/{id}` | 删除税目配置 | SUPER_ADMIN |
| GET | `/tax-ledger/config/project` | 查询项目配置表 | 所有已认证用户 |
| POST | `/tax-ledger/config/project` | 新增/更新项目配置 | SUPER_ADMIN |
| DELETE | `/tax-ledger/config/project/{id}` | 删除项目配置 | SUPER_ADMIN |
| GET | `/tax-ledger/config/vat-basic` | 查询增值税变动表基础条目配置 | 所有已认证用户 |
| POST | `/tax-ledger/config/vat-basic` | 新增/更新增值税变动表基础条目配置 | SUPER_ADMIN |
| DELETE | `/tax-ledger/config/vat-basic/{id}` | 删除增值税变动表基础条目配置 | SUPER_ADMIN |
| GET | `/tax-ledger/config/vat-special` | 查询增值税变动表特殊条目配置 | 所有已认证用户 |
| POST | `/tax-ledger/config/vat-special` | 新增/更新增值税变动表特殊条目配置 | SUPER_ADMIN |
| DELETE | `/tax-ledger/config/vat-special/{id}` | 删除增值税变动表特殊条目配置 | SUPER_ADMIN |

**读取优先级补充**：
- `companyCode` 有值：返回“通用配置（company_code为空）+ 公司覆盖配置（company_code=指定公司）”。
- `companyCode` 为空：返回全部未删除配置（用于配置总览与总数统计）。
- UI 下拉场景：前端打开公司代码下拉或输入搜索关键字时，实时调用该接口并仅允许选择返回值。

#### 4.2.7 权限管理模块

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/tax-ledger/permissions` | 获取权限列表（可按公司筛选） | SUPER_ADMIN/已具备该公司权限用户 |
| POST | `/tax-ledger/permissions` | 单条授权 | SUPER_ADMIN/已具备该公司权限用户 |
| POST | `/tax-ledger/permissions/batch` | 批量授权（一次提交多员工） | SUPER_ADMIN/已具备该公司权限用户 |
| DELETE | `/tax-ledger/permissions` | 撤销授权（按 employeeId + companyCode） | SUPER_ADMIN/已具备该公司权限用户 |
| GET | `/user/list` | 员工检索（姓名/工号/域账号） | 已认证用户 |

**批量授权请求示例**：
```json
[
  {
    "userId": "zhangsan",
    "userName": "张三",
    "employeeId": "59930",
    "companyCode": "2320"
  },
  {
    "userId": "lisi",
    "userName": "李四",
    "employeeId": "60218",
    "companyCode": "2320"
  }
]
```

---

#### 4.2.8 台账运行控制模块

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/companies/{companyCode}/months/{yearMonth}/runs` | 创建一次台账运行（可指定AUTO/GATED） | 该公司权限用户 |
| GET | `/companies/{companyCode}/months/{yearMonth}/runs/latest` | 查询最新有效运行及阶段状态 | 该公司权限用户 |
| GET | `/ledger-runs/{runId}` | 查询运行详情（阶段、错误、快照） | 该公司权限用户 |
| POST | `/ledger-runs/{runId}/confirm` | 人工确认当前批次并推进下一批（仅GATED） | 该公司权限用户 |
| POST | `/ledger-runs/{runId}/retry` | 从指定批次重试（默认失败批次） | 该公司权限用户 |
| GET | `/ledger-runs/{runId}/artifacts` | 查询并下载各批次快照 | 该公司权限用户 |

**创建运行请求示例**：
```json
{
  "mode": "AUTO",
  "startBatch": 1,
  "idempotencyKey": "20260403-2320-2026-01-run-001"
}
```

**运行控制规则**：
- 默认 `mode=AUTO`；可选 `mode=GATED`。
- 闸门粒度按“批次”控制，不按单个sheet。
- 输入文件或配置变更后，强制创建新run，不复用旧run。
- 对外查询仅展示“最新有效run”。
## 五、核心业务逻辑设计

### 5.1 数据湖拉取与拆分

#### 5.1.1 数据湖API调用

```java
// API路径模板（不含account过滤）
String url = "{platformDomain}/apis/finance/electronicarchives/datalakeit_chatenv/"
    + "finance_electronicarchives_accounting_document_print"
    + "?filter[company_code][EQ]={companyCode}"
    + "&filter[fiscal_year_period][GE]={periodStart}"
    + "&filter[fiscal_year_period][LE]={periodEnd}"
    + "&page[offset]={offset}&page[limit]={limit}"
    + "&fields={fields}";

// 查询时不加account过滤，拉取全量数据后按account拆分
// fields: company_code, fiscal_year_period, posting_date_in_the_document,
//         account, item_text, reference,
//         debit_amount_in_local_currency, credit_amount_in_local_currency,
//         accounting_document_number, company_id_of_trading_partner,
//         debit_credit_indicator
```

#### 5.1.2 科目拆分逻辑

```java
public enum AccountTypeEnum {
    OUTPUT("销项明细", Arrays.asList("2221010200", "2221010400")),
    INPUT("进项明细", Collections.singletonList("2221010100")),
    INCOME_TAX("所得税明细", Collections.singletonList("2221050000")),
    INCOME("收入明细", null),  // account.startsWith("6001")
    OTHER("其他科目明细", Arrays.asList("6603020011", "6702000010"));
}
```

#### 5.1.3 数据湖DTO字段映射

| 中文字段 | API字段 | 取值逻辑 |
|---------|---------|---------|
| 公司代码 | company_code | 直接取值 |
| 财政年/阶段 | fiscal_year_period | 直接取值 |
| 凭证日期 | document_date | 直接取值 |
| 过账日期 | posting_date_in_the_document | 直接取值 |
| 凭证类型 | document_type | 直接取值 |
| 凭证编号 | document_print_number | 直接取值 |
| 抬头文本 | document_header_text | 直接取值 |
| 行项目号 | number_of_line_item_within_accounting_document | 直接取值 |
| 总账科目 | account | 直接取值 |
| 本币金额 | debit/credit_amount_in_local_currency | S借方取debit，H贷方取credit*-1 |
| 本币 | currency_key_of_the_local_currency | 直接取值 |
| 货币金额 | debit/credit_amount_in_document_currency | S借方取debit，H贷方取credit*-1 |
| 凭证货币 | currency_key_of_the_document_currency | 直接取值 |
| 行文本 | item_text | 直接取值 |
| 付款参考 | payment_reference | 直接取值 |
| 贸易伙伴 | company_id_of_trading_partner | 直接取值 |
| 客户 | customer_number | 直接取值 |
| 供应商 | vendor_number | 直接取值 |
| 成本中心 | cost_center | 直接取值 |
| WBS | wbs_element | 直接取值 |
| 参照 | reference | 直接取值 |
| 冲销凭证号 | reverse_document_number | 直接取值 |
| 采购凭证 | purchasing_document_number | 直接取值 |
| 参考代码1 | reference_key_1_for_line_item | 直接取值 |
| 分配 | assignment | 直接取值 |
| 用户名 | user_name | 直接取值 |

### 5.2 台账生成主流程

```
┌──────────────────────────────────────────────────────────┐
│                    台账生成主流程                           │
└──────────────────────────────────────────────────────────┘
                           │
              ┌────────────┴────────────┐
              │ Step 1: 前置条件检查       │
              │ - 检查BS表/PL表是否已上传   │
              │ - 收集所有数据源文件        │
              └────────────┬────────────┘
                           │
              ┌────────────┴────────────┐
              │ Step 2: 加载配置数据       │
              │ - 公司代码配置             │
              │ - 税目配置                 │
              │ - 项目配置                 │
              │ - 增值税变动表配置          │
              └────────────┬────────────┘
                           │
              ┌────────────┴────────────┐
              │ Step 3: 加载历史台账       │
              │ - 查找该公司上月台账       │
              │ - 提取历史数据(如留抵进项税) │
              └────────────┬────────────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                  │
  ┌──────┴──────┐  ┌──────┴──────┐  ┌──────┴──────┐
  │ Step 4a:    │  │ Step 4b:    │  │ Step 4c:    │
  │ 生成非依赖型 │  │ 生成半依赖型 │  │ 生成全依赖型 │
  │ Sheet页     │  │ Sheet页     │  │ Sheet页     │
  │ (BS/PL/    │  │ (增值税变动表)│  │ (未开票监控/ │
  │  数据湖明细) │  │             │  │  累计申报等) │
  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
         │                 │                  │
         └─────────────────┼─────────────────┘
                           │
              ┌────────────┴────────────┐
              │ Step 5: Aspose组装台账Excel    │
              │ - 合并所有Sheet页         │
              │ - 设置格式/样式           │
              └────────────┬────────────┘
                           │
              ┌────────────┴────────────┐
              │ Step 6: 上传&记录         │
              │ - 上传Azure Blob         │
              │ - 更新台账记录状态        │
              └─────────────────────────┘
```

#### 5.2.1 运行模式

- 默认模式：`AUTO`（系统连续执行BATCH1~BATCH4，直至成功或失败）。
- 可选模式：`GATED`（每批成功后暂停，用户确认后再进入下一批）。
- 闸门粒度：按批次，不按单个sheet。

#### 5.2.2 分批执行与Sheet映射（以V2.0“说明”sheet为唯一来源）

**公司类型差异**：
- `2320/2355`：包含专属sheet（累计税金汇总表、累计项目税收明细表、增值税表一-累计销项、账税差异监控、预开票收入计提冲回统计、PL附表-2320、2355）。
- 其他公司：不生成上述2320/2355专属sheet；PL附表走“项目公司口径”（可由上传文件或规则生成）。

**BATCH1（无依赖，可并行）**
- 全公司：BS表、PL表、收入明细、销项明细、进项明细、所得税明细、增值税销项表、增值税进项认证清单。
- 2320/2355：额外包含 累计项目税收明细表、PL附表-2320、2355。
- 其他公司：PL附表-项目公司（若配置/输入要求）。

**BATCH2（依赖BATCH1 + 配置 + 上月留抵）**
- 全公司：增值税变动表（含附表）、Summary表。

**BATCH3（依赖前两批 + 上月台账）**
- 全公司：未开票数监控、项目累计申报、项目累计缴纳。
- 2320/2355：额外包含 累计税金汇总表、增值税表一-累计销项、账税差异监控、预开票收入计提冲回统计。

**BATCH4（依赖前几批）**
- 全公司：Ref Summary表。

**仅取数、不在最终台账展示的sheet（来自说明sheet备注）**
- 睿景景程月结数据表-报税（用于PL附表取数）。
- 合同印花税明细台账（用于取数）。
- 其他科目明细（仅用于取数，不在最终台账中显示；该口径已确认）。
#### 5.2.3 输入变更与失效策略

- 若运行过程中或运行完成后发生输入文件/关键配置变更：
1. 强制创建新run；
2. 旧run标记为 `INVALIDATED`（或其受影响阶段标记为INVALIDATED）；
3. 不复用旧run结果继续执行。

#### 5.2.4 状态机转移矩阵

**run状态转移**

| 当前状态 | 触发事件 | 下一状态 | 说明 |
|---|---|---|---|
| PENDING | 开始执行 | RUNNING | 创建run后进入执行 |
| RUNNING | 当前批成功（AUTO且非最后批） | RUNNING | 自动推进下一批 |
| RUNNING | 当前批成功（GATED且非最后批） | PAUSED | 等待用户确认 |
| PAUSED | 用户确认当前批 | RUNNING | 进入下一批 |
| RUNNING | 最后一批成功 | SUCCESS | 生成FINAL快照并回写主记录 |
| RUNNING | 任一批失败 | FAILED | 记录error_code/error_msg |
| FAILED | 用户重试 | CANCELED/INVALIDATED + 新run(RUNNING) | 推荐新建run承载重试 |
| RUNNING/PAUSED/SUCCESS/FAILED | 输入变更 | INVALIDATED | 旧run失效，不可继续 |
| RUNNING/PAUSED | 用户取消 | CANCELED | 主动终止 |

**stage状态转移**

| 当前状态 | 触发事件 | 下一状态 | 说明 |
|---|---|---|---|
| PENDING | 开始该批 | RUNNING | |
| RUNNING | 该批全部sheet成功 | SUCCESS | |
| RUNNING | 该批任一sheet失败 | FAILED | 记录失败原因 |
| SUCCESS | 用户确认（仅GATED） | CONFIRMED | AUTO模式可不落该态 |
| FAILED | 从该批重试 | INVALIDATED + 新run的PENDING | 推荐新run重试 |
| PENDING/RUNNING/SUCCESS/FAILED | 输入变更 | INVALIDATED | 旧阶段结果失效 |
| PENDING | 依赖不满足/策略跳过 | SKIPPED | 预留 |

**前端交互约束**
- 失败节点支持hover显示失败原因（error_code + error_msg）。
- 失败后允许“从该批重试”或“从指定批次重跑”，禁止跳过依赖批次。
#### 5.2.5 快照策略

- 每批成功后生成 `INTERMEDIATE` 快照，可下载核对。
- 最后一批成功后生成 `FINAL` 快照并回写主记录。
- 快照永久保留，不做TTL清理。
### 5.3 台账Sheet页生成优先级与依赖关系

#### 5.3.1 Sheet页分类

| 分类 | Sheet页 | 数据依赖 | 说明 |
|------|---------|---------|------|
| **类型A：直取型** | BS表 | 当月上传的BS表 | 取1月至台账月的BS表，按月拼接 |
| | 睿景月结数据表 | 用户上传睿景月结数据表 | **直接取用，无需计算** |
| | PL表 | 当月上传的PL表 | 取1月至台账月的PL表，按月拼接 |
| | BS附表-应交税费 | 当月上传的BS附表 | 同BS逻辑 |
| | PL附表 | 当月上传的PL附表 | 2320/2355使用"PL附表-2320、2355"，其他公司使用"PL附表-项目公司" |
| | 销项明细 | 数据湖DL_OUTPUT | 直接取当月数据 |
| | 进项明细 | 数据湖DL_INPUT | 直接取当月数据 |
| | 收入明细 | 数据湖DL_INCOME | 直接取当月数据 |
| | 所得税明细 | 数据湖DL_INCOME_TAX | 直接取当月数据 |
| | 其他科目明细（仅取数，不展示） | 数据湖DL_OTHER | 直接取当月数据 |
| | 印花税明细 | 用户上传STAMP_TAX | 直接取当月数据 |
| | 增值税销项 | 用户上传VAT_OUTPUT | 直接取当月数据 |
| | 增值税进项认证清单 | 用户上传VAT_INPUT_CERT | 直接取当月数据 |
| | 累计项目税收明细表 | 用户上传CUMULATIVE_PROJECT_TAX | 直接取当月数据 |
| | 睿景月结数据表 | 用户上传PRE_BILLED | **直接取用，无需计算**（稍后补充模板） |
| | 预开票收入计提冲回统计 | 用户上传PRE_BILLED | **直接取用，无需计算** |
| **类型B：计算型** | 增值税变动表 | 配置表+PL附表+增值税销项+增值税进项认证清单+上月台账增值税变动表 | 需读取配置和上月台账 |
| | Summary表 | 配置表+PL表+BS表+数据湖明细+印花税明细+增值税进项认证清单 | 需读取多个数据源 |
| | Ref Summary表 | 同Summary+配置表 | 需读取配置表 |
| **类型C：累积型** | 未开票数监控 | 上月台账未开票数监控+当月PL表+数据湖明细 | **必须依赖上月台账** |
| | 项目累计申报 | 上月台账项目累计申报+当月Summary表 | **必须依赖上月台账** |
| | 项目累计缴纳 | 上月台账项目累计缴纳 | **必须依赖上月台账** |
| | 累计税金汇总表 | 上月台账累计税金汇总表+当月Summary表 | **必须依赖上月台账** |
| | 增值税表-累计销项 | 上月台账+当月数据湖明细 | **必须依赖上月台账** |
| | 账税差异监控 | 上月台账+当月BS表+PL表 | **必须依赖上月台账** |

**重要说明**：
- **系统不支持跨年数据处理**：1月份台账的期初数据（如留抵进项税）取0，不依赖上年度12月数据
- **累积型Sheet页首次生成规则**：首次生成某公司台账时，累积型Sheet页的数据从0开始计算

#### 5.3.2 Sheet页生成顺序

```
第一批（无依赖，可并行生成）：
  BS表、PL表、BS附表、PL附表
  销项明细、进项明细、收入明细、所得税明细、其他科目明细（仅取数，不展示）
  印花税明细、增值税销项、增值税进项认证清单、累计项目税收明细表

第二批（依赖第一批数据 + 配置表）：
  增值税变动表（还依赖上月台账的留抵进项税）
  Summary表

第三批（依赖前两批 + 上月台账）：
  未开票数监控、项目累计申报、项目累计缴纳
  累计税金汇总表、增值税表-累计销项、账税差异监控
  预开票收入计提冲回统计

第四批（依赖前几批）：
  Ref Summary表
```

### 5.4 增值税变动表生成逻辑（核心复杂逻辑）

这是整个系统中最复杂的Sheet页，涉及按公司类型区分不同逻辑：

#### 5.4.1 输入数据

- 公司代码 companyCode
- 当月 PL表/PL附表（根据公司代码选择不同的PL附表）
- 当月 增值税销项表（用户上传）
- 当月 增值税进项认证清单（用户上传）
- 当月 收入明细（数据湖）
- 上月台账的增值税变动表（用于期初留抵进项税）
- 增值税变动表基础条目配置表
- 睿景景程增值税变动表特殊条目配置表

#### 5.4.2 公司类型判断与数据源选择

```
判断逻辑：
  IF companyCode IN (2320, 2355):
    // 睿景、景程公司
    使用"睿景、景程增值税变动表特殊条目配置表"
    PL附表使用"PL附表-2320、2355"（包含拆分依据列）
    按6种税率分别计算：专票13%/9%/6% + 普票13%/9%/6%
  ELSE:
    // 其他公司
    使用"增值税变动表基础条目配置表"
    PL附表使用"PL附表-项目公司"（包含拆分依据列）
    按基础条目配置表中的条目计算
  END IF
```

#### 5.4.3 PL附表拆分依据匹配规则

**PL附表结构（以2320、2355为例）**：

| 拆分依据 | 未开票收入 | 销项 | 已开票收入 | 已开票销项 |
|---------|-----------|------|-----------|-----------|
| 专票-13% | 申报金额-未开票收入 | 申报税额-未开票销项 | 设备区域已开票收入合计 | 设备区域已开票税额合计 |
| 专票-9% | 申报金额-未开票收入 | 申报税额-未开票销项 | 建安区域已开票收入合计 | 建安区域已开票税额合计 |
| 专票-6% | 申报金额-未开票收入 | 申报税额-未开票销项 | 其他区域已开票收入合计 | 其他区域已开票税额合计 |
| 普票-13% | ... | ... | ... | ... |
| ... | ... | ... | ... | ... |
| 利息收入 | ... | ... | ... | ... |

**匹配规则**：

增值税变动表的"拆分依据"列与PL附表的"拆分依据"列进行精确字符串匹配：
- "专票-13%" 对应 PL附表中"拆分依据=专票-13%"的那一行
- "利息收入" 对应 PL附表中"拆分依据=利息收入"的那一行

取值逻辑：
- **合计金额**：取PL附表中对应拆分依据行的"申报金额"列（未开票+已开票）
- **未开票金额**：取PL附表中对应拆分依据行的"未开票收入"列
- **当月开票金额**：取PL附表中对应拆分依据行的"已开票收入"列
- **销项税**：取PL附表中对应拆分依据行的"销项"列（或"申报税额"列，根据列名判断）

#### 5.4.4 增值税销项表匹配规则（仅2320、2355）

增值税销项表需按"发票类型+税率"进行汇总：

| 发票类型 | 税率 | 对应拆分依据 | 取值逻辑 |
|---------|------|------------|---------|
| 专用发票 | 13% | 专票-13% | 发票类型=专票且税率=13%的开票金额合计 |
| 专用发票 | 9% | 专票-9% | 发票类型=专票且税率=9%的开票金额合计 |
| 专用发票 | 6% | 专票-6% | 发票类型=专票且税率=6%的开票金额合计 |
| 普通发票 | 13% | 普票-13% | 发票类型=普票且税率=13%的开票金额合计 |
| 普通发票 | 9% | 普票-9% | 发票类型=普票且税率=9%的开票金额合计 |
| 普通发票 | 6% | 普票-6% | 发票类型=普票且税率=6%的开票金额合计 |

**注意**：非2320、2355公司，当月开票金额直接取增值税销项表的合计行（不按税率拆分）。

#### 5.4.6 异地预缴抵减取值规则（仅2320、2355）

- 适用范围：公司编码 `2320`、`2355`。
- 规则来源：`税务台账生成逻辑_V2.0.xlsx`（`增值税变动表基础条目配置表` + `累计税金汇总表-2320、2355`）。
- 取值口径：取“台账期”【增值税变动表】中条目=`异地预缴抵减` 的“合计”字段值。
- 实现约束：该项已确认，按固定规则实现，不作为可配置待确认项。
#### 5.4.5 留抵进项税与应交增值税计算

**注意**：系统不支持跨年数据处理，1月份台账不依赖上年度12月数据，期初留抵进项税取0。

```
期初留抵进项税 → 取上月台账增值税变动表期末留抵进项税合计
  （1月份取0，不支持跨年数据）

期末留抵进项税 =
  IF (销项税 - (已认证进项税 + 期初留抵进项税 - 进项转出)) < 0
  THEN ABS(差值)
  ELSE 0

应交增值税 =
  IF 期末留抵进项税合计 > 0 THEN 0
  ELSE 销项税额合计 + 销项税合计 + 进项转出合计 - 已认证进项税合计 - 期初留抵进项税合计
```

### 5.5 月份跳过与补零策略

```
当用户生成台账时，如果该公司存在月份间断（例如已有1月、2月，直接生成5月）：

1. 检查连续性：
   - 获取该公司最新台账月份 lastMonth
   - 如果当前生成月份 currentMonth > lastMonth + 1
   - 自动为中间缺失月份（lastMonth+1 到 currentMonth-1）生成空台账

2. 空台账规则：
   - 所有Sheet页的数据单元格填0
   - 累积型Sheet页（未开票数监控等）从上个月末数据开始
   - 增值税变动表中：期初留抵进项税继承上月值，其他为0

3. 补零台账的台账记录状态标记为 AUTO_ZERO
```

---

## 六、前端页面设计

### 6.1 页面结构

```
┌──────────────────────────────────────────────────┐
│  顶部导航栏（Logo + 系统名称 + 用户信息 + 退出）     │
├──────────────────────────────────────────────────┤
│  侧边栏菜单                                        │
│  ├── 公司管理                                      │
│  ├── 配置管理（仅SUPER_ADMIN可见）                   │
│  └── 权限管理（SUPER_ADMIN或当前公司授权用户可见）     │
├──────────────────────────────────────────────────┤
│                                                  │
│                   主内容区                          │
│                                                  │
└──────────────────────────────────────────────────┘
```

**全局交互约束**：
- 涉及“公司代码”的弹窗表单（除“公司代码配置”页）统一使用下拉选择，不允许手工输入。
- 下拉选项实时读取公司代码配置，前端与后端共同保证“只能选已配置公司代码”。

### 6.2 首页 - 公司列表页

```
┌─────────────────────────────────────────────────────────┐
│  EPC进项税报表系统                                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐    │
│  │ 2320    │  │ 2355    │  │ 3019    │  │ 4470    │    │
│  │ 上海睿景 │  │ 上海景程 │  │ 江阴青芦 │  │ 鄂尔多斯 │    │
│  │         │  │         │  │         │  │         │    │
│  │ 最新台账  │  │ 最新台账  │  │ 最新台账  │  │ 最新台账  │    │
│  │ 2026-02 │  │ 2026-02 │  │ 2026-01 │  │ 2025-12 │    │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘    │
│                                                         │
│  ┌─────────┐  ┌─────────┐                              │
│  │ 4471    │  │ ...     │                              │
│  │ xxx公司  │  │         │                              │
│  └─────────┘  └─────────┘                              │
│                                                         │
│  [+ 新增公司]                                            │
└─────────────────────────────────────────────────────────┘
```

每个公司卡片显示：
- 公司代码 + 公司名称
- 最新台账月份
- 该月数据完整性状态（哪些文件已上传/缺失）
- 点击卡片进入公司详情页

### 6.3 公司详情页

```
┌─────────────────────────────────────────────────────────┐
│  ← 返回列表    上海睿景能源科技有限公司 (2320)               │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  操作按钮区：                                             │
│  [上传文件]  [拉取数据湖]  [生成台账]  [权限管理]           │
│                                                         │
│  月份选择器：< 2026-03 >                                  │
│                                                         │
│  ┌─────────────────────────────────────────────────┐    │
│  │  文件中心 - 2026年03月                            │    │
│  ├──────────┬──────────┬──────┬──────┬──────────────┤    │
│  │ 文件名称  │ 文件类别   │ 来源  │ 上传人 │ 操作         │    │
│  ├──────────┼──────────┼──────┼──────┼──────────────┤    │
│  │ BS表.xlsx│ BS       │ 上传  │ 张三  │ [下载][删除]  │    │
│  │ PL表.xlsx│ PL       │ 上传  │ 张三  │ [下载][删除]  │    │
│  │ 销项明细  │ DL_OUTPUT│ 数据湖 │ 系统  │ [下载]       │    │
│  │ 进项明细  │ DL_INPUT │ 数据湖 │ 系统  │ [下载]       │    │
│  │ 收入明细  │ DL_INCOME│ 数据湖 │ 系统  │ [下载]       │    │
│  │ 所得税明细 │DL_INCOME │ 数据湖 │ 系统  │ [下载]       │    │
│  │ 其他明细  │ DL_OTHER │ 数据湖 │ 系统  │ [下载]       │    │
│  ├──────────┼──────────┼──────┼──────┼──────────────┤    │
│  │ 缺失文件提醒：印花税明细、增值税销项表...               │    │
│  └──────────┴──────────┴──────┴──────┴──────────────┘    │
│                                                         │
│  ┌─────────────────────────────────────────────────┐    │
│  │  台账记录                                         │    │
│  ├──────────┬──────────────────────────────────────┤    │
│  │ 月份      │ 状态              │ 操作              │    │
│  ├──────────┼──────────────────────────────────────┤    │
│  │ 2026-02  │ 已生成 (2026-02-28) │ [下载]           │    │
│  │ 2026-01  │ 已生成 (2026-01-30) │ [下载]           │    │
│  │ 2025-12  │ 已生成 (2025-12-29) │ [下载]           │    │
│  └──────────┴──────────────────────────────────────┘    │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 6.4 上传文件弹窗

```
┌──────────────────────────────┐
│  上传文件                      │
│                              │
│  公司：上海睿景(2320) [只读]    │
│  月份：2026-03      [只读]     │
│                              │
│  文件类别：[请选择 ▼]          │
│  ├ BS 资产负债表               │
│  ├ PL 利润表                   │
│  ├ BS附表-应交税费             │
│  ├ PL附表                     │
│  ├ 印花税明细                  │
│  ├ 增值税销项                  │
│  ├ 增值税进项认证清单           │
│  └ 累计项目税收明细表           │
│                              │
│  [拖拽或点击上传文件]           │
│  ┌──────────────────────────┐│
│  │   选择文件或拖拽到此处     ││
│  └──────────────────────────┘│
│                              │
│  支持.xlsx/.xls格式           │
│                              │
│        [取消]    [确认上传]    │
└──────────────────────────────┘
```

### 6.5 拉取数据湖弹窗

```
┌──────────────────────────────┐
│  拉取数据湖数据                │
│                              │
│  公司：上海睿景(2320) [只读]    │
│  月份：2026-03      [只读]     │
│                              │
│  会计期间：2026-03            │
│  凭证日期范围（选填）：         │
│  起始：[2026-03-01]           │
│  结束：[2026-03-31]           │
│                              │
│  将自动拉取并生成以下文件：      │
│  □ 收入明细                    │
│  □ 销项明细                    │
│  □ 进项明细                    │
│  □ 所得税明细                  │
│  其他科目明细（仅取数，不展示）                │
│                              │
│        [取消]    [开始拉取]    │
└──────────────────────────────┘
```

### 6.6 生成台账弹窗

```
┌──────────────────────────────────┐
│  生成台账                          │
│                                  │
│  公司：上海睿景(2320) [只读]        │
│  月份：2026-03      [只读]         │
│                                  │
│  数据检查：                        │
│  ✅ BS表 (已上传)                  │
│  ✅ PL表 (已上传)                  │
│  ❌ 印花税明细 (未上传)              │
│  ✅ 收入明细 (数据湖已拉取)          │
│  ✅ 销项明细 (数据湖已拉取)          │
│  ✅ 进项明细 (数据湖已拉取)          │
│  ...                              │
│                                  │
│  ⚠️ 检测到2月、4月台账缺失，        │
│     将自动补零生成                  │
│                                  │
│  上月台账依赖：                     │
│  ✅ 2026-02台账已存在               │
│                                  │
│        [取消]    [开始生成]         │
└──────────────────────────────────┘
```

### 6.6.1 分批生成与人工闸门交互

- 在“生成台账弹窗”增加运行模式切换：`AUTO`（默认）/`GATED`。
- 展示四批阶段时间线：每批状态、开始/结束时间、失败信息、快照下载入口。
- GATED模式下：每批成功后显示“确认进入下一批”按钮。
- 提供“从失败批次重试”与“从指定批次重跑”操作。
- 若检测到输入变更，提示“当前run已失效，需新建run”。
- 列表页与详情页仅展示最新有效run，同时提供历史run入口（只读审计）。
### 6.7 配置管理页（仅SUPER_ADMIN）

**交互补充规范（已联调验证）**：
- 页面布局：左侧配置导航，右侧列表操作区（搜索/刷新/新增）。
- 新增/编辑：使用独立弹窗（Modal）录入，不嵌入列表区域。
- 删除操作：必须二次确认（Popconfirm），防止误操作。
- 分页：展示总条数与页码，分页文案中文化（示例：`共X条`、`10/页`）。
- 字段约束：仅“公司代码配置”页允许手工输入公司代码；其余配置页中的 `companyCode` 字段必须使用下拉选择。
- 下拉数据源：实时查询 `/tax-ledger/config/company-code`，禁止自由输入，确保公司代码只来自主数据表。

```
┌─────────────────────────────────────────────────────────┐
│  配置管理                                    超级管理员   │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Tab: [公司代码配置] [税目配置] [项目配置]                   │
│       [增值税变动表基础条目] [增值税变动表特殊条目]           │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │  公司代码配置                                       │  │
│  ├──────┬──────┬──────┬──────┬──────┬───────────────┤  │
│  │公司代码│公司名称│BP AD │BP姓名 │BP邮箱 │  操作        │  │
│  ├──────┼──────┼──────┼──────┼──────┼───────────────┤  │
│  │ 2320 │上海睿景│ xxx  │ xxx  │ xxx  │[编辑][删除]   │  │
│  │ 2355 │上海景程│ xxx  │ xxx  │ xxx  │[编辑][删除]   │  │
│  │ 3019 │江阴青芦│ xxx  │ xxx  │ xxx  │[编辑][删除]   │  │
│  └──────┴──────┴──────┴──────┴──────┴───────────────┘  │
│                                                         │
│  [+ 新增配置]                                            │
└─────────────────────────────────────────────────────────┘
```

### 6.8 权限管理页

**交互补充规范（已落地）**：
- 主页面仅保留“添加权限”按钮，点击后弹出独立授权弹窗。
- 员工选择使用可搜索下拉（调用 `/user/list`），支持多选员工。
- 批量提交时调用 `/tax-ledger/permissions/batch`，一次完成同公司代码的多员工授权。
- 取消 `permission_level` 字段，权限页仅维护“用户-公司”映射关系。
- 权限列表右下角分页，展示总条数与每页条数中文文案。
- 公司代码字段使用下拉选择（非输入框），数据实时来自 `/tax-ledger/config/company-code`。
- 新增/撤销权限均要求明确公司代码。

**超级管理员视图**：

```
┌─────────────────────────────────────────────────────────┐
│  权限管理                                    超级管理员   │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Tab: [超级管理员] [公司权限]                               │
│                                                         │
│  超级管理员列表：                                          │
│  ┌──────────┬──────────┬─────────────────────────────┐  │
│  │ 用户ID    │ 用户姓名   │ 操作                        │  │
│  ├──────────┼──────────┼─────────────────────────────┤  │
│  │ user_001 │ 张三     │ [移除超级管理员]              │  │
│  │ user_002 │ 李四     │ [移除超级管理员]              │  │
│  └──────────┴──────────┴─────────────────────────────┘  │
│                                                         │
│  [+ 添加超级管理员]                                        │
└─────────────────────────────────────────────────────────┘
```

**公司权限视图**：

```
┌─────────────────────────────────────────────────────────┐
│  公司权限                                        超级管理员 │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  选择公司：[上海睿景(2320) ▼]                              │
│                                                         │
│  ┌──────────┬────────────┬─────────────────────────────┐ │
│  │ 用户姓名   │ 授权人      │ 操作                        │ │
│  ├──────────┼────────────┼─────────────────────────────┤ │
│  │ 王五     │ 张三       │ [移除]                       │ │
│  │ 赵六     │ 王五       │ [移除]                       │ │
│  └──────────┴────────────┴─────────────────────────────┘ │
│                                                         │
│  [+ 添加用户权限]                                          │
└─────────────────────────────────────────────────────────┘
```

---

## 七、权限模型设计

### 7.1 权限层级

```
SUPER_ADMIN（超级管理员）
├── 可访问所有公司
├── 可维护5张配置表
├── 可管理超级管理员列表
└── 可管理所有公司的用户权限

COMPANY_MEMBER（公司授权用户）
├── 可访问指定公司
├── 可上传/下载该公司文件
├── 可拉取数据湖、生成台账
├── 可给他人分配该公司的访问权限
└── 不可管理5个配置页
```

### 7.2 权限校验拦截器

```java
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, 
                             HttpServletResponse response, 
                             Object handler) {
        String userId = OktaUtil.getCurrentUserId();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        
        // 1. 解析URI中的companyCode
        String companyCode = extractCompanyCode(uri);
        
        // 2. 查询用户权限
        List<UserPermission> permissions = permissionService
            .findByUserId(userId);
        
        // 3. 校验逻辑：SUPER_ADMIN（来自配置）放行
        if (superAdminConfig.contains(userId)) return true;
        
        if (companyCode != null) {
            boolean hasAccess = permissions.stream()
                .anyMatch(p -> companyCode.equals(p.getCompanyCode()));
            if (!hasAccess) {
                throw new AccessDeniedException("无权访问该公司数据");
            }
        }
        
        return true;
    }
}
```

---

## 八、文件存储设计

### 8.1 Azure Blob目录结构

```
tax-ledger/
├── {companyCode}/
│   ├── {yearMonth}/
│   │   ├── upload/
│   │   │   ├── BS_{yearMonth}.xlsx
│   │   │   ├── PL_{yearMonth}.xlsx
│   │   │   ├── STAMP_TAX_{yearMonth}.xlsx
│   │   │   └── ...
│   │   ├── datalake/
│   │   │   ├── DL_INCOME_{yearMonth}.xlsx
│   │   │   ├── DL_OUTPUT_{yearMonth}.xlsx
│   │   │   ├── DL_INPUT_{yearMonth}.xlsx
│   │   │   ├── DL_INCOME_TAX_{yearMonth}.xlsx
│   │   │   └── DL_OTHER_{yearMonth}.xlsx
│   │   └── ledger/
│   │       └── 税务台账_{companyName}_{yearMonth}.xlsx
```

### 8.2 Blob命名规则

| 文件类型 | 命名格式 | 示例 |
|---------|---------|------|
| 上传文件 | `{category}_{yearMonth}_{timestamp}.xlsx` | `BS_2026-03_1711776000.xlsx` |
| 数据湖文件 | `DL_{category}_{yearMonth}.xlsx` | `DL_INCOME_2026-03.xlsx` |
| 台账文件 | `税务台账_{companyName}_{yearMonth}.xlsx` | `税务台账_上海睿景_2026-03.xlsx` |

---

## 九、异常处理与边界场景

### 9.1 关键异常场景

| 场景 | 处理策略 |
|------|---------|
| 台账生成时BS/PL表缺失 | 中断生成，返回缺失文件列表提示用户上传 |
| 数据湖API调用失败 | 返回错误信息，不生成数据湖文件 |
| 数据湖返回数据为空 | 生成空的明细Excel（只有表头），不中断流程 |
| 上月台账不存在（非1月份） | 自动为缺失月份补零生成台账，确保连续性 |
| 同一月份重复上传同类别文件 | 覆盖旧文件（逻辑删除旧记录） |
| 同一月份重复生成台账 | 覆盖旧台账（逻辑删除旧记录） |
| 数据湖拉取与上传文件冲突 | 同一类别优先使用用户上传的文件 |

### 9.1.1 分批生成异常处理策略

| 场景 | 处理策略 |
|------|---------|
| 某批sheet生成失败 | 当前stage置FAILED，run置FAILED，保留前序批次快照 |
| GATED模式下用户长期未确认 | run保持PAUSED，不自动超时推进 |
| 同公司同月份重复触发生成 | 并发互斥；若输入变更则新建run，旧run置INVALIDATED |
| 上传新文件后继续旧run | 禁止继续；必须新建run |
| 中间快照上传失败 | 当前stage置FAILED，run置FAILED，记录错误并可重试 |

**回滚说明**：采用“逻辑回滚”，不物理删除历史快照；对外仅暴露最新有效run。
### 9.2 台账生成完整性校验

生成完成后进行后置校验：

```
1. 检查所有Sheet页是否都已生成
2. 检查是否有#REF!、#DIV/0!等公式错误
3. 检查累积型Sheet页的数据是否与上月衔接
4. 检查增值税变动表的应交增值税计算是否正确
5. 使用Aspose执行公式重算后检查是否存在#REF!/#DIV/0!等错误
6. 检查关键模板区域（样式/合并单元格）未被破坏
7. 校验通过 → 状态设为SUCCESS
   校验失败 → 状态设为FAILED，记录错误信息（含sheet、cell、errorCode）
```

### 9.3 前后端错误协同约束（实施补充）

- 后端返回契约：统一 `Echo{code,msg,data}`。
- 前端判定规则：
  - 仅 `code==0` 视为成功。
  - `code!=0` 必须展示 `msg`，并阻断成功提示。
- 稳定性要求：所有关键异步提交/删除操作必须 `try/catch`，避免 `Uncaught (in promise)` 导致页面卡死或静默失败。

---

## 十、Excel格式规范与样式要求

### 10.1 台账Excel总体要求

**核心原则**：生成的台账Excel必须与现有Excel模板格式完全一致，包括：
- Sheet页顺序、名称
- 单元格合并、字体、颜色、边框
- 数字格式、日期格式
- 公式链接（如存在）

### 10.2 各Sheet页格式规范

#### 10.2.1 Sheet页顺序

最终台账Excel应按以下顺序包含Sheet页：

```
1. 税务台账_{companyName}_{yearMonth}（主文件名）
2. BS表
3. PL表
4. BS附表
5. PL附表
6. 印花税明细
7. 增值税销项
8. 增值税进项认证清单
9. 累计项目税收明细表
10. 收入明细
11. 销项明细
12. 进项明细
13. 所得税明细
14. 其他科目明细（仅取数，不在最终台账展示）
15. Summary表
16. 增值税变动表
17. 未开票数监控
18. 项目累计申报
19. 项目累计缴纳
20. 累计税金汇总表
21. 增值税表一 累计销项
22. 账税差异监控
23. 预开票收入计提冲回统计
24. Ref Summary表
```

#### 10.2.2 格式样式标准

| 样式项 | 要求 | 备注 |
|--------|------|------|
| 字体 | 微软雅黑或Arial | 保持与模板一致 |
| 标题行 | 加粗、背景色（黄色/蓝色/绿色等） | 根据不同Sheet页的模板样式 |
| 数字格式 | 保留2位小数 | 千分位分隔符根据模板确定 |
| 日期格式 | yyyy-MM-dd | 或根据模板要求 |
| 边框 | 细边框 | 所有数据单元格 |
| 对齐 | 标题居中，数字右对齐 | 文本左对齐 |
| 合并单元格 | 与模板完全一致 | 特别是表头部分 |

#### 10.2.3 颜色编码规范

根据Excel模板，常见的颜色用途：

| 颜色 | 用途 | Sheet页 |
|------|------|---------|
| 浅黄色 | 表头、重点数据 | 多数Sheet页 |
| 浅绿色 | 用户手工上传的数据 | 说明Sheet中标绿的部分 |
| 浅蓝色 | 从数据湖拉取的数据 | 说明Sheet中标蓝的部分 |
| 浅紫色 | 从上月台账导出的数据 | 说明Sheet中标紫的部分 |
| 浅橙色 | 从配置表取数的数据 | 说明Sheet中标橙色的部分 |

### 10.3 模板文件管理

**模板文件存储位置**：
- Azure Blob Storage: `tax-ledger/templates/`
- 本地开发环境：`src/main/resources/templates/`

**模板文件列表**：
- `template_vat_change.xlsx` - 增值税变动表模板
- `template_summary.xlsx` - Summary表模板
- `template_bs.xlsx` - BS表模板
- `template_pl.xlsx` - PL表模板
- 等等...

**模板版本治理要求**：
- 模板元数据：`template_code`、`template_version`、`checksum(SHA-256)`、`effective_from`
- 运行时落库：每次`ledger_run`记录`template_version`与`template_checksum`，用于审计追溯
- 生效规则：同一`template_code`仅允许一个生效版本；新版本发布后仅影响新建run
### 10.4 Excel公式处理策略

**推荐方案:计算后值+部分公式(混合模式)**

| 单元格类型 | 处理方式 | 理由 |
|----------|---------|------|
| **系统计算的字段**(如汇总行、差异列、留抵税计算等) | 填写计算结果值 | 系统计算的值更准确,避免公式错误 |
| **用户可能修改的字段**(如备注、说明等) | 保留文本值,不使用公式 | 用户修改方便,不会破坏公式 |
| **模板中的固定公式**(如=SUM(A2:A100)) | 尽量保留 | 用户可理解计算逻辑,便于后续调整 |
| **动态引用公式**(如='其他Sheet'!A1) | 替换为计算后的值 | 避免引用丢失,跨Sheet引用维护困难 |

**具体实现策略**:

```java
// 策略1：系统计算值直接写入（Aspose）
Workbook wb = new Workbook(templatePath);
Worksheet ws = wb.getWorksheets().get("增值税变动表");
Cells cells = ws.getCells();

// F列写入系统计算结果
cells.get(targetRow, 5).putValue(totalAmount.doubleValue());

// 策略2：汇总类单元格保留简单公式（Aspose）
cells.get(sumRow, 10).setFormula("=SUM(E2:E" + (dataRowCount + 1) + ")");

// 策略3：模板公式复制到目标单元格（Aspose）
Cell templateCell = cells.get(templateRow, 3);
Cell newCell = cells.get(newRow, 3);
if (templateCell.getType() == CellValueType.IS_FORMULA) {
    newCell.setFormula(templateCell.getFormula());
}

// 按需重算
wb.calculateFormula();
```

**优点**:
- ✅ 系统计算准确,无公式错误风险
- ✅ 关键汇总单元格有公式,用户可理解计算逻辑
- ✅ 数据稳定,不会因单元格删除/插入导致公式失效
- ✅ 兼容性好,不同Excel版本都能正常打开

**缺点**:
- ⚠️ 混合模式需要明确的编码规范
- ⚠️ 修改后需要重新生成才能更新计算值

### 10.5 Aspose模板填充/公式重算/Excel生成技术方案

使用 **Aspose.Cells** 作为本项目Excel生成的唯一实现方式：

**推荐方案**：
```java
// 1) 加载模板
Workbook workbook = new Workbook(templatePath);
Worksheet sheet = workbook.getWorksheets().get("Sheet名称");
Cells cells = sheet.getCells();

// 2) 按定位写入数据（示例：从第6行开始）
int startRow = 5;
for (int i = 0; i < rows.size(); i++) {
    cells.get(startRow + i, 0).putValue(rows.get(i).getName());
    cells.get(startRow + i, 1).putValue(rows.get(i).getAmount().doubleValue());
}

// 3) 公式重算（按需）
workbook.calculateFormula();

// 4) 导出阶段快照 / 最终台账
workbook.save(outputPath);
```

**样式与结构保障策略**：
- 以模板为唯一样式来源，运行时仅填充值，不在代码中重建样式
- 合并单元格、边框、条件格式按模板保留；新增数据行仅做必要样式复制
- 跨Sheet引用优先保留模板公式；不稳定动态引用可回填计算值

### 10.6 公式处理

**策略**：
- 如果原始模板包含公式，生成的Excel应保留公式结构
- 系统计算的数据优先填写计算结果值，而非公式
- 需要动态计算的单元格使用公式，便于用户后续修改

---

## 十一、开发里程碑建议

| 阶段 | 内容 | 预估工作量 |
|------|------|-----------|
| P0 - 基础框架 | 项目搭建、数据库建表、认证集成、权限框架 | 3天 |
| P1 - 文件管理 | 公司CRUD、文件上传/下载/列表、Azure Blob集成 | 3天 |
| P2 - 数据湖集成 | API调用、科目拆分、Aspose模板填充/公式重算/Excel生成 | 3天 |
| P3 - 台账生成（核心） | 各Sheet页生成器、台账组装、前置校验 | 7天 |
| P4 - 前端页面 | 首页、公司详情页、上传/拉取/生成弹窗 | 5天 |
| P5 - 配置与权限 | 配置表管理CRUD、权限管理页面 | 3天 |
| P6 - 测试与优化 | 集成测试、异常处理、性能优化 | 4天 |
| **合计** | | **约28个工作日** |

---

## 十一、待确认事项

| 序号 | 事项 | 状态 | 备注 |
|------|------|------|------|
| 1 | 前端技术栈确认 | 待确认 | 建议与现有脚手架一致 |
| 2 | 增值税变动表附表的详细取值逻辑 | ✅ 已确认 | 按PL附表的"拆分依据"列精确匹配，2320/2355按6种税率拆分，其他公司按基础条目计算 |
| 3 | 预开票收入计提冲回统计的详细逻辑 | ✅ 已确认 | 用户上传的文件，系统直接取用，无需计算 |
| 4 | 1月份台账生成时上年度12月台账数据来源 | ✅ 已确认 | 系统不支持跨年数据处理，1月份期初留抵进项税取0 |
| 5 | 台账Excel的具体格式/样式要求 | ✅ 已确认 | 需要与现有Excel模板格式完全一致 |
| 6 | 数据湖API的具体域名和认证方式 | ✅ 已确认 | 参考InvoiceCommandService实现，使用PlatformRemote，域名配置在application.yml |
| 7 | 【睿景月结数据表】的数据来源 | ✅ 已确认 | 用户上传的文件，模板稍后补充 |
| 8 | 增值税变动表中"异地预缴抵减"的取值逻辑 | ✅ 已确认 | 仅2320/2355；取台账期【增值税变动表】条目=异地预缴抵减的合计值（依据V2.0配置与累计税金汇总表） |
> 说明：sheet覆盖范围与公司差异以《税务台账生成逻辑_V2.0.xlsx》“说明”sheet为唯一来源。  
| 9 | 财务BP的权限模型 | ✅ 已确认 | 已简化为两级权限：超级管理员 + 公司授权用户（无 permission_level 字段） |
| 10 | 台账生成失败后的回滚策略 | 待确认 | 是否删除已生成的部分数据？ |
| 11 | 台账导出时是否包含公式 | ✅ 已确认 | 采用混合模式：系统计算字段填值，汇总类单元格保留公式 |
| 12 | 大量数据时的性能优化方案 | ✅ 已确认 | 暂不纳入当前交付范围，作为后续优化方向处理 |
| 13 | 睿景月结数据表的具体模板 | ✅ 已确认 | 已提供，按《税务台账生成逻辑_V2.0.xlsx》的【睿景景程月结数据表-报税】sheet执行 |
| 14 | 默认模式（AUTO/GATED） | ✅ 已确认 | 默认AUTO，可切换GATED |
| 15 | 人工闸门粒度 | ✅ 已确认 | 按批次闸门，不按单sheet |
| 16 | 中间快照保留期 | ✅ 已确认 | 永久保留 |
| 17 | 输入变更后运行策略 | ✅ 已确认 | 强制新建run，不复用旧run |
| 18 | 对外展示策略 | ✅ 已确认 | 仅展示最新有效run |
---

## 十二、下一步行动建议

### 12.1 需要业务方澄清的问题

**优先级P0（必须澄清才能开始开发）**：

1. ~~**数据湖API接入信息**~~ ✅ 已确认
   - 参考现有InvoiceCommandService实现即可
   - API域名配置在application.yml的custom.platform.token.domain
   - 使用PlatformRemote封装的接口，复用Okta认证

2. ~~**【睿景月结数据表】来源**~~ ✅ 已确认
   - 用户上传的文件
   - 模板样式会稍后补充

3. ~~**"异地预缴抵减"取值来源**~~ ✅ 已确认
   - 仅适用于2320/2355
   - 取台账期【增值税变动表】中条目=异地预缴抵减的合计字段值
   - 依据：V2.0中的【增值税变动表基础条目配置表】与【累计税金汇总表-2320、2355】

**优先级P1（开发过程中可以逐步明确）**：

4. ~~**财务BP权限模型**~~ ✅ 已确认
   - 已简化为两级权限模型
   - 超级管理员配置在application.yml
   - 公司使用者由超级管理员或已具备该公司权限的用户分配

5. **台账生成失败的回滚策略**
   - 部分Sheet生成失败，是否回滚已生成的Sheet？
   - 还是记录错误状态，允许用户查看已生成的部分？

6. ~~**Excel公式策略**~~ ✅ 已确认
   - 采用混合模式：系统计算字段填值，汇总类单元格保留公式
   - 具体策略见第10.4节

7. ~~**睿景月结数据表模板**~~ ✅ 已确认
   - 已提供模板，按《税务台账生成逻辑_V2.0.xlsx》的【睿景景程月结数据表-报税】sheet执行

**优先级P2（优化阶段考虑）**：

7. **性能优化阈值**
   - 单月数据湖数据量达到多少行时需要分页处理？
   - 台账生成耗时超过多少秒需要异步处理？

### 12.2 技术准备建议

1. **环境准备**
   - 准备Azure Blob Storage的连接字符串
   - 配置MySQL数据库连接
   - 准备Okta认证配置（复用现有实现）

2. **模板文件整理**
   - 将现有Excel模板按Sheet页拆分为独立模板文件
   - 确认每个模板的格式要求
   - 建立模板版本管理机制

3. **测试数据准备**
   - 准备至少2家公司的完整测试数据（包含2320/2355和其他公司）
   - 准备至少3个月的连续数据（测试累积型Sheet页）
   - 准备各种边界情况数据（1月、月份间断、缺失文件等）

4. **数据湖API联调**
   - 首先进行数据湖API联调测试
   - 确认数据格式、分页逻辑、错误码
   - 建立Mock数据用于开发环境测试

5. **初始化与联调运维建议（补充）**
   - 提供5张配置表初始化SQL（每表约20条），并按 company_code 维度保持关联一致。
   - 初始化脚本需支持重复执行（避免唯一键冲突），建议先按业务键清理再插入。
   - 前后端联调固定端口映射（示例：前端5173代理后端8088），并在调试结束后关闭临时端口进程。

### 12.3 开发顺序建议

**第一阶段：基础设施与核心模块（P0）**
1. 数据库建表与实体类生成
2. Azure Blob Storage集成（复用现有BlobStorageRemote）
3. Okta认证集成（复用现有实现）
4. 简化权限模型实现（超级管理员+公司使用者）
5. 公司管理CRUD
1. 数据库建表与实体类生成
2. Azure Blob Storage集成
3. Okta认证集成（复用）
4. 权限管理框架
5. 公司管理CRUD

**第二阶段：文件管理与数据湖集成（P1）**
1. 文件上传/下载/列表接口
2. 数据湖API调用封装（参考InvoiceCommandService）
3. 科目拆分逻辑
4. 数据湖文件Aspose模板填充/公式重算/Excel生成（使用Aspose.Cells）
1. 文件上传/下载/列表接口
2. 数据湖API调用封装
3. 科目拆分逻辑
4. 数据湖文件Aspose模板填充/公式重算/Excel生成

**第三阶段：台账生成核心（P2）**
1. 直取型Sheet页生成器（BS、PL、数据湖明细等）
2. 配置表管理接口
3. 增值税变动表生成器（最复杂）
4. Summary表生成器

**第四阶段：累积型Sheet与Ref（P3）**
1. 累积型Sheet页生成器
2. Ref Summary表生成器
3. 台账组装器（合并所有Sheet）
4. 台账生成前置校验

**第五阶段：前端开发（P4）**
1. 首页（公司列表）
2. 公司详情页
3. 文件上传/管理界面
4. 数据湖拉取界面
5. 台账生成与下载界面
6. 配置管理界面（仅超级管理员）
7. 权限管理界面（超级管理员和公司授权用户）

**第六阶段：测试与优化（P5）**
1. 单元测试
2. 集成测试
3. 性能测试
4. 异常处理完善
5. 用户体验优化

---

## 附录A：关键业务规则速查

### A.1 公司类型区分

| 公司类型 | 配置表使用 | PL附表使用 | 增值税变动表逻辑 |
|---------|----------|-----------|----------------|
| 2320（上海睿景） | 特殊条目配置表 | PL附表-2320、2355 | 按6种税率拆分（专票13%/9%/6% + 普票13%/9%/6%） |
| 2355（上海景程） | 特殊条目配置表 | PL附表-2320、2355 | 按6种税率拆分（专票13%/9%/6% + 普票13%/9%/6%） |
| 其他公司（如3019） | 基础条目配置表 | PL附表-项目公司 | 按基础条目配置表计算 |

### A.2 PL附表拆分依据示例（2320/2355）

| 拆分依据 | 对应增值税销项表汇总条件 |
|---------|----------------------|
| 专票-13% | 发票类型=专用发票 且 税率=13% |
| 专票-9% | 发票类型=专用发票 且 税率=9% |
| 专票-6% | 发票类型=专用发票 且 税率=6% |
| 普票-13% | 发票类型=普通发票 且 税率=13% |
| 普票-9% | 发票类型=普通发票 且 税率=9% |
| 普票-6% | 发票类型=普通发票 且 税率=6% |
| 利息收入 | 固定资产处置收入（单独逻辑） |
| 其他收益计税金额 | 其他收益（单独逻辑） |

### A.3 累积型Sheet页首次生成规则

| Sheet页 | 首次生成（无上月台账） | 后续生成 |
|---------|---------------------|---------|
| 未开票数监控 | 从0开始 | 上月数据 + 当月增量 |
| 项目累计申报 | 从0开始 | 上月数据 + 当月增量 |
| 项目累计缴纳 | 从0开始 | 上月数据（不变化） |
| 累计税金汇总表 | 从0开始 | 上月数据 + 当月增量 |
| 增值税表-累计销项 | 从0开始 | 上月数据 + 当月增量 |
| 账税差异监控 | 从0开始 | 上月数据 + 当月BS/PL差异 |

### A.4 前置文件依赖检查表

| Sheet页 | 必需的输入文件 | 可选文件 |
|---------|--------------|---------|
| BS表 | BS表（用户上传） | BS附表 |
| PL表 | PL表（用户上传） | PL附表 |
| 增值税变动表 | PL附表、增值税销项表、增值税进项认证清单 | 上月台账 |
| Summary表 | PL表、BS表、数据湖明细、印花税明细、增值税进项认证清单 | - |
| 累积型Sheet | 上月台账、当月相应数据 | - |

---

## 附录B：技术实现速查

### B.1 Aspose.Cells常用代码片段

```java
// 读取Excel模板
Workbook wb = new Workbook(templatePath);
Worksheet ws = wb.getWorksheets().get("Sheet名称");

// 写入单元格
ws.getCells().get("B5").putValue("示例值");

// 保存Excel
wb.save(outputPath);
```

```java
// 基于模板批量填值（示例）
Workbook wb = new Workbook(templatePath);
Worksheet ws = wb.getWorksheets().get("Sheet名称");
Cells cells = ws.getCells();
for (int i = 0; i < rows.size(); i++) {
    cells.get(5 + i, 0).putValue(rows.get(i).getName());
    cells.get(5 + i, 1).putValue(rows.get(i).getAmount().doubleValue());
}
wb.save(outputPath);
```

### B.2 Aspose.Cells公式处理示例

```java
Workbook wb = new Workbook(templatePath);
Worksheet ws = wb.getWorksheets().get("Summary表");

// 设置公式
ws.getCells().get("E100").setFormula("=SUM(E2:E99)");

// 全工作簿公式重算
wb.calculateFormula();

// 可选：读取计算结果
Object value = ws.getCells().get("E100").getValue();

wb.save(outputPath);
```

```java
// 复制模板行（保留样式与公式）
Cells cells = ws.getCells();
cells.copyRow(cells, 10, 20); // 将第11行复制到第21行
```
### B.3 Azure Blob Storage常用代码

```java
// 上传文件
BlobClient blobClient = blobContainerClient.getBlobClient(blobPath);
blobClient.upload(file.getInputStream, fileSize);

// 下载文件
BlobClient blobClient = blobContainerClient.getBlobClient(blobPath);
InputStream inputStream = blobClient.openInputStream();

// 列出文件
ListBlobsOptions options = new ListBlobsOptions();
options.setPrefix(prefix);
blobContainerClient.listBlobs(options, Duration.ofSeconds(60));
```

### B.4 数据湖API调用示例（参考InvoiceCommandService）

```java
@Service
@RequiredArgsConstructor
public class DataLakeQueryService {
    private final PlatformRemote platformRemote;

    @Value("${custom.platform.token.domain}")
    private String platformDomain;

    /**
     * 查询数据湖会计凭证数据
     */
    public List<AccountingDocumentDTO> queryAccountingDocuments(
            String companyCode,
            String fiscalYearPeriodStart,
            String fiscalYearPeriodEnd
    ) {
        int offset = 0;
        int limit = 5000;

        String reqUrl = platformDomain + String.format(
                "/api/finance/electronicArchives/%s/%s/%s/%d/%d",
                companyCode, fiscalYearPeriodStart, fiscalYearPeriodEnd, offset, limit
        );

        return platformRemote.fetchFromDataLake(
                "FINANCE_ELECTRONICARCHIVES_SVC",
                reqUrl,
                AccountingDocumentDTO::fromPltData
        );
    }

    /**
     * 分页查询大数据量
     */
    public List<AccountingDocumentDTO> queryWithPagination(
            String companyCode,
            String fiscalYearPeriodStart,
            String fiscalYearPeriodEnd
    ) {
        List<AccountingDocumentDTO> allResults = new ArrayList<>();
        int offset = 0;
        int limit = 5000;

        while (true) {
            String reqUrl = platformDomain + String.format(
                    "/api/finance/electronicArchives/%s/%s/%s/%d/%d",
                    companyCode, fiscalYearPeriodStart, fiscalYearPeriodEnd, offset, limit
            );

            List<AccountingDocumentDTO> pageResults = platformRemote.fetchFromDataLake(
                    "FINANCE_ELECTRONICARCHIVES_SVC",
                    reqUrl,
                    AccountingDocumentDTO::fromPltData
            );

            if (CollectionUtils.isEmpty(pageResults)) {
                break;
            }

            allResults.addAll(pageResults);

            if (pageResults.size() < limit) {
                break;  // 最后一页
            }

            offset += limit;
        }

        return allResults;
    }
}
```

### B.5 权限校验拦截器示例

```java
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 获取当前用户工号（从Okta JWT中提取）
        String employeeId = SecurityContextHolder.getCurrentEmployeeId();

        // 判断是否是超级管理员
        if (isSuperAdmin(employeeId)) {
            return true;  // 超级管理员放行
        }

        // 获取请求的公司代码
        String companyCode = request.getParameter("companyCode");

        // 校验用户是否有该公司权限
        if (!hasCompanyPermission(employeeId, companyCode)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("无权限访问该公司数据");
            return false;
        }

        return true;
    }
}
```

| 序号 | 事项 | 状态 | 备注 |
|------|------|------|------|
| 1 | 前端技术栈确认 | 待确认 | 建议与现有脚手架一致 |
| 2 | 增值税变动表附表的详细取值逻辑 | ✅ 已确认 | 按PL附表的"拆分依据"列精确匹配，2320/2355按6种税率拆分，其他公司按基础条目计算 |
| 3 | 预开票收入计提冲回统计的详细逻辑 | ✅ 已确认 | 用户上传的文件，系统直接取用，无需计算 |
| 4 | 1月份台账生成时上年度12月台账数据来源 | ✅ 已确认 | 系统不支持跨年数据处理，1月份期初留抵进项税取0 |
| 5 | 台账Excel的具体格式/样式要求 | ✅ 已确认 | 需要与现有Excel模板格式完全一致 |


