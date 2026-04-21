# 台账 Sheet 分析报告 - PL附表-项目公司

适用文件：`税务台账生成逻辑_20260415V5.0(1).xlsx`  
适用范围：单 sheet（`PL附表-项目公司`）

---

## 1. 文档头信息

| 项目 | 内容 |
|---|---|
| Sheet名称 | PL附表-项目公司 |
| Sheet编号 | 17 |
| 颜色标签 | 标蓝 |
| 功能标签 | 用户手工上传 |
| 是否纳入台账展示 | 是 |
| 适用公司范围 | all |
| 版本基线 | 税务台账生成逻辑_20260415V5.0(1).xlsx |
| 分析日期 | 2026-04-16 |
| 分析人 | Codex |

---

## 2. 业务定位

- 本页用于承载项目公司 PL 附表的手工上传数据。
- 结构为轻量单表，适合作为导入 DTO 和 `jsonArray` 输入。

---

## 3. 表头与字段定义

基于当前 sheet 表头（第 2 行）识别：

- A列：`拆分依据`
- B列：`主营业务收入`
- C列：`销项`

字段英文名建议：

- `splitBasis`: 拆分依据（文本）
- `mainBusinessRevenue`: 主营业务收入（金额）
- `outputTax`: 销项（金额）

---

## 4. Java 对象（字段英文名 + 中文注释）

```java
package com.envision.epc.module.taxledger.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PlAppendixProjectCompanyUploadDTO {

    /** 拆分依据 */
    private String splitBasis;

    /** 主营业务收入 */
    private BigDecimal mainBusinessRevenue;

    /** 销项 */
    private BigDecimal outputTax;
}
```

---

## 5. JSON结构化表达（jsonArray）

该 sheet 推荐表达为 `jsonArray`，每行对应一条数据。

### 5.1 示例

```json
[
  {
    "splitBasis": "A",
    "mainBusinessRevenue": 15371.42,
    "outputTax": null
  },
  {
    "splitBasis": "B",
    "mainBusinessRevenue": 4569869.59,
    "outputTax": null
  },
  {
    "splitBasis": "C",
    "mainBusinessRevenue": null,
    "outputTax": null
  }
]
```

### 5.2 空模板

```json
[
  {
    "splitBasis": "",
    "mainBusinessRevenue": null,
    "outputTax": null
  }
]
```

---

## 6. 补充说明

- 当前页结构范围：`A1:C6`。
- 当前页无公式单元格、无错误单元格（如 `#VALUE!`）。
- 金额字段建议按数值类型导入，允许 `null`。
