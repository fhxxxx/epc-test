package com.envision.epc.module.taxledger.application.ledger;

import com.envision.epc.module.taxledger.application.dto.PrecheckSnapshotDTO;
import com.envision.epc.module.taxledger.domain.FileRecord;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * 构建阶段上下文
 */
@Value
@Builder
public class LedgerBuildContext {
    String companyCode;
    String yearMonth;
    PrecheckSnapshotDTO snapshot;
    List<FileRecord> files;
    Map<String, Object> nodeOutputs;
    String traceId;
    String operator;
    LedgerParsedDataGateway parsedDataGateway;
}
