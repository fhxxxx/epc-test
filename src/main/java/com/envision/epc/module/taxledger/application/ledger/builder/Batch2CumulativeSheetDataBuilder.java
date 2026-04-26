package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.Batch2CumulativeSheetData;
import org.springframework.stereotype.Component;

@Component
public class Batch2CumulativeSheetDataBuilder implements LedgerSheetDataBuilder<Batch2CumulativeSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.BATCH2_CUMULATIVE;
    }

    @Override
    public Batch2CumulativeSheetData build(LedgerBuildContext ctx) {
        Object n20 = ctx.getNodeOutputs() == null ? null : ctx.getNodeOutputs().get("N20");
        return Batch2CumulativeSheetData.builder()
                .message("Stage 2 cumulative sheets generated")
                .n20Output(toStringObjectMap(n20))
                .build();
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> toStringObjectMap(Object value) {
        if (!(value instanceof java.util.Map<?, ?> map)) {
            return java.util.Map.of();
        }
        return (java.util.Map<String, Object>) map;
    }
}
