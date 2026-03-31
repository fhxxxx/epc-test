package com.envision.bunny.module.extract.infrastructure;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author yakun.meng
 * @since 2024/5/6
 */
@NoArgsConstructor(access= AccessLevel.PRIVATE)
public class Constant {
    //数据湖员工信息服务标记
    public static final String FINANCE_ELECTRONICARCHIVES_SVC = "finance-electronicarchives-datalakeit_chatenv";
    public static final String FINANCE_ELECTRONICARCHIVES_REQ_PATH_PATTERN = "/apis/finance/electronicarchives/datalakeit_chatenv/finance_electronicarchives_accounting_document_print?filter[company_code][EQ]={}&filter[fiscal_year_period][GE]={}&filter[fiscal_year_period][LE]={}&filter[account][LIKE]=2221010100&filter[account][LIKE]=2221010400&page[offset]={}&page[limit]={}&fields=company_code,fiscal_year_period,posting_date_in_the_document,account,item_text,reference,debit_amount_in_local_currency,credit_amount_in_local_currency,accounting_document_number,company_id_of_trading_partner";
    public static final String DATE_PARAMETER = "&filter[posting_date_in_the_document][GE]={}&filter[posting_date_in_the_document][LE]={}";
}
