package com.envision.epc.module.taxledger.application.parse;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class SheetParserRegistry {
    private final Map<FileCategoryEnum, SheetParser<?>> parserMap = new EnumMap<>(FileCategoryEnum.class);

    public SheetParserRegistry(List<SheetParser<?>> parsers) {
        for (SheetParser<?> parser : parsers) {
            SheetParser<?> existing = parserMap.putIfAbsent(parser.category(), parser);
            if (existing != null) {
                throw new BizException(ErrorCode.BAD_REQUEST,
                        "duplicate parser for category: " + parser.category().name());
            }
        }
    }

    public SheetParser<?> get(FileCategoryEnum category) {
        SheetParser<?> parser = parserMap.get(category);
        if (parser != null) {
            return parser;
        }
        return new UnsupportedSheetParser(category);
    }

    private static final class UnsupportedSheetParser implements SheetParser<Object> {
        private final FileCategoryEnum category;

        private UnsupportedSheetParser(FileCategoryEnum category) {
            this.category = category;
        }

        @Override
        public FileCategoryEnum category() {
            return category;
        }

        @Override
        public Class<Object> resultType() {
            return Object.class;
        }

        @Override
        public ParseResult<Object> parse(java.io.InputStream inputStream, ParseContext context) {
            ParseResult<Object> result = ParseResult.<Object>builder().build();
            result.addIssue("parser not implemented for category: " + category.name());
            return result;
        }
    }
}
