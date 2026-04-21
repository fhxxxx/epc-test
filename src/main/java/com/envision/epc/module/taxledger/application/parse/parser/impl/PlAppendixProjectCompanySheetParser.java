package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.application.dto.PlAppendixProjectCompanyUploadDTO;
import com.envision.epc.module.taxledger.application.parse.EngineType;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.AbstractEasyExcelSheetParser;
import com.envision.epc.module.taxledger.application.parse.parser.ParserValueUtils;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * PL附表-项目公司解析器。
 * 对应sheet页：PL附表-项目公司
 * 对应类别：FileCategoryEnum.PL_APPENDIX_PROJECT
 */
@Component
public class PlAppendixProjectCompanySheetParser extends AbstractEasyExcelSheetParser<List<PlAppendixProjectCompanyUploadDTO>> {
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
    protected List<PlAppendixProjectCompanyUploadDTO> mapToDto(HeaderData headerData,
                                                               BodyData bodyData,
                                                               ParseContext context,
                                                               ParseResult<List<PlAppendixProjectCompanyUploadDTO>> result) {
        List<PlAppendixProjectCompanyUploadDTO> rows = new ArrayList<>();
        for (List<String> row : bodyData.rows()) {
            String splitBasis = findCell(row, headerData, "拆分依据");
            if (splitBasis == null || splitBasis.isBlank()) {
                continue;
            }
            PlAppendixProjectCompanyUploadDTO dto = new PlAppendixProjectCompanyUploadDTO();
            dto.setSplitBasis(splitBasis);
            dto.setMainBusinessRevenue(ParserValueUtils.toBigDecimal(findCell(row, headerData, "主营业务收入")));
            dto.setOutputTax(ParserValueUtils.toBigDecimal(findCell(row, headerData, "销项")));
            rows.add(dto);
        }
        return rows;
    }

    @Override
    protected EngineType engineType() {
        return EngineType.EASY_EXCEL;
    }
}
