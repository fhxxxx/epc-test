package com.envision.epc.module.taxledger.application.command;

import com.envision.epc.module.taxledger.domain.LedgerRunModeEnum;
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
    /**
     * 运行模式：AUTO / GATED
     */
    private LedgerRunModeEnum mode = LedgerRunModeEnum.AUTO;
}
