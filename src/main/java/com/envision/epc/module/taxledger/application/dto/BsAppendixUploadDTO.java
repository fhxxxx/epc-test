package com.envision.epc.module.taxledger.application.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class BsAppendixUploadDTO {

    /** 公司 */
    @ExcelProperty("公司")
    private String companyCode;

    /** 总账科目 */
    @ExcelProperty("总账科目")
    private String glAccount;

    /** 短文本 */
    @ExcelProperty("短文本")
    private String shortText;

    /** 货币 */
    @ExcelProperty("货币")
    private String currency;

    /** 已结转余额 */
    @ExcelProperty("已结转余额")
    private String carriedForwardBalance;

    /** 累计余额 */
    @ExcelProperty("累计余额")
    private String cumulativeBalance;
}
