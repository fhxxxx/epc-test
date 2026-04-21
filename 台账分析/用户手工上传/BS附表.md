# 台账 Sheet 分析报告 - BS附表

适用文件：`税务台账生成逻辑_20260415V5.0(1).xlsx`  
适用范围：单 sheet（`BS附表`）

---

## 1. 业务定位

- 本页属于**用户上传输入页**，用于提供 BS 相关补充数据。
- 从示例表头看，数据是“公司 + 科目 + 文本 + 币种 + 余额”结构，适合作为台账生成的原始输入 DTO。

---

## 2. 字段定义（基于表头）

表头识别（A~F）：

- 公司
- 总账科目
- 短文本
- 货币
- 已结转余额
- 累计余额

字段建议：

- `companyCode`: 公司代码（文本）
- `glAccount`: 总账科目（文本，保留前导零）
- `shortText`: 短文本（文本）
- `currency`: 货币（文本，如 RMB）
- `carriedForwardBalance`: 已结转余额（金额）
- `cumulativeBalance`: 累计余额（金额）

---

## 3. Java DTO 设计

```java
package com.envision.epc.module.taxledger.dto;

import lombok.Data;

@Data
public class BsAppendixUploadDTO {

    /** 公司 */
    private String companyCode;

    /** 总账科目 */
    private String glAccount;

    /** 短文本 */
    private String shortText;

    /** 货币 */
    private String currency;

    /** 已结转余额 */
    private String carriedForwardBalance;

    /** 累计余额 */
    private String cumulativeBalance;
}
```

---

## 4. 导入校验建议

- `companyCode`、`glAccount`、`currency` 建议必填。
- `glAccount` 必须按字符串处理，避免 Excel/程序转数字导致精度或前导零丢失。

---

## 5. JSON结构化表达（jsonArray）

该 sheet 推荐表达为 **JSON Array**，每一行对应一个对象。

### 5.1 字段英文名映射

- `companyCode`: 公司代码
- `glAccount`: 总账科目
- `shortText`: 短文本
- `currency`: 货币
- `carriedForwardBalance`: 已结转余额
- `cumulativeBalance`: 累计余额

### 5.2 结构化 JSON 示例

```json
[
  {
    "companyCode": "2320",
    "glAccount": "2221010100",
    "shortText": "",
    "currency": "RMB",
    "carriedForwardBalance": null,
    "cumulativeBalance": null
  },
  {
    "companyCode": "2320",
    "glAccount": "2221010200",
    "shortText": "",
    "currency": "RMB",
    "carriedForwardBalance": null,
    "cumulativeBalance": null
  }
]
```

### 5.3 空模板（用于程序生成）

```json
[
  {
    "companyCode": "",
    "glAccount": "",
    "shortText": "",
    "currency": "",
    "carriedForwardBalance": null,
    "cumulativeBalance": null
  }
]
```
