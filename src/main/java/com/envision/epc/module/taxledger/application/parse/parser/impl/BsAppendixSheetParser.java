package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.envision.epc.module.taxledger.application.dto.BsAppendixUploadDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.SafeBigDecimalReadConverter;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.application.parse.parser.SheetSelectUtils;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * BS附表解析器。
 * 对应sheet页：BS附表
 * 对应类别：FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE
 */
@Component
public class BsAppendixSheetParser implements SheetParser<List<BsAppendixUploadDTO>> {
    @Override
    public ParseResult<List<BsAppendixUploadDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<BsAppendixUploadDTO>> result = ParseResult.<List<BsAppendixUploadDTO>>builder()
                .data(List.of())
                .build();

        try {
            byte[] bytes = inputStream.readAllBytes();
            String sheetName = SheetSelectUtils.resolveEasyExcelSheetName(bytes, category());
            List<BsAppendixUploadDTO> rows = EasyExcelFactory.read(new ByteArrayInputStream(bytes))
                    .registerConverter(new SafeBigDecimalReadConverter())
                    .head(BsAppendixUploadDTO.class)
                    .sheet(sheetName)
                    .doReadSync();
            result.setData(rows);
            return result;
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }
    }

    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE;
    }

    @Override
    public Class<List<BsAppendixUploadDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<BsAppendixUploadDTO>> cls = (Class<List<BsAppendixUploadDTO>>) (Class<?>) List.class;
        return cls;
    }
}
