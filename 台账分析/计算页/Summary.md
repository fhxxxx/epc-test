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

结论：`Summary` 不适合建成单一 row DTO，最合适的是“一个总对象 + 三段明细List对象（印花税/公共税种/企业所得税）”。

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

## 4. 对象建模建议（按3段表头）

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

- `Summary` 采用“三段List + 顶层头信息/合计”最匹配实际表头结构。
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

## 9. 三大类税目字段取值逻辑（按表头顺序）

以下规则与 `2.1 前三列生成逻辑` 联动执行：每一行先确定 `seqNo/taxType/taxItem`，再填后续业务字段。

### 9.1 印花税段（`stampDutyRows`，对应A3~O3表头）

#### 9.1.1 `seqNo`（序号）
- 来源：`税目配置表.序号`（仅用于排序后重排）
- 规则：按过滤+排序后的最终顺序生成展示序号。
- 约束：整数合计行显示序号；小数明细行置空。

#### 9.1.2 `taxType`（申报税种）
- 来源：`税目配置表.税种`
- 规则：当前段固定为印花税相关税种行（包含“印花税合计”和印花税明细）。

#### 9.1.3 `taxItem`（税目）
- 来源：`税目配置表.税目`
- 规则：按配置顺序列示（如买卖合同、借款合同等）。

#### 9.1.4 `taxBasisDesc`（计税依据说明）
- 来源优先级：
  - `税目配置表.计税依据` 有值则优先；
  - 无值时按业务规则取固定说明（如“财务提供”）。

#### 9.1.5 `taxBaseQuarter`（计税金额(A)-季度）
- 规则：按当前税目对应的计税依据汇总本季度税基。
- 常见来源：合同印花税明细台账、业务台账或财务口径输入值。
- 合计行：为本段明细税目 `taxBaseQuarter` 求和。

#### 9.1.6 `levyRatio`（征收比例(B)）
- 来源：`税目配置表.征收比例`
- 规则：无特殊口径时默认 `100%`（即1.0）。

#### 9.1.7 `taxRate`（税率(C)）
- 来源：`税目配置表.税率`
- 规则：按税目固定税率填充（如0.005%、0.03%等）。

#### 9.1.8 `actualTaxPayable`（实际应缴税(D)）
- 规则：`taxBaseQuarter * levyRatio * taxRate`。
- 合计行：等于本段明细行 `actualTaxPayable` 之和。

#### 9.1.9 `taxBaseMonth1`（计税金额-月1）
- 列头映射：对应 `I` 列（`计税金额-YYYY年MM月`，季度第1个月）。
- 取值规则（新增明确口径）：
  - 仅当 `taxItem=买卖合同` 时取值；
  - 数据来源：对应公司代码、对应月份的 `PL表` 中“主营业务收入”的本期发生额；
  - 其他税目（非买卖合同）此字段置空。
- 示例：台账期 `2026-06`（Q2）时，月1对应 `2026-04`。

#### 9.1.10 `taxBaseMonth2`（计税金额-月2）
- 列头映射：对应 `J` 列（季度第2个月）。
- 取值规则（与9.1.9一致）：
  - 仅 `taxItem=买卖合同` 取 `PL表` 主营业务收入本期发生额（对应月）；
  - 其他税目置空。

#### 9.1.11 `taxBaseMonth3`（计税金额-月3）
- 列头映射：对应 `K` 列（季度第3个月）。
- 取值规则（与9.1.9一致）：
  - 仅 `taxItem=买卖合同` 取 `PL表` 主营业务收入本期发生额（对应月）；
  - 其他税目置空。

#### 9.1.12 `varianceReason`（差异原因）
- 规则：当申报口径与账面/来源口径存在差异时人工填写；
- 无差异时可为空。

### 9.2 公共税种段（`commonTaxRows`，对应A18~O18表头）

#### 9.2.1 `seqNo`（序号）
- 规则同 `9.1.1`：来自配置排序后的展示序号，明细行置空、合计行显示整数序号。

#### 9.2.2 `taxType`（申报税种）
- 来源：`税目配置表.税种`
- 范围：**非增值税**且非企业所得税税种。

> 说明：增值税行已独立放入 `vatTaxRows`，并在独立增值税段渲染；`commonTaxRows` 仅渲染普通税种。

#### 9.2.3 `taxItem`（税目）
- 来源：`税目配置表.税目`
- 规则：按配置行输出对应税目（如增值税进项税额、城建税等）。

#### 9.2.4 `taxBasisDesc`（计税依据/来源说明）
- 来源：`税目配置表.计税依据` 或黄色规则中的固定文案。
- 作用：说明该行税基来自哪个sheet/哪个业务字段。

#### 9.2.5 `taxBaseAmount`（计税金额(A)）
- 规则：按 `taxBasisDesc` 指向来源取值。
- 典型示例：
  - 增值税相关：取 `增值税变动表` 对应 `baseItem` 的 `totalAmount`；
  - 附加税相关：以上游增值税金额作为税基；
  - 其他税种：按 `Summary` 规则行或来源sheet汇总。

#### 9.2.6 `levyRatio`（征收比例(B)）
- 来源：`税目配置表.征收比例`
- 规则：默认100%，有减免或比例口径时按配置覆盖。

#### 9.2.7 `taxRate`（税率(C)）
- 来源：`税目配置表.税率`
- 规则：按税目固定税率取值。

#### 9.2.8 `actualTaxPayable`（实际应缴税(D)）
- 规则：`taxBaseAmount * levyRatio * taxRate`。
- 若该税目规则指定“直接取值”（非乘算），则按规则源字段直取。

#### 9.2.9 `accountCode`（记账科目）
- 来源：`税目配置表.会计科目`
- 规则：用于账税对比取数定位；可为空。

#### 9.2.10 `bookAmount`（账面金额）
- 规则：按 `accountCode` 到账务来源（如明细表/总账）取本期账面发生或累计值。

#### 9.2.11 `varianceAmount`（差异金额）
- 规则：`actualTaxPayable - bookAmount`。
- 用于监控申报与账面偏差。
- 渲染口径补充（已落地）：
  - 增值税明细行：差异列以公式写入 `实际应纳税额(D) - 账面金额`
  - 普通税种明细行：同样以公式写入 `实际应纳税额(D) - 账面金额`

#### 9.2.12 `varianceReason`（差异原因）
- 规则：`varianceAmount != 0` 时必填；
- `varianceAmount = 0` 时可为空。

### 9.3 企业所得税段（`corporateIncomeTaxRows`，对应C31~N31表头）

#### 9.3.1 `projectName`（项目/税种名称）
- 规则：取企业所得税段的行标识（公司/项目维度名称）。

#### 9.3.2 `preferentialPeriod`（优惠期/状态）
- 规则：按企业所得税政策口径标记（如手工、减免期、普通期等）。

#### 9.3.3 `taxableIncome`（应纳税所得额）
- 规则：按企业所得税口径计算出的应纳税所得额。

#### 9.3.4 `taxRate`（所得税率）
- 来源：企业所得税规则配置或行内固定税率（如2.40%等）。

#### 9.3.5 `annualTaxPayable`（当年度缴税额）
- 规则：当年累计应缴企业所得税金额。

#### 9.3.6 `q1Tax`（Q1税额）
- 字段语义固定为一季度。
- 展示文案可动态为“Q1应缴税额/已缴税额”，但字段映射不变。

#### 9.3.7 `q2Tax`（Q2税额）
- 字段语义固定为二季度。
- 展示文案可动态，但始终映射Q2列。

#### 9.3.8 `q3Tax`（Q3税额）
- 字段语义固定为三季度。

#### 9.3.9 `q4Tax`（Q4税额）
- 字段语义固定为四季度。

#### 9.3.10 `q1PayLastYearQ4`（Q1缴纳上年Q4金额）
- 规则：记录当年Q1缴纳的上年Q4税额。

#### 9.3.11 `lossCarryforwardUsed`（当年亏损所得税金额）
- 规则：记录当年用于抵减税额的亏损影响金额。

#### 9.3.12 `remainingLossCarryforward`（剩余可弥补亏损金额）
- 规则：记录本期后可继续结转弥补的亏损余额。

### 9.4 合计对象（`finalTotal`）

#### 9.4.1 `totalTitle`
- 规则：按台账期间生成标题，如 `2025-02应申报合计`。
- 渲染口径补充（已落地）：
  - 最终行标题写入第一列合并单元格占位符 `{{finalTotalTitle}}`
  - 不再写入中间列单元格

---

## 10. 差异更正与新增规则（2026-05-04）

本节用于修正文档与当前实现/确认口径的差异。

### 10.1 表头与全局占位符

- `H2` 日期单元格：
  - 不再写文本年月
  - 写“台账月份第一天”的日期值（date类型），由模板格式控制显示
- 企业所得税季度表头（`{{citQ1Label}}~{{citQ4Label}}`）：
  - 顺序规则：当前台账月份所属季度排第一，后续季度循环排列
  - 文案规则：季度已结束 -> `Qx已缴税额`；未结束 -> `Qx应缴税额`
- 印花税季度三个月表头（模板已改占位）：
  - `I3={{stampMonth1Label}}`、`J3={{stampMonth2Label}}`、`K3={{stampMonth3Label}}`
  - 命名口径：`计税金额-YYYY年MM月`，取当前台账所属季度的3个月（按先后顺序）

### 10.2 分段顺序与结构

- 分段渲染固定顺序：
  - 印花税 -> 增值税 -> 企业所得税 -> 普通税种 -> 最终合计
- 普通税种段：
  - 不输出小计行
  - 仅参与最终大合计

### 10.3 合计行与明细行口径

- 印花税合计行：
  - 多个字段置空（不显示“小计”文案）
  - `实际应纳税额(D)` 使用明细求和公式
- 增值税合计行：
  - 不显示“小计”文案
  - `实际应纳税额(D)` 优先使用该合计行业务值；无值时回退明细求和公式
- 企业所得税合计行：
  - 使用业务数据替代模板占位（含序号/申报税种）
  - 普通明细行不展示序号

### 10.4 合并规则补充

- 印花税明细：
  - 申报税种列连续相同值纵向合并
  - 序号列按申报税种分组纵向合并
  - 计税依据列连续相同值纵向合并
- 企业所得税段：
  - 表头行 + 明细行范围内，序号列与申报税种列纵向合并（不含小计行）

### 10.5 对象结构补充

- `SummarySheetDTO.CorporateIncomeTaxItem` 新增 `seqNo` 字段：
  - 用于企业所得税合计行序号回填及渲染控制

### 10.6 印花税 I/J/K 取值规则补充（2026-05-04）

- 这3列不仅表头动态，**字段值口径也有特定业务规则**：
  - `I/J/K` 分别对应台账月份所属季度的3个月；
  - 仅 `taxItem=买卖合同` 时，从对应月份 `PL表` 取“主营业务收入-本期发生额”；
  - 非买卖合同税目统一置空。
- 当前实现状态说明（文档与代码对齐）：
  - 截至本次更新，代码已实现 `I/J/K` 表头动态文案；
  - `I/J/K` 数值仍为占位值（尚未按上述口径取 `PL`），该部分待开发。

#### 9.4.2 `declaredTotal`
- 规则：汇总三段税目中纳入申报口径的 `actualTaxPayable`/季度应缴字段。

#### 9.4.3 `bookTotal`
- 规则：汇总对应税种的 `bookAmount` 或账面口径金额。
