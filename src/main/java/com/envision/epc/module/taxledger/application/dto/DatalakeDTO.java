package com.envision.epc.module.taxledger.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 数据湖会计凭证 DTO（按 fields 输出字段定义）
 */
@Data
public class DatalakeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 公司代码 */
    @JsonProperty("company_code")
    private String companyCode;

    /** 财政年度/期间 */
    @JsonProperty("fiscal_year_period")
    private String fiscalYearPeriod;

    /** 凭证日期 */
    @JsonProperty("document_date")
    private String documentDate;

    /** 过账日期 */
    @JsonProperty("posting_date_in_the_document")
    private String postingDateInTheDocument;

    /** 凭证类型 */
    @JsonProperty("document_type")
    private String documentType;

    /** 凭证编号 */
    @JsonProperty("document_print_number")
    private String documentPrintNumber;

    /** 抬头文本 */
    @JsonProperty("document_header_text")
    private String documentHeaderText;

    /** 行项目号 */
    @JsonProperty("number_of_line_item_within_accounting_document")
    private String numberOfLineItemWithinAccountingDocument;

    /** 总账科目 */
    @JsonProperty("account")
    private String account;

    /** 借贷标识（S借方/H贷方） */
    @JsonProperty("debit_credit_indicator")
    private String debitCreditIndicator;

    /** 本币贷方金额 */
    @JsonProperty("credit_amount_in_local_currency")
    private BigDecimal creditAmountInLocalCurrency;

    /** 本币借方金额 */
    @JsonProperty("debit_amount_in_local_currency")
    private BigDecimal debitAmountInLocalCurrency;

    /** 本币币种 */
    @JsonProperty("currency_key_of_the_local_currency")
    private String currencyKeyOfTheLocalCurrency;

    /** 凭证币贷方金额 */
    @JsonProperty("credit_amount_in_document_currency")
    private BigDecimal creditAmountInDocumentCurrency;

    /** 凭证币借方金额 */
    @JsonProperty("debit_amount_in_document_currency")
    private BigDecimal debitAmountInDocumentCurrency;

    /** 凭证币币种 */
    @JsonProperty("currency_key_of_the_document_currency")
    private String currencyKeyOfTheDocumentCurrency;

    /** 行文本 */
    @JsonProperty("item_text")
    private String itemText;

    /** 付款参考 */
    @JsonProperty("payment_reference")
    private String paymentReference;

    /** 贸易伙伴 */
    @JsonProperty("company_id_of_trading_partner")
    private String companyIdOfTradingPartner;

    /** 客户 */
    @JsonProperty("customer_number")
    private String customerNumber;

    /** 供应商 */
    @JsonProperty("vendor_number")
    private String vendorNumber;

    /** 成本中心 */
    @JsonProperty("cost_center")
    private String costCenter;

    /** WBS */
    @JsonProperty("wbs_element")
    private String wbsElement;

    /** 参考 */
    @JsonProperty("reference")
    private String reference;

    /** 冲销凭证号 */
    @JsonProperty("reverse_document_number")
    private String reverseDocumentNumber;

    /** 采购凭证号 */
    @JsonProperty("purchasing_document_number")
    private String purchasingDocumentNumber;

    /** 参考代码1 */
    @JsonProperty("reference_key_1_for_line_item")
    private String referenceKey1ForLineItem;

    /** 分配 */
    @JsonProperty("assignment")
    private String assignment;

    /** 用户名 */
    @JsonProperty("user_name")
    private String userName;

    public static DatalakeDTO fromPltData(JsonNode attributes) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.convertValue(attributes, DatalakeDTO.class);
    }
}
