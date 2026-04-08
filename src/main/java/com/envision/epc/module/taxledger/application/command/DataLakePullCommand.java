package com.envision.epc.module.taxledger.application.command;

import lombok.Data;

/**
 * 数据湖拉取命令
 */
@Data
public class DataLakePullCommand {
    /**
     * 公司代码
     */
    private String companyCode;
    /**
     * 账期，格式：yyyy-MM
     */
    private String yearMonth;
    /**
     * 财务期间开始值（数据湖接口参数）
     */
    private String fiscalYearPeriodStart;
    /**
     * 财务期间结束值（数据湖接口参数）
     */
    private String fiscalYearPeriodEnd;
}
