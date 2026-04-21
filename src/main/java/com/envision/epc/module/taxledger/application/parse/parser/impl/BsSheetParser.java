package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.envision.epc.module.taxledger.application.dto.BsStatementRowDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * BS（资产负债表）解析器。
 * 对应sheet页：资产负债表
 * 对应类别：FileCategoryEnum.BS
 */
@Component
public class BsSheetParser implements SheetParser<List<BsStatementRowDTO>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.BS;
    }

    @Override
    public Class<List<BsStatementRowDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<BsStatementRowDTO>> cls = (Class<List<BsStatementRowDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<BsStatementRowDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<BsStatementRowDTO>> result = ParseResult.<List<BsStatementRowDTO>>builder()
                .data(List.of())
                .build();
        try {
            List<BsStatementRowDTO> rows = EasyExcelFactory.read(inputStream)
                    .head(BsStatementRowDTO.class)
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

