# EPC进项税报表系统 - 详细设计文档

> 版本：v1.1  
> 日期：2026-03-30  
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
| 权限控制 | 超级管理员+公司级权限的双层权限体系 |

### 1.3 技术栈

| 层级 | 技术选型 |
|------|---------|
| 后端框架 | Java Spring Boot 3.5.9 |
| 数据库 | MySQL |
| 文件存储 | Azure Blob Storage（现成实现，本地仅存path） |
| 认证 | Okta（现成实现） |
| 前端 | 待定（Vue3/React均可，建议使用现有脚手架中的方案） |
| Excel操作 | Apache POI + EasyExcel |

### 1.4 业务规模

| 维度 | 预估值 |
|------|--------|
| 公司数量 | 10~50家 |
| 用户数量 | 1~5人 |
| 台账Sheet页 | 最多26个 |
| 数据湖科目拆分 | 5类（收入/销项/进项/所得税/其他） |

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
│  │(Okta)    │  │(上传/下载/    │  │(Excel生成/      │    │
│  │          │  │ Blob存储)     │  │ 数据湖拉取)      │    │
│  └──────────┘  └──────────────┘  └─────────────────┘    │
│  ┌──────────┐  ┌──────────────┐  ┌─────────────────┐    │
│  │配置管理   │  │数据湖集成模块  │  │权限管理模块      │    │
│  │(5张配置表) │  │(API调用+     │  │(超级管理员+     │    │
│  │          │  │ 数据拆分)     │  │ 公司级权限)      │    │
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
| `company` | 公司CRUD、公司配置 | `CompanyController`, `CompanyService` |
| `file` | 文件上传、下载、Azure Blob管理 | `FileController`, `FileService`, `BlobStorageService` |
| `datalake` | 数据湖API调用、科目拆分、Excel生成 | `DataLakeController`, `DataLakeService`, `AccountSplitService` |
| `ledger` | 台账生成核心逻辑 | `LedgerController`, `LedgerService` |
| `config` | 5张配置表的CRUD管理 | `ConfigController`, `ConfigService` |
| `permission` | 权限管理（超管+公司级） | `PermissionController`, `PermissionService` |

### 2.3 目录结构

```
src/main/java/com/xxx/taxledger/
├── controller/
│   ├── AuthController.java
│   ├── CompanyController.java
│   ├── FileController.java
│   ├── DataLakeController.java
│   ├── LedgerController.java
│   ├── ConfigController.java
│   └── PermissionController.java
├── service/
│   ├── auth/
│   │   └── AuthService.java
│   ├── company/
│   │   └── CompanyService.java
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
│   ├── Company.java
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
│   └── PermissionLevelEnum.java       # 权限级别枚举
├── repository/
│   ├── CompanyRepository.java
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
Company (1) ──< FileRecord (N)
Company (1) ──< LedgerRecord (N)
Company (1) ──< UserPermission (N)

CompanyCodeConfig (N)  -- 独立配置表，与Company通过company_code关联
TaxCategoryConfig (N)
ProjectConfig (N)
VatBasicItemConfig (N)
VatSpecialItemConfig (N)
```

### 3.2 表结构

#### 3.2.1 公司表 `t_company`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| company_code | VARCHAR(20) | 公司代码 | UNIQUE, NOT NULL |
| company_name | VARCHAR(200) | 公司名称 | NOT NULL |
| finance_bp_ad | VARCHAR(100) | 财务BP AD号 | |
| finance_bp_name | VARCHAR(100) | 财务BP姓名 | |
| finance_bp_email | VARCHAR(200) | 财务BP邮箱 | |
| status | TINYINT | 状态(1启用/0禁用) | DEFAULT 1 |
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

#### 3.2.4 公司代码配置表 `t_company_code_config`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| company_code | VARCHAR(20) | 公司代码 | NOT NULL, UNIQUE |
| company_name | VARCHAR(200) | 公司名称 | NOT NULL |
| finance_bp_ad | VARCHAR(100) | 财务BP AD号 | |
| finance_bp_name | VARCHAR(100) | 财务BP姓名 | |
| finance_bp_email | VARCHAR(200) | 财务BP邮箱 | |
| created_by | VARCHAR(100) | 创建人 | |
| created_at | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| updated_at | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE |

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
| user_id | VARCHAR(100) | 用户ID（Okta userId） | NOT NULL, IDX |
| user_name | VARCHAR(100) | 用户姓名 | NOT NULL |
| permission_level | VARCHAR(20) | 权限级别 | NOT NULL |
| company_code | VARCHAR(20) | 公司代码（SUPER_ADMIN为空） | IDX |
| granted_by | VARCHAR(100) | 授权人 | |
| created_at | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |

**permission_level 枚举值**：
- `SUPER_ADMIN`：超级管理员，可访问所有公司、维护5张配置表
- `COMPANY_ADMIN`：公司管理员，可管理该公司用户权限
- `COMPANY_USER`：公司普通用户，可查看/操作该公司数据

**约束**：
- `permission_level=SUPER_ADMIN` 时，`company_code` 必须为 NULL
- `permission_level` 非 SUPER_ADMIN 时，`company_code` 不能为 NULL
- `UNIQUE(user_id, company_code, permission_level)`

---

## 四、API接口设计

### 4.1 接口规范

| 规范项 | 说明 |
|--------|------|
| 基础路径 | `/api/v1/tax-ledger` |
| 认证方式 | Okta Bearer Token（已有实现） |
| 响应格式 | 统一JSON `{code, message, data}` |
| 分页 | `page`, `size` 参数，返回 `{list, total, page, size}` |

### 4.2 接口清单

#### 4.2.1 认证模块

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/auth/current-user` | 获取当前登录用户信息 | 登录即可 |
| GET | `/auth/permissions` | 获取当前用户权限列表 | 登录即可 |

#### 4.2.2 公司管理模块

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/companies` | 获取公司列表 | 所有已认证用户 |
| POST | `/companies` | 新增公司 | SUPER_ADMIN |
| PUT | `/companies/{id}` | 修改公司信息 | SUPER_ADMIN |
| GET | `/companies/{companyCode}/detail` | 获取公司详情（含月份列表） | 该公司权限用户 |
| GET | `/companies/{companyCode}/months` | 获取该公司所有有数据的月份 | 该公司权限用户 |

#### 4.2.3 文件管理模块

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/companies/{companyCode}/months/{yearMonth}/files/upload` | 上传文件到指定公司月份 | 该公司权限用户 |
| GET | `/companies/{companyCode}/months/{yearMonth}/files` | 查询指定公司月份的文件列表 | 该公司权限用户 |
| DELETE | `/companies/{companyCode}/months/{yearMonth}/files/{fileId}` | 删除文件记录 | 该公司ADMIN或SUPER_ADMIN |
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
| GET | `/configs/company-code` | 查询公司代码配置表 | 所有已认证用户 |
| POST | `/configs/company-code` | 新增公司代码配置 | SUPER_ADMIN |
| PUT | `/configs/company-code/{id}` | 修改公司代码配置 | SUPER_ADMIN |
| DELETE | `/configs/company-code/{id}` | 删除公司代码配置 | SUPER_ADMIN |
| GET | `/configs/tax-category` | 查询税目配置表 | 所有已认证用户 |
| POST | `/configs/tax-category` | 新增税目配置 | SUPER_ADMIN |
| PUT | `/configs/tax-category/{id}` | 修改税目配置 | SUPER_ADMIN |
| DELETE | `/configs/tax-category/{id}` | 删除税目配置 | SUPER_ADMIN |
| GET | `/configs/project` | 查询项目配置表 | 所有已认证用户 |
| POST | `/configs/project` | 新增项目配置 | SUPER_ADMIN |
| PUT | `/configs/project/{id}` | 修改项目配置 | SUPER_ADMIN |
| DELETE | `/configs/project/{id}` | 删除项目配置 | SUPER_ADMIN |
| GET | `/configs/vat-basic-item` | 查询增值税变动表基础条目配置 | 所有已认证用户 |
| POST | `/configs/vat-basic-item` | 新增增值税变动表基础条目配置 | SUPER_ADMIN |
| PUT | `/configs/vat-basic-item/{id}` | 修改增值税变动表基础条目配置 | SUPER_ADMIN |
| DELETE | `/configs/vat-basic-item/{id}` | 删除增值税变动表基础条目配置 | SUPER_ADMIN |
| GET | `/configs/vat-special-item` | 查询增值税变动表特殊条目配置 | 所有已认证用户 |
| POST | `/configs/vat-special-item` | 新增增值税变动表特殊条目配置 | SUPER_ADMIN |
| PUT | `/configs/vat-special-item/{id}` | 修改增值税变动表特殊条目配置 | SUPER_ADMIN |
| DELETE | `/configs/vat-special-item/{id}` | 删除增值税变动表特殊条目配置 | SUPER_ADMIN |

#### 4.2.7 权限管理模块

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/permissions/users` | 获取所有用户权限列表 | SUPER_ADMIN |
| POST | `/permissions/super-admin` | 设置超级管理员 | SUPER_ADMIN |
| DELETE | `/permissions/super-admin/{userId}` | 移除超级管理员 | SUPER_ADMIN |
| GET | `/companies/{companyCode}/permissions/users` | 获取该公司用户权限列表 | 该公司ADMIN或SUPER_ADMIN |
| POST | `/companies/{companyCode}/permissions` | 给公司用户赋权限 | 该公司ADMIN或SUPER_ADMIN |
| DELETE | `/companies/{companyCode}/permissions/{permissionId}` | 移除公司用户权限 | 该公司ADMIN或SUPER_ADMIN |

---

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
              │ Step 5: 组装台账Excel    │
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

### 5.3 台账Sheet页生成优先级与依赖关系

#### 5.3.1 Sheet页分类

| 分类 | Sheet页 | 数据依赖 | 说明 |
|------|---------|---------|------|
| **类型A：直取型** | BS表 | 当月上传的BS表 | 取1月至台账月的BS表，按月拼接 |
| | PL表 | 当月上传的PL表 | 取1月至台账月的PL表，按月拼接 |
| | BS附表-应交税费 | 当月上传的BS附表 | 同BS逻辑 |
| | PL附表 | 当月上传的PL附表 | 同PL逻辑 |
| | 销项明细 | 数据湖DL_OUTPUT | 直接取当月数据 |
| | 进项明细 | 数据湖DL_INPUT | 直接取当月数据 |
| | 收入明细 | 数据湖DL_INCOME | 直接取当月数据 |
| | 所得税明细 | 数据湖DL_INCOME_TAX | 直接取当月数据 |
| | 其他科目明细 | 数据湖DL_OTHER | 直接取当月数据 |
| | 印花税明细 | 用户上传STAMP_TAX | 直接取当月数据 |
| | 增值税销项 | 用户上传VAT_OUTPUT | 直接取当月数据 |
| | 增值税进项认证清单 | 用户上传VAT_INPUT_CERT | 直接取当月数据 |
| | 累计项目税收明细表 | 用户上传CUMULATIVE_PROJECT_TAX | 直接取当月数据 |
| **类型B：计算型** | 增值税变动表 | 配置表+PL附表+增值税销项+增值税进项认证清单+收入明细+上月台账增值税变动表 | 需读取配置和上月台账 |
| | Summary表 | 配置表+PL表+BS表+数据湖明细+印花税明细+增值税进项认证清单 | 需读取多个数据源 |
| | Ref Summary表 | 同Summary+配置表 | 需读取配置表 |
| **类型C：累积型** | 未开票数监控 | 上月台账未开票数监控+当月PL表+数据湖明细 | **必须依赖上月台账** |
| | 项目累计申报 | 上月台账项目累计申报+当月Summary表 | **必须依赖上月台账** |
| | 项目累计缴纳 | 上月台账项目累计缴纳 | **必须依赖上月台账** |
| | 累计税金汇总表 | 上月台账累计税金汇总表+当月Summary表 | **必须依赖上月台账** |
| | 增值税表-累计销项 | 上月台账+当月数据湖明细 | **必须依赖上月台账** |
| | 账税差异监控 | 上月台账+当月BS表+PL表 | **必须依赖上月台账** |
| | 预开票收入计提冲回统计 | 上月台账+当月PL附表 | **必须依赖上月台账** |

#### 5.3.2 Sheet页生成顺序

```
第一批（无依赖，可并行生成）：
  BS表、PL表、BS附表、PL附表
  销项明细、进项明细、收入明细、所得税明细、其他科目明细
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

```
输入：
  - 公司代码 companyCode
  - 当月 PL表/PL附表
  - 当月 增值税销项表
  - 当月 增值税进项认证清单
  - 当月 收入明细（数据湖）
  - 上月台账的增值税变动表
  - 增值税变动表基础条目配置表
  - 睿景景程增值税变动表特殊条目配置表

判断逻辑：
  IF companyCode IN (2320, 2355):
    使用特殊条目配置表，按6种税率（专票13%/9%/6% + 普票13%/9%/6%）分别计算
    当月开票金额 → 取增值税销项表按税率+发票类型匹配
    合计金额 → 取PL附表按拆分依据匹配
  ELSE:
    使用基础条目配置表
    当月开票金额 → 取增值税销项表合计行
    合计金额 → 取PL附表或PL表
  END IF

  期初留抵进项税 → 取上月台账增值税变动表期末留抵进项税合计
    （1月份取上年度12月台账，如无则取0）

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
│  └── 权限管理（SUPER_ADMIN或公司ADMIN可见）          │
├──────────────────────────────────────────────────┤
│                                                  │
│                   主内容区                          │
│                                                  │
└──────────────────────────────────────────────────┘
```

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
│  □ 其他科目明细                │
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

### 6.7 配置管理页（仅SUPER_ADMIN）

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
│  ┌──────────┬──────────┬────────────┬─────────────────┐ │
│  │ 用户姓名   │ 权限级别   │ 授权人      │ 操作            │ │
│  ├──────────┼──────────┼────────────┼─────────────────┤ │
│  │ 王五     │ 公司管理员 │ 张三       │ [修改权限][移除] │ │
│  │ 赵六     │ 普通用户   │ 王五       │ [修改权限][移除] │ │
│  └──────────┴──────────┴────────────┴─────────────────┘ │
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

COMPANY_ADMIN（公司管理员）
├── 可访问指定公司
├── 可管理该公司下的用户权限
├── 可上传/下载该公司文件
├── 可拉取数据湖、生成台账
└── 不可修改配置表

COMPANY_USER（公司普通用户）
├── 可访问指定公司
├── 可上传/下载该公司文件
├── 可拉取数据湖、生成台账
└── 不可管理权限、不可修改配置表
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
        
        // 3. 校验逻辑
        boolean isSuperAdmin = permissions.stream()
            .anyMatch(p -> "SUPER_ADMIN".equals(p.getPermissionLevel()));
        
        if (isSuperAdmin) return true;
        
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

### 9.2 台账生成完整性校验

生成完成后进行后置校验：

```
1. 检查所有Sheet页是否都已生成
2. 检查是否有#REF!、#DIV/0!等公式错误
3. 检查累积型Sheet页的数据是否与上月衔接
4. 检查增值税变动表的应交增值税计算是否正确
5. 校验通过 → 状态设为SUCCESS
   校验失败 → 状态设为FAILED，记录错误信息
```

---

## 十、开发里程碑建议

| 阶段 | 内容 | 预估工作量 |
|------|------|-----------|
| P0 - 基础框架 | 项目搭建、数据库建表、认证集成、权限框架 | 3天 |
| P1 - 文件管理 | 公司CRUD、文件上传/下载/列表、Azure Blob集成 | 3天 |
| P2 - 数据湖集成 | API调用、科目拆分、Excel生成 | 3天 |
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
| 2 | 增值税变动表附表的详细取值逻辑 | 待补充 | 需查看Excel中该Sheet的具体逻辑说明 |
| 3 | 预开票收入计提冲回统计的详细逻辑 | 待补充 | 需查看Excel中该Sheet的具体逻辑说明 |
| 4 | 1月份台账生成时上年度12月台账数据来源 | 待确认 | 是否需要支持跨年台账导入？ |
| 5 | 台账Excel的具体格式/样式要求 | 待确认 | 是否需要与现有Excel模板完全一致？ |
