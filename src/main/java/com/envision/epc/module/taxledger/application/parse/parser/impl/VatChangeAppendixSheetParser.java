package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 增值税变动表附表解析器。
 * 对应sheet页：增值税变动表附表
 * 对应类别：FileCategoryEnum.VAT_CHANGE_APPENDIX
 */
@Component
public class VatChangeAppendixSheetParser implements SheetParser<List<Map<String, BigDecimal>>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.VAT_CHANGE_APPENDIX;
    }

    @Override
    public Class<List<Map<String, BigDecimal>>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<Map<String, BigDecimal>>> cls = (Class<List<Map<String, BigDecimal>>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<Map<String, BigDecimal>>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<Map<String, BigDecimal>>> result = ParseResult.<List<Map<String, BigDecimal>>>builder()
                .data(List.of())
                .build();
        result.addIssue("增值税变动表附表 parser is initialized only");
        return result;
    }
}
