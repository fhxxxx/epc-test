package com.envision.epc.module.taxledger.application.command;

import lombok.Data;

/**
 * 创建台账任务命令
 */
@Data
public class CreateLedgerJobCommand {
    private String companyCode;
    private String yearMonth;
}
