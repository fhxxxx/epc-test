package com.envision.epc.module.taxledger.application.parse;

import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class SheetParseService {
    private final SheetParserRegistry registry;

    public ParseResult<?> parse(InputStream inputStream, FileCategoryEnum category, ParseContext context) {
        SheetParser<?> parser = registry.get(category);
        return parser.parse(inputStream, context);
    }
}

