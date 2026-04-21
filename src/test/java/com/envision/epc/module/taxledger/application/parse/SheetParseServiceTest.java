package com.envision.epc.module.taxledger.application.parse;

import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.application.parse.parser.impl.BsAppendixSheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

class SheetParseServiceTest {

    @Test
    void shouldReturnErrorWhenInputStreamIsEmpty() {
        SheetParser<?> parser = new BsAppendixSheetParser();
        SheetParserRegistry registry = new SheetParserRegistry(List.of(parser));
        SheetParseService service = new SheetParseService(registry);

        ParseResult<?> result = service.parse(
                new ByteArrayInputStream(new byte[0]),
                FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE,
                ParseContext.builder().companyCode("2320").yearMonth("2025-12").build()
        );

        Assertions.assertTrue(result.hasError());
        Assertions.assertTrue(result.getIssues().stream().anyMatch(it -> "EMPTY_FILE".equals(it.getCode())));
    }

    @Test
    void shouldFallbackToUnsupportedParserForUnimplementedCategory() {
        SheetParserRegistry registry = new SheetParserRegistry(List.of(new BsAppendixSheetParser()));
        SheetParseService service = new SheetParseService(registry);

        ParseResult<?> result = service.parse(
                new ByteArrayInputStream("abc".getBytes()),
                FileCategoryEnum.SUMMARY,
                ParseContext.builder().companyCode("2320").yearMonth("2025-12").build()
        );

        Assertions.assertTrue(result.hasError());
        Assertions.assertTrue(result.getIssues().stream().anyMatch(it -> "PARSER_NOT_IMPLEMENTED".equals(it.getCode())));
    }
}

