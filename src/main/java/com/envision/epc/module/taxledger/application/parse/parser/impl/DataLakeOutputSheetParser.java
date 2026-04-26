package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.application.dto.DatalakeExportRowDTO;
import com.envision.epc.module.taxledger.application.dto.DlOutputParsedDTO;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数据湖-销项明细解析器。
 */
@Component
public class DataLakeOutputSheetParser extends AbstractDatalakeDetailSheetParser<DlOutputParsedDTO> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.DL_OUTPUT;
    }

    @Override
    protected Class<DlOutputParsedDTO> resultClass() {
        return DlOutputParsedDTO.class;
    }

    @Override
    protected DlOutputParsedDTO aggregate(List<DatalakeExportRowDTO> rows) {
        BigDecimal sum = BigDecimal.ZERO;
        for (DatalakeExportRowDTO row : rows) {
            if (row == null) {
                continue;
            }
            sum = sum.add(amount(row.getDocumentAmount()));
        }
        DlOutputParsedDTO dto = new DlOutputParsedDTO();
        dto.setDocumentAmountSum(sum);
        return dto;
    }
}
