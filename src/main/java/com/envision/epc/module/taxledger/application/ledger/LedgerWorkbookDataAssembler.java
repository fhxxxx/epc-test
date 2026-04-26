package com.envision.epc.module.taxledger.application.ledger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 整本台账数据构建器
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LedgerWorkbookDataAssembler {
    private final SheetExecutionPlan executionPlan;
    private final LedgerSheetRegistry registry;

    public LedgerWorkbookData buildAll(LedgerBuildContext ctx) {
        LedgerWorkbookData.LedgerWorkbookDataBuilder builder = LedgerWorkbookData.builder()
                .companyCode(ctx.getCompanyCode())
                .yearMonth(ctx.getYearMonth());

        for (LedgerSheetCode code : executionPlan.orderedFor(ctx.getCompanyCode())) {
            long start = System.currentTimeMillis();
            LedgerSheetData data = registry.requiredBuilder(code).build(ctx);
            long elapsed = System.currentTimeMillis() - start;
            builder.sheetData(code, data);
            builder.buildMetric(code, elapsed);
            log.info("ledger sheet data assembled: companyCode={}, yearMonth={}, sheetCode={}, dataType={}, rowCount={}, buildMs={}",
                    ctx.getCompanyCode(),
                    ctx.getYearMonth(),
                    code.name(),
                    data == null ? "null" : data.getClass().getSimpleName(),
                    data == null ? null : data.rowCount(),
                    elapsed);
        }
        return builder.build();
    }
}
