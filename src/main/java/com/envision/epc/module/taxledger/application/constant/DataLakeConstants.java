package com.envision.epc.module.taxledger.application.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 数据湖请求常量。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataLakeConstants {
    public static final String FINANCE_ELECTRONICARCHIVES_SVC = "finance-electronicarchives-datalakeit_chatenv";
    public static final String FINANCE_ELECTRONICARCHIVES_REQ_PATH_PATTERN = "/apis/finance/electronicarchives/datalakeit_chatenv/finance_electronicarchives_accounting_document_print?filter[company_code][EQ]={}&filter[fiscal_year_period][GE]={}&filter[fiscal_year_period][LE]={}&page[offset]={}&page[limit]={}&fields=company_code,fiscal_year_period,document_date,posting_date_in_the_document,document_type,document_print_number,document_header_text,number_of_line_item_within_accounting_document,account,debit_credit_indicator,credit_amount_in_local_currency,debit_amount_in_local_currency,currency_key_of_the_local_currency,credit_amount_in_document_currency,debit_amount_in_document_currency,currency_key_of_the_document_currency,item_text,payment_reference,company_id_of_trading_partner,customer_number,vendor_number,cost_center,wbs_element,reference,reverse_document_number,purchasing_document_number,reference_key_1_for_line_item,assignment,user_name";
}
