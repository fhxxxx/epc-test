# 台账 Sheet 分析报告 - Summary

适用文件：`税务台账生成逻辑_20260415V5.0(1).xlsx`  
适用范围：单 sheet（`Summary `，注意页签末尾有空格）

---

## 1. 文档头信息

| 项目       | 内容                            |
| -------- | ----------------------------- |
| Sheet名称  | Summary                       |
| Sheet编号  | 4                             |
| 颜色标签     | 标黄                            |
| 功能标签     | 汇总页（核心总览）                     |
| 是否纳入台账展示 | 是                             |
| 适用公司范围   | all                           |
| 版本基线     | 税务台账生成逻辑_20260415V5.0(1).xlsx |
| 分析日期     | 2026-04-20                    |
| 分析人      | Codex                         |

---

## 2. 重新分析（sheet真实结构）

该页不是单一表格，而是一个“总览汇总页”，按税种分段展示：

- 表头信息区（R1~R2）
  - 公司名称 + 台账年月说明
  - 申报日期
- 印花税区（约 R3~R17）
  - 税目明细 + 印花税合计
- 增值税区（约 R18~R30）
  - 税目明细 + 增值税合计 + 增值税附加 + 增值税附加合计
- 企业所得税区（约 R31~R33）
  - 项目明细 + 企业所得税合计
- 其他税费区（约 R34~R44）
  - 城镇土地使用税、房产税、个人所得税、水利建设专项收入、耕地占用税、契税等
- 全表合计区（R45）
  - “应申报合计”与账面合计

结论：`Summary` 不适合建成单一 row DTO，最合适的是“一个总对象 + 四段明细List对象（印花税/增值税/企业所得税/普通税种）”。

---

## 2.1 前三列生成逻辑（序号/申报税种/税目）

前三列统一来自 `税目配置表`，不再对印花税、增值税、企业所得税做特殊分支。

- 来源字段：
  - `序号` <- `税目配置表.序号`（仅用于排序，不直接回填到Summary）
  - `申报税种` <- `税目配置表.税种`
  - `税目` <- `税目配置表.税目`
- 过滤规则：
  - `税目配置表.公司代码 = 当前Summary公司代码` 或 `税目配置表.公司代码 为空`
- 唯一性前提：
  - `税种 + 税目` 在配置表中唯一，不存在重复命中场景
- 排序规则：
  - 按 `税目配置表.序号` 升序
  - 同一整数段内，小数明细排在整数合计前面（如 `2.1` 在 `2` 前）
- Summary序号回填规则（关键）：
  - Summary展示的 `序号` 不直接取配置表 `序号`
  - 先按“过滤 + 排序”得到最终行顺序，再生成展示序号
  - 仅整数税目行显示序号（合计类）
  - 小数明细税目行 `序号` 置空

### 2.2 本次实现变更同步（2026-04-29）

- `SummarySheetDTO` 已新增：`vatTaxRows`（增值税分段）。
- `commonTaxRows` 语义已调整为：仅承载**非增值税**公共税种。
- Builder 拆分规则（已落地）：
  - `taxType` 包含“增值税” -> 进入 `vatTaxRows`
  - 其余公共税种 -> 进入 `commonTaxRows`
- Renderer 当前实现（已更新）：
  - 分段独立渲染顺序：`印花税 -> 增值税 -> 企业所得税 -> 普通税种 -> 最终合计`
  - `commonTaxRows` 不再与 `vatTaxRows` 合并渲染
  - 普通税种段不再输出“小合计行”，仅保留最终大合计行

---

## 3. 数据来源（仅sheet名称）

- `Summary `
- `税目配置表`
- `公司代码配置表`
- `项目配置表`
- `增值税变动表`
- `增值税进项认证清单`
- `PL附表-2320、2355`
- `PL附表-项目公司`
- `PL表`
- `BS附表`
- `印花税明细-2320、2355`
- `印花税明细--非2320、2355`
- `销项明细`
- `进项明细`
- `其他科目明细`

---

## 4. 对象建模建议（按4大part）

采用“一个总对象 + 四个List”的固定结构：

- `stampDutyRows`：印花税段（对应 `A3~O3` 表头）
- `vatTaxRows`：增值税段（从公共税种中拆分，逻辑对象）
- `commonTaxRows`：非增值税公共税种段（对应 `A18~O18` 表头）
- `corporateIncomeTaxRows`：企业所得税段（对应 `C31~N31` 表头）

其中企业所得税4个季度字段采用“字段固定、表头动态”：

- 对象字段固定：`q1Tax`、`q2Tax`、`q3Tax`、`q4Tax`
- 实际表头展示动态：根据当前台账月份动态显示“应缴/已缴”等文案
- 映射不变：表头中 `Q2` 永远映射 `q2Tax`，`Q1` 永远映射 `q1Tax`，以此类推

---

## 5. Java对象（推荐结构）

```java
package com.envision.epc.module.taxledger.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SummarySheetDTO {

    /** 公司名称（表头） */
    private String companyName;

    /** 台账期间（如2025-02） */
    private String ledgerPeriod;

    /** 印花税分段 */
    private List<StampDutyItem> stampDutyRows;

    /** 增值税分段（从原commonTaxRows中拆分） */
    private List<CommonTaxItem> vatTaxRows;

    /** 公共税种分段（非增值税，除企业所得税） */
    private List<CommonTaxItem> commonTaxRows;

    /** 企业所得税分段 */
    private List<CorporateIncomeTaxItem> corporateIncomeTaxRows;

    /** 全表最终合计 */
    private FinalTotalItem finalTotal;

    @Data
    public static class StampDutyItem {
        private Integer seqNo;
        private String taxType;
        private String taxItem;
        private String taxBasisDesc;
        private BigDecimal taxBaseQuarter;
        private BigDecimal levyRatio;
        private BigDecimal taxRate;
        private BigDecimal actualTaxPayable;
        private BigDecimal taxBaseMonth1;
        private BigDecimal taxBaseMonth2;
        private BigDecimal taxBaseMonth3;
        private String varianceReason;
    }

    @Data
    public static class CommonTaxItem {
        private Integer seqNo;
        private String taxType;
        private String taxItem;
        private String taxBasisDesc;
        private BigDecimal taxBaseAmount;
        private BigDecimal levyRatio;
        private BigDecimal taxRate;
        private BigDecimal actualTaxPayable;
        private String accountCode;
        private BigDecimal bookAmount;
        private BigDecimal varianceAmount;
        private String varianceReason;
    }

    @Data
    public static class CorporateIncomeTaxItem {
        private String projectName;
        private String preferentialPeriod;
        private BigDecimal taxableIncome;
        private BigDecimal taxRate;
        private BigDecimal annualTaxPayable;
        /** 固定映射Q1（一季度） */
        private BigDecimal q1Tax;
        /** 固定映射Q2（二季度） */
        private BigDecimal q2Tax;
        /** 固定映射Q3（三季度） */
        private BigDecimal q3Tax;
        /** 固定映射Q4（四季度） */
        private BigDecimal q4Tax;
        private BigDecimal q1PayLastYearQ4;
        private BigDecimal lossCarryforwardUsed;
        private BigDecimal remainingLossCarryforward;
    }

    @Data
    public static class FinalTotalItem {
        private String totalTitle;
        private BigDecimal declaredTotal;
        private BigDecimal bookTotal;
    }
}
```

---

## 6. JSON结构化表达（推荐）

```json
{
  "companyName": "伊金霍洛旗悦盛新能源有限公司",
  "ledgerPeriod": "2025-02",
  "stampDutyRows": [
    {
      "seqNo": 1,
      "taxType": "印花税",
      "taxItem": "借款合同",
      "taxBasisDesc": "财务提供",
      "taxBaseQuarter": 0.00,
      "levyRatio": 1.0,
      "taxRate": 0.00005,
      "actualTaxPayable": 0.00,
      "taxBaseMonth1": null,
      "taxBaseMonth2": null,
      "taxBaseMonth3": null,
      "varianceReason": ""
    }
  ],
  "vatTaxRows": [
    {
      "seqNo": 2,
      "taxType": "增值税",
      "taxItem": "销项税额-主营业务收入",
      "taxBasisDesc": "主营业务收入",
      "taxBaseAmount": 0.00,
      "levyRatio": 1.0,
      "taxRate": 1.0,
      "actualTaxPayable": 0.00,
      "accountCode": "2221010100",
      "bookAmount": 0.00,
      "varianceAmount": 0.00,
      "varianceReason": ""
    }
  ],
  "commonTaxRows": [
    {
      "seqNo": 4,
      "taxType": "城建税",
      "taxItem": "城建税",
      "taxBasisDesc": "增值税应纳税额",
      "taxBaseAmount": 0.00,
      "levyRatio": 1.0,
      "taxRate": 0.07,
      "actualTaxPayable": 0.00,
      "accountCode": "2221010400",
      "bookAmount": 0.00,
      "varianceAmount": 0.00,
      "varianceReason": ""
    }
  ],
  "corporateIncomeTaxRows": [
    {
      "projectName": "项目A",
      "preferentialPeriod": "减半期",
      "taxableIncome": 0.00,
      "taxRate": 0.25,
      "annualTaxPayable": 0.00,
      "q1Tax": 0.00,
      "q2Tax": 0.00,
      "q3Tax": 0.00,
      "q4Tax": 0.00,
      "q1PayLastYearQ4": 0.00,
      "lossCarryforwardUsed": 0.00,
      "remainingLossCarryforward": 0.00
    }
  ],
  "finalTotal": {
    "totalTitle": "2025-02应申报合计",
    "declaredTotal": 0.00,
    "bookTotal": 0.00
  }
}
```

---

## 7. 建模结论

- `Summary` 采用“四段List + 顶层头信息/合计”最匹配实际表头结构。
- 企业所得税季度字段采用“字段固定（q1~q4）+ 表头动态文案”的方式，实现简单且稳定。

## 8. 动态表头映射规则（企业所得税）

- 对象字段固定：
  - `q1Tax` -> Q1
  - `q2Tax` -> Q2
  - `q3Tax` -> Q3
  - `q4Tax` -> Q4
- 表头动态生成：
  - 根据当前台账月份所属季度，动态生成“Qx应缴税额 / Qx已缴税额”展示文案
  - 只改展示名，不改字段映射关系
- 例：台账月份为 `2026-04` 时，表头可显示 `Q2应缴税额、Q1已缴税额、Q3应缴税额、Q4应缴税额`，字段仍映射到 `q2Tax、q1Tax、q3Tax、q4Tax`

---

## 9. 四大part字段取值逻辑（按渲染顺序）

以下规则与 `2.1 前三列生成逻辑` 联动执行：每一行先确定 `seqNo/taxType/taxItem`，再填后续业务字段。渲染顺序固定为：`印花税 -> 增值税 -> 企业所得税 -> 普通税种 -> 最终合计`。

### 9.1 印花税段（`stampDutyRows`）

#### 9.1.1 字段取值
- `seqNo/taxType/taxItem`：来自税目配置排序结果，序号按展示规则生成。
- `taxBasisDesc`：优先 `税目配置表.计税依据`，无值按业务固定文案。
- `taxBaseQuarter`：按税目汇总季度税基。
- `levyRatio`：取 `税目配置表.征收比例`，默认 `100%`。
- `taxRate`：取 `税目配置表.税率`。
- `actualTaxPayable`：`taxBaseQuarter * levyRatio * taxRate`。
- `taxBaseMonth1/2/3`：仅 `taxItem=买卖合同` 取值，来源为当前台账所属季度三个月 `PL` 的“主营业务收入-本期发生额”；其他税目置空。

#### 9.1.2 表头与渲染规则
- `H2` 写台账月份第一天（日期类型，格式由模板控制）。
- `I3/J3/K3` 动态表头：`计税金额-YYYY年MM月`（当前台账所属季度3个月）。
- 合并规则：
  - 申报税种列连续相同值纵向合并；
  - 序号列按申报税种分组纵向合并；
  - 计税依据列连续相同值纵向合并。
- 合计行规则：
  - 清空非汇总字段，不显示“小计”；
  - `实际应纳税额(D)` 使用明细求和公式。

### 9.2 增值税段（`vatTaxRows`）

#### 9.2.1 通用字段取值
- `seqNo/taxType/taxItem`：来自税目配置排序结果。
- `taxBasisDesc`：来自税目配置或规则文案。
- `taxBaseAmount(A)`：主要来自 `VAT_CHANGE`（优先 builder产物）。
- `levyRatio/taxRate`：来自税目配置。
- `actualTaxPayable(D)`：默认 `A * B * C`，特殊行按业务值覆盖。
- `accountCode/bookAmount`：按公司分支规则取值（见 9.2.4），非命中税目为空。
- `varianceAmount`：渲染时统一写公式 `D - 账面金额`。

#### 9.2.2 `taxBaseAmount(A)` 特殊规则
- `销项税额-主营业务收入*`：
  - 从 Summary 税目提取后缀（支持 `-后缀` 或 `(后缀)`）；
  - 同时提取税率 token；
  - 在 `VAT_CHANGE` builder 行中筛选：
    - `baseItem` 或 `itemName` 以 `当月利润表主营业务收入-` 开头；
    - 非普票；
    - 包含后缀；
    - 包含税率 token；
  - 命中多行累加 `totalAmount`；未命中按0并记录 `summaryBuildIssues`。
- 下列3个手工税目在渲染阶段改为跨 sheet 公式联动（引用“增值税变动表”同条目“合计”单元格）：
  - `销项税额-固定资产处置收入`
  - `销项税额-财务费用-利息收入`
  - `销项税额-其他收益`

#### 9.2.3 合计行规则
- 不显示“小计”文案；
- `实际应纳税额(D)` 优先使用该合计行业务值，无值时回退明细求和公式。

#### 9.2.4 账面金额（`bookAmount`）公司分支口径

- 公司代码 **不等于** `2320/2355`：
  - `销项税额-主营业务收入`：`DL_OUTPUT.documentAmountSum * -1`
  - `增值税进项税额`：`DL_INPUT.documentAmountSum`
  - `期末进项留抵金额`：`BS附表` 中“应交税费-增值税*”累计余额合计
  - 其他增值税税目：空
- 公司代码 **等于** `2320/2355`：
  - `销项税额-主营业务收入*`：`PL附表-2320/2355` 的 `declarationSplitList.declaredTaxAmount`，按“税目后缀 contains splitBasis”匹配并过滤普票后汇总
  - `增值税进项税额`：`DL_INPUT.documentAmountSum`
  - `期末进项留抵金额`：`BS附表` 中“应交税费-增值税*”累计余额合计
  - 其他增值税税目：空

### 9.3 企业所得税段（`corporateIncomeTaxRows`）

#### 9.3.1 字段取值
- `projectName/preferentialPeriod/taxableIncome/taxRate/annualTaxPayable`：按企业所得税配置和业务口径。
- 季度字段固定映射：`q1Tax/q2Tax/q3Tax/q4Tax`。
- 其余字段：`q1PayLastYearQ4/lossCarryforwardUsed/remainingLossCarryforward` 按业务输入。
- `CorporateIncomeTaxItem.seqNo` 用于合计行序号回填与渲染控制。

#### 9.3.2 动态表头与渲染规则
- 季度表头动态：
  - 当前台账月份所属季度排在最前，后续季度循环排列；
  - 季度已结束显示 `Qx已缴税额`，未结束显示 `Qx应缴税额`。
- 合并规则：
  - 表头行 + 明细行范围内，序号列与申报税种列纵向合并（不含小计行）。
- 合计行规则：
  - 使用业务数据替代模板占位（含序号/申报税种）；
  - 普通明细行不展示序号。

### 9.4 普通税种段（`commonTaxRows`，非增值税）

#### 9.4.1 字段取值
- `seqNo/taxType/taxItem/taxBasisDesc`：来自税目配置及文案。
- `taxBaseAmount`：按税基规则取值（不含增值税专段逻辑）。
- `levyRatio/taxRate`：来自税目配置。
- `actualTaxPayable`：`A * B * C`（无特殊覆盖时）。
- `accountCode/bookAmount`：按科目取账面值。
- `varianceAmount`：渲染时写公式 `D - 账面金额`。
- `varianceReason`：`varianceAmount != 0` 时必填，否则可空。

#### 9.4.2 渲染规则
- 不输出小计行；
- 明细行直接参与最终大合计。

### 9.5 最终合计（`finalTotal`）

#### 9.5.1 `totalTitle`
- 规则：按台账期间生成，如 `2025-02应申报合计`。
- 渲染：写入第一列合并单元格占位符 `{{finalTotalTitle}}`。

#### 9.5.2 `declaredTotal`
- 规则：汇总印花税、增值税、企业所得税、普通税种纳入申报口径的金额（以渲染小计/明细汇总公式为准）。

#### 9.5.3 `bookTotal`
- 规则：汇总纳入账面口径的账面金额列。
