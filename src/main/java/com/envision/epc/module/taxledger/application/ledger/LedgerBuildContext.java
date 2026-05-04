package com.envision.epc.module.taxledger.application.ledger;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
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
    /** 当前台账构建所属公司编码（如 2320、2355、项目公司编码），用于分支规则与数据过滤。 */
    String companyCode;
    /** 当前构建账期，统一使用 yyyy-MM 口径（例如 2026-04）。 */
    String yearMonth;
    /** 当期可用文件记录集合（通常为该公司该账期上传文件），供 builder 按类别取数。 */
    List<FileRecord> files;
    /** 本次构建链路追踪标识，用于日志关联与排障定位。 */
    String traceId;
    /** 触发构建的操作人标识（系统任务场景通常为 system）。 */
    String operator;
    /** 解析结果读取网关，封装从解析产物中按类型读取对象/列表的能力。 */
    LedgerParsedDataGateway parsedDataGateway;
    /** 构建期配置快照（公司配置、税种配置、项目配置等），保证本次构建读取一致配置视图。 */
    LedgerConfigSnapshot configSnapshot;
    /** 预加载的解析结果缓存，key=文件分类，value=已反序列化对象或列表，避免重复读取存储。 */
    Map<FileCategoryEnum, Object> preloadedParsedData;
    /** 构建摘要信息容器，主要用于报告/日志输出（如 preload 统计、issues），不承载业务互通数据。 */
    Map<String, Object> preloadSummary;
    /** Summary 季度预取快照（印花税 I/J/K 月度值等跨月数据）。 */
    SummaryQuarterSnapshot summaryQuarterSnapshot;
    /** 构建期产物总线：已完成 builder 的 sheetData 映射，供后续 builder 直接读取前序产物。 */
    Map<LedgerSheetCode, LedgerSheetData> builtSheetDataMap;

    public boolean hasParsed(FileCategoryEnum category) {
        return preloadedParsedData != null && preloadedParsedData.containsKey(category);
    }

    public <T> T getParsedObject(FileCategoryEnum category, Class<T> type) {
        if (!hasParsed(category)) {
            return null;
        }
        Object value = preloadedParsedData.get(category);
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "预加载数据类型不匹配: " + category.name() + ", 期望=" + type.getSimpleName()
                            + ", 实际=" + value.getClass().getSimpleName());
        }
        return type.cast(value);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getParsedList(FileCategoryEnum category, Class<T> elementType) {
        if (!hasParsed(category)) {
            return List.of();
        }
        Object value = preloadedParsedData.get(category);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?>)) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "预加载数据类型不匹配: " + category.name() + ", 期望=List, 实际=" + value.getClass().getSimpleName());
        }
        List<?> list = (List<?>) value;
        for (Object item : list) {
            if (item != null && !elementType.isInstance(item)) {
                throw new BizException(ErrorCode.BAD_REQUEST,
                        "预加载数据元素类型不匹配: " + category.name() + ", 期望=" + elementType.getSimpleName()
                                + ", 实际=" + item.getClass().getSimpleName());
            }
        }
        return (List<T>) list;
    }

    public void putBuilt(LedgerSheetCode code, LedgerSheetData data) {
        if (builtSheetDataMap == null || code == null || data == null) {
            return;
        }
        builtSheetDataMap.put(code, data);
    }

    public <T extends LedgerSheetData> T getBuilt(LedgerSheetCode code, Class<T> type) {
        if (builtSheetDataMap == null || code == null || type == null) {
            return null;
        }
        LedgerSheetData data = builtSheetDataMap.get(code);
        if (data == null) {
            return null;
        }
        if (!type.isInstance(data)) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "构建产物类型不匹配: " + code.name() + ", 期望=" + type.getSimpleName()
                            + ", 实际=" + data.getClass().getSimpleName());
        }
        return type.cast(data);
    }

    public <T extends LedgerSheetData> T requireBuilt(LedgerSheetCode code, Class<T> type, LedgerSheetCode consumer) {
        T data = getBuilt(code, type);
        if (data == null) {
            String consumerName = consumer == null ? "UNKNOWN" : consumer.name();
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "缺少前置 builder 产物: producer=" + code.name() + ", consumer=" + consumerName);
        }
        return data;
    }
}
