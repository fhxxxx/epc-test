# 台账 Sheet 分析报告 - 利润表

适用文件：`3019_江阴青芦新能源有限公司-资产负债表和利润表-重分类后.xlsx`  
适用范围：单 sheet（`利润表`）

---

## 1. 文档头信息

| 项目 | 内容 |
|---|---|
| Sheet名称 | 利润表 |
| Sheet编号 | 2 |
| 颜色标签 | 标蓝 |
| 功能标签 | 用户手工上传 |
| 是否纳入台账展示 | 是 |
| 适用公司范围 | 指定公司（3019） |
| 版本基线 | 3019_江阴青芦新能源有限公司-资产负债表和利润表-重分类后.xlsx |
| 分析日期 | 2026-04-17 |
| 分析人 | Codex |

---

## 2. 业务定位

- 本页为用户上传的 PL（利润表）原始输入页。
- 下游按本页字段读取“本期发生额/累计发生额”用于台账映射。
- 本页不做跨 sheet 自动取数。

---

## 3. 表头字段（按图片）

- 项目 -> `itemName`
- 行 -> `lineNo`
- 本期发生额 -> `currentPeriodAmount`
- 累计发生额 -> `accumulatedAmount`

---

## 4. Java对象（按表头）

```java
package com.envision.epc.module.taxledger.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PlStatementRowDTO {

    /** 项目 */
    private String itemName;

    /** 行 */
    private String lineNo;

    /** 本期发生额 */
    private BigDecimal currentPeriodAmount;

    /** 累计发生额 */
    private BigDecimal accumulatedAmount;
}
```

---

## 5. JSON结构化表达（jsonArray）

```json
[
  {
    "itemName": "一、营业总收入",
    "lineNo": "1",
    "currentPeriodAmount": null,
    "accumulatedAmount": null
  },
  {
    "itemName": "其中：营业收入",
    "lineNo": "2",
    "currentPeriodAmount": 1000000.00,
    "accumulatedAmount": 3500000.00
  },
  {
    "itemName": "主营业务收入",
    "lineNo": "3",
    "currentPeriodAmount": 850000.00,
    "accumulatedAmount": 3000000.00
  }
]
```
