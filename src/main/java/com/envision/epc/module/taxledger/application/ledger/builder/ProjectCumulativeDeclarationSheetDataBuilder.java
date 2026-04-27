package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.ProjectCumulativeDeclarationLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.ProjectCumulativeDeclarationSheetDTO;
import org.springframework.stereotype.Component;


/**
 * 项目累计申报 页数据构建器。
 */
@Component
public class ProjectCumulativeDeclarationSheetDataBuilder implements LedgerSheetDataBuilder<ProjectCumulativeDeclarationLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.PROJECT_CUMULATIVE_DECLARATION;
    }

    @Override
    public ProjectCumulativeDeclarationLedgerSheetData build(LedgerBuildContext ctx) {
        return new ProjectCumulativeDeclarationLedgerSheetData(
                SheetDataReaders.requireObject(ctx, FileCategoryEnum.PROJECT_CUMULATIVE_DECLARATION, ProjectCumulativeDeclarationSheetDTO.class, LedgerSheetCode.PROJECT_CUMULATIVE_DECLARATION));
    }
}

