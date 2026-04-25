package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.envision.epc.module.taxledger.application.dto.PlAppendixProjectCompanyUploadDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.SafeBigDecimalReadConverter;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * PL附表-项目公司解析器。
 * 对应sheet页：PL附表-项目公司
 * 对应类别：FileCategoryEnum.PL_APPENDIX_PROJECT
 */
@Component
public class PlAppendixProjectCompanySheetParser implements SheetParser<List<PlAppendixProjectCompanyUploadDTO>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.PL_APPENDIX_PROJECT;
    }

    @Override
    public Class<List<PlAppendixProjectCompanyUploadDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<PlAppendixProjectCompanyUploadDTO>> cls = (Class<List<PlAppendixProjectCompanyUploadDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<PlAppendixProjectCompanyUploadDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<PlAppendixProjectCompanyUploadDTO>> result = ParseResult.<List<PlAppendixProjectCompanyUploadDTO>>builder()
                .data(List.of())
                .build();
        try {
            List<PlAppendixProjectCompanyUploadDTO> rows = EasyExcelFactory.read(inputStream)
                    .registerConverter(new SafeBigDecimalReadConverter())
                    .head(PlAppendixProjectCompanyUploadDTO.class)
                    .sheet(category().getTargetSheetName())
                    .doReadSync();
            result.setData(rows);
            return result;
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }
    }
}
