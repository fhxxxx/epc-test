package com.envision.epc.module.taxledger.application.dto;

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
