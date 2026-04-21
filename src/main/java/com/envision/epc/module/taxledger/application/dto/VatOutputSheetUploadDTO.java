package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VatOutputSheetUploadDTO {

    /** 发票明细表数据 */
    private List<InvoiceDetailItem> invoiceDetails;

    /** 税率（征收率）统计表数据 */
    private List<TaxRateSummaryItem> taxRateSummaries;

    @Data
    public static class InvoiceDetailItem {
        /** 序号 */
        private String serialNo;
        /** 发票代码 */
        private String invoiceCode;
        /** 发票号码 */
        private String invoiceNo;
        /** 数电发票号码 */
        private String digitalInvoiceNo;
        /** 销方识别号 */
        private String sellerTaxpayerId;
        /** 销方名称 */
        private String sellerName;
        /** 购方识别号 */
        private String buyerTaxpayerId;
        /** 购方名称 */
        private String buyerName;
        /** 开票日期 */
        private String invoiceDate;
        /** 税收分类编码 */
        private String taxClassificationCode;
        /** 特定业务类型 */
        private String specificBusinessType;
    }

    @Data
    public static class TaxRateSummaryItem {
        /** 序号 */
        private String serialNo;
        /** 发票状态 */
        private String invoiceStatus;
        /** 税率/征收率 */
        private BigDecimal taxRateOrLevyRate;
        /** 开具蓝字发票金额 */
        private BigDecimal blueInvoiceAmount;
        /** 开具蓝字发票税额 */
        private BigDecimal blueInvoiceTaxAmount;
        /** 作废蓝字发票金额 */
        private BigDecimal canceledBlueInvoiceAmount;
        /** 作废蓝字发票税额 */
        private BigDecimal canceledBlueInvoiceTaxAmount;
        /** 开具红字发票金额 */
        private BigDecimal redInvoiceAmount;
        /** 开具红字发票税额 */
        private BigDecimal redInvoiceTaxAmount;
        /** 作废红字发票金额 */
        private BigDecimal canceledRedInvoiceAmount;
        /** 作废红字发票税额 */
        private BigDecimal canceledRedInvoiceTaxAmount;
    }
}
