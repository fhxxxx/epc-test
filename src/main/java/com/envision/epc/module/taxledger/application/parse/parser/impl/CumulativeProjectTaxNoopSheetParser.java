package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 累计项目税收明细表占位解析器。
 * 对应类别：FileCategoryEnum.CUMULATIVE_PROJECT_TAX
 *
 * 说明：该类别文件当前仅存档，不做结构化解析。
 * 返回空结果且不记issue，避免触发解析失败状态。
 */
@Component
public class CumulativeProjectTaxNoopSheetParser implements SheetParser<Object> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.CUMULATIVE_PROJECT_TAX;
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

