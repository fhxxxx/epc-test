package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.envision.epc.module.taxledger.application.dto.PlStatementRowDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * PL（利润表）解析器。
 * 对应sheet页：利润表
 * 对应类别：FileCategoryEnum.PL
 */
@Component
public class PlSheetParser implements SheetParser<List<PlStatementRowDTO>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.PL;
    }

    @Override
    public Class<List<PlStatementRowDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<PlStatementRowDTO>> cls = (Class<List<PlStatementRowDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<PlStatementRowDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<PlStatementRowDTO>> result = ParseResult.<List<PlStatementRowDTO>>builder()
                .data(List.of())
                .build();
        try {
            List<PlStatementRowDTO> rows = EasyExcelFactory.read(inputStream)
                    .head(PlStatementRowDTO.class)
                    .sheet()
                    .doReadSync();
            rows.removeIf(row -> isBlank(row.getItemName()) && isBlank(row.getLineNo()));
            result.setData(rows);
            return result;
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

