package com.envision.epc.module.taxledger.application.command;

import lombok.Data;

@Data
public class DataLakePullCommand {
    private String companyCode;
    private String yearMonth;
    private String fiscalYearPeriodStart;
    private String fiscalYearPeriodEnd;
}
