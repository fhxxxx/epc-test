package com.envision.epc.module.taxledger.application.parse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParseIssue {
    private ParseSeverity severity;
    private String code;
    private String message;
    private String cellRef;
}
