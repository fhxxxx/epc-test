package com.envision.bunny.module.extract.application.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author gangxiang.guan
 * @date 2025/9/30 16:18
 */
@Data
public class AccountingDocumentDTO implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * reference : 20220722000001YW
     * fiscal_year_period : 2022007
     * caspian_id : 20746A2F757FB5614050D3CE97C6D643
     * debit_amount_in_local_currency : 1300
     * posting_date_in_the_document : 20220725
     * item_text : 20220722000001YW,蒋政费用报销
     * company_code : 3660
     * credit_amount_in_local_currency : 0
     * accounting_document_number : 4000000099
     * account : 6600080001业务招待费
     */
    /**
     * 参照
     */
    @JsonProperty("reference")
    private String reference;
    /**
     * 财政年/阶段
     */
    @JsonProperty("fiscal_year_period")
    private String fiscalYearPeriod;
    /**
     * 数据湖编号
     */
    @JsonProperty("caspian_id")
    private String caspianId;
    /**
     * 借方本币
     */
    @JsonProperty("debit_amount_in_local_currency")
    private BigDecimal debitAmountInLocalCurrency;
    /**
     * 凭证中的过账日期
     */
    @JsonProperty("posting_date_in_the_document")
    private String postingDateInTheDocument;
    /**
     * 行项目文本
     */
    @JsonProperty("item_text")
    private String itemText;
    /**
     * 公司代码
     */
    @JsonProperty("company_code")
    private String companyCode;
    /**
     * 贷方本币
     */
    @JsonProperty("credit_amount_in_local_currency")
    private BigDecimal creditAmountInLocalCurrency;
    /**
     * 会计凭证编号
     */
    @JsonProperty("accounting_document_number")
    private String accountingDocumentNumber;
    /**
     * 科目
     */
    @JsonProperty("account")
    private String account;
    /**
     * 贸易伙伴
     */
    @JsonProperty("company_id_of_trading_partner")
    private String companyIdOfTradingPartner;

    public static AccountingDocumentDTO fromPltData(JsonNode attributes) {
        ObjectMapper objectMapper = new ObjectMapper();
        // 忽略未知属性，不报错
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.convertValue(attributes, AccountingDocumentDTO.class);
    }
}
