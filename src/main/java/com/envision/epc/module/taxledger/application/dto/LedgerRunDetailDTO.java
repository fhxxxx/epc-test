package com.envision.epc.module.taxledger.application.dto;

import com.envision.epc.module.taxledger.domain.LedgerRunStatusEnum;
import com.envision.epc.module.taxledger.domain.TaxLedgerRun;
import com.envision.epc.module.taxledger.domain.TaxLedgerRunArtifact;
import com.envision.epc.module.taxledger.domain.TaxLedgerRunStage;
import lombok.Data;

import java.util.List;

@Data
public class LedgerRunDetailDTO {
    private Long runId;
    private Long ledgerId;
    private Integer runNo;
    private LedgerRunStatusEnum status;
    private Integer currentBatch;
    private String errorCode;
    private String errorMsg;
    private List<TaxLedgerRunStage> stages;
    private List<TaxLedgerRunArtifact> artifacts;

    public static LedgerRunDetailDTO of(TaxLedgerRun run, List<TaxLedgerRunStage> stages, List<TaxLedgerRunArtifact> artifacts) {
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
