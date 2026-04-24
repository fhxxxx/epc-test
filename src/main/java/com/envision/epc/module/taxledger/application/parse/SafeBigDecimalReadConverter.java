package com.envision.epc.module.taxledger.application.parse;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * BigDecimal 安全读取转换器：
 * - 支持会计格式下 '-' 文本按 0 处理
 * - 支持千分位与百分比文本
 * - 非法文本返回 null，不中断整文件解析
 */
@Slf4j
public class SafeBigDecimalReadConverter implements Converter<BigDecimal> {
    @Override
    public Class<?> supportJavaTypeKey() {
        return BigDecimal.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public BigDecimal convertToJavaData(ReadCellData<?> cellData,
                                        ExcelContentProperty contentProperty,
                                        GlobalConfiguration globalConfiguration) {
        if (cellData == null) {
            return null;
        }
        if (cellData.getType() == CellDataTypeEnum.NUMBER) {
            return cellData.getNumberValue();
        }

        String raw = cellData.getStringValue();
        BigDecimal parsed = BigDecimalNormalizeUtils.parse(raw);
        if (parsed == null && BigDecimalNormalizeUtils.isInvalidNumericText(raw)) {
            String field = contentProperty == null || contentProperty.getField() == null
                    ? "unknown"
                    : contentProperty.getField().getName();
            log.warn("INVALID_NUMERIC_TEXT: field={}, value={}", field, raw);
        }
        return parsed;
    }
}
