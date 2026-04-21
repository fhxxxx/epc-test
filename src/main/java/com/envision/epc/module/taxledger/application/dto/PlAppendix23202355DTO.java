package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PlAppendix23202355DTO {

    /** Section1：未开票/销项/已开票口径表 */
    private List<InvoicingSplitItem> invoicingSplitList;

    /** Section2：申报口径表 */
    private List<DeclarationSplitItem> declarationSplitList;

    @Data
    public static class InvoicingSplitItem {
        /** 拆分依据 */
        private String splitBasis;

        /** 未开票收入 */
        private BigDecimal uninvoicedIncome;

        /** 销项 */
        private BigDecimal outputTax;

        /** 已开票收入 */
        private BigDecimal invoicedIncome;

        /** 已开票销项 */
        private BigDecimal invoicedOutputTax;
    }

    @Data
    public static class DeclarationSplitItem {
        /** 拆分依据 */
        private String splitBasis;

        /** 申报金额 */
        private BigDecimal declaredAmount;

        /** 申报税额 */
        private BigDecimal declaredTaxAmount;
    }
}
