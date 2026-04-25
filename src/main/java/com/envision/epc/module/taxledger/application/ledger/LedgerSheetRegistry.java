package com.envision.epc.module.taxledger.application.ledger;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sheet Builder/Renderer 注册中心
 */
@Component
@RequiredArgsConstructor
public class LedgerSheetRegistry {
    private final List<LedgerSheetDataBuilder<?>> builders;
    private final List<LedgerSheetRenderer<?>> renderers;
    private final SheetExecutionPlan executionPlan;
    private Map<LedgerSheetCode, LedgerSheetDataBuilder<?>> builderMap;
    private Map<LedgerSheetCode, LedgerSheetRenderer<?>> rendererMap;

    @PostConstruct
    public void validateAndBuildIndex() {
        this.builderMap = toUniqueBuilderMap(builders);
        this.rendererMap = toUniqueRendererMap(renderers);
        for (LedgerSheetCode code : executionPlan.allDefined()) {
            if (!builderMap.containsKey(code)) {
                throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "缺少Sheet Builder: " + code.getDisplayName());
            }
            if (!rendererMap.containsKey(code)) {
                throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "缺少Sheet Renderer: " + code.getDisplayName());
            }
        }
    }

    public LedgerSheetDataBuilder<?> requiredBuilder(LedgerSheetCode code) {
        LedgerSheetDataBuilder<?> builder = builderMap.get(code);
        if (builder == null) {
            throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "未注册Sheet Builder: " + code.getDisplayName());
        }
        return builder;
    }

    public LedgerSheetRenderer<?> requiredRenderer(LedgerSheetCode code) {
        LedgerSheetRenderer<?> renderer = rendererMap.get(code);
        if (renderer == null) {
            throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "未注册Sheet Renderer: " + code.getDisplayName());
        }
        return renderer;
    }

    private Map<LedgerSheetCode, LedgerSheetDataBuilder<?>> toUniqueBuilderMap(List<LedgerSheetDataBuilder<?>> source) {
        Map<LedgerSheetCode, LedgerSheetDataBuilder<?>> map = new HashMap<>();
        for (LedgerSheetDataBuilder<?> builder : source) {
            LedgerSheetDataBuilder<?> previous = map.putIfAbsent(builder.support(), builder);
            if (previous != null) {
                throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "重复Sheet Builder: " + builder.support().getDisplayName());
            }
        }
        return map;
    }

    private Map<LedgerSheetCode, LedgerSheetRenderer<?>> toUniqueRendererMap(List<LedgerSheetRenderer<?>> source) {
        Map<LedgerSheetCode, LedgerSheetRenderer<?>> map = new HashMap<>();
        for (LedgerSheetRenderer<?> renderer : source) {
            LedgerSheetRenderer<?> previous = map.putIfAbsent(renderer.support(), renderer);
            if (previous != null) {
                throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "重复Sheet Renderer: " + renderer.support().getDisplayName());
            }
        }
        return map;
    }
}
