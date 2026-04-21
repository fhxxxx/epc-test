package com.envision.epc.module.taxledger.application.parse.parser;

import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;

import java.io.InputStream;

public interface SheetParser<T> {
    FileCategoryEnum category();

    Class<T> resultType();

    ParseResult<T> parse(InputStream inputStream, ParseContext context);
}
