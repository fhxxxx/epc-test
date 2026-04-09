package com.envision.epc.module.taxledger.application.dto;

import com.envision.epc.module.taxledger.domain.LedgerRunStatusEnum;
import com.envision.epc.module.taxledger.domain.LedgerRun;
import com.envision.epc.module.taxledger.domain.LedgerRunArtifact;
import com.envision.epc.module.taxledger.domain.LedgerRunStage;
import lombok.Data;

import java.util.List;

/**
 * 台账运行详情返回对象
 */
@Data
public class LedgerRunDetailDTO {
    /**
     * 运行ID
     */
    private Long runId;
    /**
     * 台账主记录ID
     */
    private Long ledgerId;
    /**
     * 第几次运行
     */
    private Integer runNo;
    /**
     * 运行状态
     */
    private LedgerRunStatusEnum status;
    /**
     * 当前批次
     */
    private Integer currentBatch;
    /**
     * 错误编码
     */
    private String errorCode;
    /**
     * 错误信息
     */
    private String errorMsg;
    /**
     * 批次阶段明细
     */
    private List<LedgerRunStage> stages;
    /**
     * 本次运行产物清单
     */
    private List<LedgerRunArtifact> artifacts;

    /**
     * 构建详情DTO
     */
    public static LedgerRunDetailDTO of(LedgerRun run, List<LedgerRunStage> stages, List<LedgerRunArtifact> artifacts) {
        LedgerRunDetailDTO dto = new LedgerRunDetailDTO();
        dto.setRunId(run.getId());
        dto.setLedgerId(run.getLedgerId());
        dto.setRunNo(run.getRunNo());
        dto.setStatus(run.getStatus());
        dto.setCurrentBatch(run.getCurrentBatch());
        dto.setErrorCode(run.getErrorCode());
        dto.setErrorMsg(run.getErrorMsg());
        dto.setStages(stages);
        dto.setArtifacts(artifacts);
        return dto;
    }
}
