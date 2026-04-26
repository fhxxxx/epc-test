package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.application.dto.DatalakeExportRowDTO;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据湖-所得税明细解析器。
 */
@Component
public class DataLakeIncomeTaxSheetParser extends AbstractDatalakeDetailSheetParser<Object> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.DL_INCOME_TAX;
    }

    @Override
    protected Class<Object> resultClass() {
        return Object.class;
    }

    @Override
    protected Object aggregate(List<DatalakeExportRowDTO> rows) {
        return null;
    }
}
