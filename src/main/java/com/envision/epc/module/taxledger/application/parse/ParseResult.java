package com.envision.epc.module.taxledger.application.parse;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ParseResult<T> {
    private T data;
    @Builder.Default
    private List<ParseIssue> issues = new ArrayList<>();
    private ParseMeta meta;

    public void addIssue(ParseSeverity severity, String code, String message) {
        issues.add(ParseIssue.builder().severity(severity).code(code).message(message).build());
    }

    public boolean hasError() {
        return issues.stream().anyMatch(it -> it.getSeverity() == ParseSeverity.ERROR);
    }
}
