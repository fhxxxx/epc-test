package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.envision.epc.module.taxledger.application.dto.VatInputCertParsedDTO;
import com.envision.epc.module.taxledger.application.dto.VatInputCertificationItemDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.SafeBigDecimalReadConverter;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import cn.hutool.core.text.CharSequenceUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/**
 * 增值税进项认证清单解析器。
 * 对应sheet页：增值税进项认证清单
 * 对应类别：FileCategoryEnum.VAT_INPUT_CERT
 */
@Component
public class VatInputCertificationSheetParser implements SheetParser<VatInputCertParsedDTO> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.VAT_INPUT_CERT;
    }

    @Override
    public Class<VatInputCertParsedDTO> resultType() {
        return VatInputCertParsedDTO.class;
    }

    @Override
    public ParseResult<VatInputCertParsedDTO> parse(InputStream inputStream, ParseContext context) {
        if (inputStream == null) {
            return ParseResult.<VatInputCertParsedDTO>builder()
                    .data(emptyResult())
                    .issues(new ArrayList<>(List.of("EMPTY_FILE: inputStream is null")))
                    .build();
        }

        ParseResult<VatInputCertParsedDTO> result = ParseResult.<VatInputCertParsedDTO>builder()
                .data(emptyResult())
                .build();
        try {
            List<VatInputCertificationItemDTO> rows = EasyExcelFactory.read(inputStream)
                    .registerConverter(new SafeBigDecimalReadConverter())
                    .head(VatInputCertificationItemDTO.class)
                    .sheet(category().getTargetSheetName())
                    .headRowNumber(3)
                    .doReadSync();
            BigDecimal sum = BigDecimal.ZERO;
            for (VatInputCertificationItemDTO row : rows) {
                if (row == null || isTotalRow(row.getSerialNo())) {
                    continue;
                }
                if (row.getTaxAmount() != null) {
                    sum = sum.add(row.getTaxAmount());
                }
            }
            VatInputCertParsedDTO parsed = new VatInputCertParsedDTO();
            parsed.setTaxAmountSum(sum);
            result.setData(parsed);
            return result;
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }
    }

    private VatInputCertParsedDTO emptyResult() {
        VatInputCertParsedDTO dto = new VatInputCertParsedDTO();
        dto.setTaxAmountSum(BigDecimal.ZERO);
        return dto;
    }

    private boolean isTotalRow(String serialNo) {
        if (CharSequenceUtil.isBlank(serialNo)) {
            return false;
        }
        String normalized = serialNo.trim();
        return normalized.contains("合计");
    }
}
