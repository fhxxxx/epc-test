package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.envision.epc.module.taxledger.application.dto.DatalakeExportRowDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;

import java.io.InputStream;
import java.util.List;

/**
 * 数据湖明细通用解析器基类。
 * 适用于表头结构一致的 DL_* 文件类型。
 */
public abstract class AbstractDatalakeDetailSheetParser implements SheetParser<List<DatalakeExportRowDTO>> {
    @Override
    public Class<List<DatalakeExportRowDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<DatalakeExportRowDTO>> cls = (Class<List<DatalakeExportRowDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<DatalakeExportRowDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<DatalakeExportRowDTO>> result = ParseResult.<List<DatalakeExportRowDTO>>builder()
                .data(List.of())
                .build();
        try {
            List<DatalakeExportRowDTO> rows = EasyExcelFactory.read(inputStream)
                    .head(DatalakeExportRowDTO.class)
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

