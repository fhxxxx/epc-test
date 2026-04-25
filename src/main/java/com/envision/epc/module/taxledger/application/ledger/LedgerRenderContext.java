package com.envision.epc.module.taxledger.application.ledger;

import lombok.Builder;
import lombok.Value;

/**
 * 渲染阶段上下文
 */
@Value
@Builder
public class LedgerRenderContext {
    String companyCode;
    String yearMonth;
    String templateLoader;
    String stylePolicy;
    String formulaPolicy;
}
