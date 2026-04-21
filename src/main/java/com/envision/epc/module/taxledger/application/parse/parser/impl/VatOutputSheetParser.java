package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.application.dto.VatOutputSheetUploadDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 增值税销项解析器（双section）。
 * 对应sheet页：增值税销项
 * 对应类别：FileCategoryEnum.VAT_OUTPUT
 */
@Component
public class VatOutputSheetParser implements SheetParser<VatOutputSheetUploadDTO> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.VAT_OUTPUT;
    }

    @Override
    public Class<VatOutputSheetUploadDTO> resultType() {
        return VatOutputSheetUploadDTO.class;
    }

    @Override
    public ParseResult<VatOutputSheetUploadDTO> parse(InputStream inputStream, ParseContext context) {
        ParseResult<VatOutputSheetUploadDTO> result = ParseResult.<VatOutputSheetUploadDTO>builder()
                .data(new VatOutputSheetUploadDTO())
                .build();
        result.addIssue("增值税销项 parser is initialized only");
        return result;
    }
}
