package com.envision.epc.module.taxledger.application.command;

import com.envision.epc.module.taxledger.domain.LedgerRunModeEnum;
import lombok.Data;

@Data
public class CreateLedgerRunCommand {
    private String companyCode;
    private String yearMonth;
    private LedgerRunModeEnum mode = LedgerRunModeEnum.AUTO;
}
