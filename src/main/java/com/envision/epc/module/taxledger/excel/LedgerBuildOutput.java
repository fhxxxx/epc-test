package com.envision.epc.module.taxledger.excel;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class LedgerBuildOutput {
    byte[] workbookBytes;
    Map<String, Object> buildReport;
}
