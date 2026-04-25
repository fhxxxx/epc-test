package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 生成前校验快照
 */
@Data
public class PrecheckSnapshotDTO {
    private String companyCode;
    private String periodMonth;
    private List<String> requiredCategories;
    private List<InputItem> inputs;
    private PlAppendix23202355DTO n30NormalizedData;
    private Map<String, Object> validationDetails;
    private PreviousLedgerValidation previousLedgerValidation;
    private String fingerprint;
    private LocalDateTime generatedAt;

    @Data
    public static class InputItem {
        private Long fileId;
        private String fileName;
        private String fileCategory;
        private String parseStatus;
        private String parseResultBlobPath;
        private Long fileSize;
    }

    @Data
    public static class PreviousLedgerValidation {
        private String previousPeriodMonth;
        private Long previousLedgerRunId;
        private String previousLedgerArtifactPath;
        private List<String> checkedSheets;
        private List<String> issues;
        private Map<String, Object> parsedSummary;
    }
}
