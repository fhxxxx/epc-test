package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.application.dto.DatalakeExportRowDTO;
import com.envision.epc.module.taxledger.application.dto.DlOtherParsedDTO;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数据湖-其他科目明细解析器。
 */
@Component
public class DataLakeOtherSheetParser extends AbstractDatalakeDetailSheetParser<DlOtherParsedDTO> {
    private static final String ACCOUNT_INTEREST = "6603020011";
    private static final String ACCOUNT_OTHER_INCOME = "6702000010";

    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.DL_OTHER;
    }

    @Override
    protected Class<DlOtherParsedDTO> resultClass() {
        return DlOtherParsedDTO.class;
    }

    @Override
    protected DlOtherParsedDTO aggregate(List<DatalakeExportRowDTO> rows) {
        DlOtherParsedDTO dto = new DlOtherParsedDTO();
        dto.getDocumentAmountSumByAccount().put(ACCOUNT_INTEREST, BigDecimal.ZERO);
        dto.getDocumentAmountSumByAccount().put(ACCOUNT_OTHER_INCOME, BigDecimal.ZERO);
        dto.getLocalAmountSumByAccount().put(ACCOUNT_INTEREST, BigDecimal.ZERO);
        dto.getLocalAmountSumByAccount().put(ACCOUNT_OTHER_INCOME, BigDecimal.ZERO);

        for (DatalakeExportRowDTO row : rows) {
            if (row == null) {
                continue;
            }
            boolean matchedInterest = accountContains(row.getAccount(), ACCOUNT_INTEREST);
            boolean matchedOtherIncome = accountContains(row.getAccount(), ACCOUNT_OTHER_INCOME);
            if (!matchedInterest && !matchedOtherIncome) {
                continue;
            }
            if (matchedInterest) {
                dto.getDocumentAmountSumByAccount().compute(ACCOUNT_INTEREST, (k, v) -> (v == null ? BigDecimal.ZERO : v)
                        .add(amount(row.getDocumentAmount())));
                dto.getLocalAmountSumByAccount().compute(ACCOUNT_INTEREST, (k, v) -> (v == null ? BigDecimal.ZERO : v)
                        .add(amount(row.getLocalAmount())));
            }
            if (matchedOtherIncome) {
                dto.getDocumentAmountSumByAccount().compute(ACCOUNT_OTHER_INCOME, (k, v) -> (v == null ? BigDecimal.ZERO : v)
                        .add(amount(row.getDocumentAmount())));
                dto.getLocalAmountSumByAccount().compute(ACCOUNT_OTHER_INCOME, (k, v) -> (v == null ? BigDecimal.ZERO : v)
                        .add(amount(row.getLocalAmount())));
            }
        }
        return dto;
    }
}
