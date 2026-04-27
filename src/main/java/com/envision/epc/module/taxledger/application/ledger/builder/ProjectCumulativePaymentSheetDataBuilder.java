package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.ProjectCumulativePaymentLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.ProjectCumulativePaymentSheetDTO;
import org.springframework.stereotype.Component;


/**
 * 项目累计缴纳 页数据构建器。
 */
@Component
public class ProjectCumulativePaymentSheetDataBuilder implements LedgerSheetDataBuilder<ProjectCumulativePaymentLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.PROJECT_CUMULATIVE_PAYMENT;
    }

    @Override
    public ProjectCumulativePaymentLedgerSheetData build(LedgerBuildContext ctx) {
        return new ProjectCumulativePaymentLedgerSheetData(
                SheetDataReaders.requireObject(ctx, FileCategoryEnum.PROJECT_CUMULATIVE_PAYMENT, ProjectCumulativePaymentSheetDTO.class, LedgerSheetCode.PROJECT_CUMULATIVE_PAYMENT));
    }
}

