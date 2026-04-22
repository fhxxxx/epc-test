package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.envision.epc.module.taxledger.application.dto.StampDutySummaryRowDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 印花税明细解析器。
 * 对应sheet页：印花税明细-2320、2355
 * 对应类别：FileCategoryEnum.STAMP_TAX
 */
@Component
public class StampTaxSheetParser implements SheetParser<List<StampDutySummaryRowDTO>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.STAMP_TAX;
    }

    @Override
    public Class<List<StampDutySummaryRowDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<StampDutySummaryRowDTO>> cls = (Class<List<StampDutySummaryRowDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<StampDutySummaryRowDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<StampDutySummaryRowDTO>> result = ParseResult.<List<StampDutySummaryRowDTO>>builder()
                .data(List.of())
                .build();
        try {
            List<StampDutySummaryRowDTO> rows = EasyExcelFactory.read(inputStream)
                    .head(StampDutySummaryRowDTO.class)
                    .sheet()
                    .doReadSync();
            result.setData(rows);
            return result;
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }
    }
}
