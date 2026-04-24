package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 预开票收入计提及冲回统计（2320、2355）占位解析器。
 * 对应类别：FileCategoryEnum.PREINVOICE_ACCRUAL_REVERSAL_2320_2355
 *
 * 说明：该类别当前仅存档，不做结构化解析。
 */
@Component
public class PreinvoiceAccrualReversalNoopSheetParser implements SheetParser<Object> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.PREINVOICE_ACCRUAL_REVERSAL_2320_2355;
    }

    @Override
    public Class<Object> resultType() {
        return Object.class;
    }

    @Override
    public ParseResult<Object> parse(InputStream inputStream, ParseContext context) {
        return ParseResult.<Object>builder()
                .data(null)
                .build();
    }
}
