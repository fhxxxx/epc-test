package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VatChangeAppendixUploadDTO {
    /** 本期抵减预缴 */
    private BigDecimal currentPeriodPrepaidDeduction;
}

