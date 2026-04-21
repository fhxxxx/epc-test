package com.envision.epc.module.taxledger.application.dto;

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
