package com.envision.epc.module.taxledger.application.command;

import lombok.Data;

/**
 * 人工确认批次命令
 */
@Data
public class ConfirmStageCommand {
    /**
     * 需要确认通过的批次号
     */
    private Integer batchNo;
}
