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
    private List<String> issues = new ArrayList<>();

    public void addIssue(String message) {
        issues.add(message);
    }

    public boolean hasError() {
        return !issues.isEmpty();
    }
}
