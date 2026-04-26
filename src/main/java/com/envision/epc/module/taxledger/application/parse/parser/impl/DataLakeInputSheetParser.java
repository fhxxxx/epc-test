package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.application.dto.DatalakeExportRowDTO;
import com.envision.epc.module.taxledger.application.dto.DlInputParsedDTO;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数据湖-进项明细解析器。
 */
@Component
public class DataLakeInputSheetParser extends AbstractDatalakeDetailSheetParser<DlInputParsedDTO> {
    private static final String ACCOUNT_INPUT_TRANSFER_OUT = "2221010400";

    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.DL_INPUT;
    }

    @Override
    protected Class<DlInputParsedDTO> resultClass() {
        return DlInputParsedDTO.class;
    }

    @Override
    protected DlInputParsedDTO aggregate(List<DatalakeExportRowDTO> rows) {
        DlInputParsedDTO dto = new DlInputParsedDTO();
        dto.getLocalAmountSumByAccount().put(ACCOUNT_INPUT_TRANSFER_OUT, BigDecimal.ZERO);

        for (DatalakeExportRowDTO row : rows) {
            if (row == null) {
                continue;
            }
            String account = normalizedAccount(row.getAccount());
            if (!ACCOUNT_INPUT_TRANSFER_OUT.equals(account)) {
                continue;
            }
            dto.getLocalAmountSumByAccount().compute(account, (k, v) -> (v == null ? BigDecimal.ZERO : v)
                    .add(amount(row.getLocalAmount())));
        }
        return dto;
    }
}
