package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.envision.epc.module.taxledger.application.dto.VatInputCertificationItemDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.SafeBigDecimalReadConverter;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 增值税进项认证清单解析器。
 * 对应sheet页：增值税进项认证清单
 * 对应类别：FileCategoryEnum.VAT_INPUT_CERT
 */
@Component
public class VatInputCertificationSheetParser implements SheetParser<List<VatInputCertificationItemDTO>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.VAT_INPUT_CERT;
    }

    @Override
    public Class<List<VatInputCertificationItemDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<VatInputCertificationItemDTO>> cls = (Class<List<VatInputCertificationItemDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<VatInputCertificationItemDTO>> parse(InputStream inputStream, ParseContext context) {
        if (inputStream == null) {
            return ParseResult.<List<VatInputCertificationItemDTO>>builder()
                    .data(List.of())
                    .issues(new ArrayList<>(List.of("EMPTY_FILE: inputStream is null")))
                    .build();
        }

        ParseResult<List<VatInputCertificationItemDTO>> result = ParseResult.<List<VatInputCertificationItemDTO>>builder()
                .data(List.of())
                .build();
        try {
            List<VatInputCertificationItemDTO> rows = EasyExcelFactory.read(inputStream)
                    .registerConverter(new SafeBigDecimalReadConverter())
                    .head(VatInputCertificationItemDTO.class)
                    .sheet(category().getTargetSheetName())
                    .headRowNumber(3)
                    .doReadSync();
            result.setData(rows);
            return result;
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }
    }
}
