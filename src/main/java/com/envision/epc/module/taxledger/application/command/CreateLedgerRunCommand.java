package com.envision.epc.module.taxledger.application.command;

import lombok.Data;

/**
 * 创建台账运行命令
 */
@Data
public class CreateLedgerRunCommand {
    /**
     * 公司代码
     */
    private String companyCode;
    /**
     * 账期，格式：yyyy-MM
     */
    private String yearMonth;
}
