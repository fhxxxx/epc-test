package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.envision.epc.module.taxledger.application.dto.DatalakeExportRowDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.SafeBigDecimalReadConverter;
import com.envision.epc.module.taxledger.application.parse.parser.ParserValueUtils;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.application.parse.parser.SheetSelectUtils;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * 数据湖明细通用解析器基类。
 * 适用于表头结构一致的 DL_* 文件类型。
 */
public abstract class AbstractDatalakeDetailSheetParser<T> implements SheetParser<T> {
    protected abstract Class<T> resultClass();

    protected abstract T aggregate(List<DatalakeExportRowDTO> rows);

    @Override
    public Class<T> resultType() {
        return resultClass();
    }

    @Override
    public ParseResult<T> parse(InputStream inputStream, ParseContext context) {
        ParseResult<T> result = ParseResult.<T>builder()
                .data(null)
                .build();
        try {
            byte[] bytes = inputStream.readAllBytes();
            String sheetName = SheetSelectUtils.resolveEasyExcelSheetName(bytes, category());
            List<DatalakeExportRowDTO> rows = EasyExcelFactory.read(new ByteArrayInputStream(bytes))
                    .registerConverter(new SafeBigDecimalReadConverter())
                    .head(DatalakeExportRowDTO.class)
                    .sheet(sheetName)
                    .doReadSync();
            if (CollectionUtils.isEmpty(rows)) {
                rows = List.of();
            }
            result.setData(aggregate(rows));
            return result;
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }
    }

    protected BigDecimal amount(String value) {
        BigDecimal parsed = ParserValueUtils.toBigDecimal(value);
        return parsed == null ? BigDecimal.ZERO : parsed;
    }

    protected String normalizedAccount(String account) {
        if (account == null) {
            return "";
        }
        return account.trim();
    }
}
