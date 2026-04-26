package com.envision.epc.module.taxledger.application.ledger;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.dto.PrecheckSnapshotDTO;
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
    String companyCode;
    String yearMonth;
    PrecheckSnapshotDTO snapshot;
    List<FileRecord> files;
    Map<String, Object> nodeOutputs;
    String traceId;
    String operator;
    LedgerParsedDataGateway parsedDataGateway;
    Map<FileCategoryEnum, Object> preloadedParsedData;
    Map<String, Object> preloadSummary;

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
        if (!(value instanceof List<?> list)) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "预加载数据类型不匹配: " + category.name() + ", 期望=List, 实际=" + value.getClass().getSimpleName());
        }
        for (Object item : list) {
            if (item != null && !elementType.isInstance(item)) {
                throw new BizException(ErrorCode.BAD_REQUEST,
                        "预加载数据元素类型不匹配: " + category.name() + ", 期望=" + elementType.getSimpleName()
                                + ", 实际=" + item.getClass().getSimpleName());
            }
        }
        return (List<T>) list;
    }
}
