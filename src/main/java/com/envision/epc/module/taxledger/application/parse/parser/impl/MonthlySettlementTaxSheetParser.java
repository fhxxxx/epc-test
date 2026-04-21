package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.application.dto.MonthlyTaxSectionDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 睿景景程月结数据表-报税解析器。
 * 对应sheet页：睿景景程月结数据表-报税
 * 对应类别：FileCategoryEnum.MONTHLY_SETTLEMENT_TAX
 */
@Component
public class MonthlySettlementTaxSheetParser implements SheetParser<List<MonthlyTaxSectionDTO>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.MONTHLY_SETTLEMENT_TAX;
    }

    @Override
    public Class<List<MonthlyTaxSectionDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<MonthlyTaxSectionDTO>> cls = (Class<List<MonthlyTaxSectionDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<MonthlyTaxSectionDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<MonthlyTaxSectionDTO>> result = ParseResult.<List<MonthlyTaxSectionDTO>>builder()
                .data(List.of())
                .build();
        result.addIssue("睿景景程月结数据表-报税 parser is initialized only");
        return result;
    }
}
