package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

/**
 * 数据湖-进项明细解析器。
 */
@Component
public class DataLakeInputSheetParser extends AbstractDatalakeDetailSheetParser {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.DL_INPUT;
    }
}

