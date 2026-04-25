package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.envision.epc.module.taxledger.application.dto.ContractStampDutyLedgerItemDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.SafeBigDecimalReadConverter;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 合同印花税明细台账解析器。
 * 对应sheet页：合同印花税明细台账
 * 对应类别：FileCategoryEnum.CONTRACT_STAMP_DUTY_LEDGER
 */
@Component
public class ContractStampDutyLedgerSheetParser implements SheetParser<List<ContractStampDutyLedgerItemDTO>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.CONTRACT_STAMP_DUTY_LEDGER;
    }

    @Override
    public Class<List<ContractStampDutyLedgerItemDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<ContractStampDutyLedgerItemDTO>> cls = (Class<List<ContractStampDutyLedgerItemDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<ContractStampDutyLedgerItemDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<ContractStampDutyLedgerItemDTO>> result = ParseResult.<List<ContractStampDutyLedgerItemDTO>>builder()
                .data(List.of())
                .build();
        try {
            List<ContractStampDutyLedgerItemDTO> rows = EasyExcelFactory.read(inputStream)
                    .registerConverter(new SafeBigDecimalReadConverter())
                    .head(ContractStampDutyLedgerItemDTO.class)
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
