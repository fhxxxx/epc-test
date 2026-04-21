package com.envision.epc.module.taxledger.application.parse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParseContext {
    private String companyCode;
    private String yearMonth;
    private String fileName;
    private String operator;
    private String traceId;
}
