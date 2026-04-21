package com.envision.epc.module.taxledger.application.parse;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ParseMeta {
    private String sheetName;
    private List<String> headerSnapshot;
    private EngineType engineType;
    private String templateVersion;
    private Long elapsedMs;
}
