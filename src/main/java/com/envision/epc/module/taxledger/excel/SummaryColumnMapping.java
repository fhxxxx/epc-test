package com.envision.epc.module.taxledger.excel;

/**
 * Summary 模板列映射（0-based）。
 */
public final class SummaryColumnMapping {
    private SummaryColumnMapping() {
    }

    public static final int COL_SEQ = 0;
    public static final int COL_TAX_TYPE = 1;
    public static final int COL_TAX_ITEM = 2;
    public static final int COL_TAX_BASIS_DESC = 3;
    public static final int COL_TAX_BASE_MAIN = 4;
    public static final int COL_LEVY_RATIO = 5;
    public static final int COL_TAX_RATE = 6;
    public static final int COL_DECLARED_AMOUNT = 7;
    public static final int COL_EXTRA_1 = 8;
    public static final int COL_BOOK_AMOUNT = 9;
    public static final int COL_VARIANCE_AMOUNT = 10;
    public static final int COL_VARIANCE_REASON = 11;
    /** 企业所得税：剩余可弥补亏损金额 */
    public static final int COL_EXTRA_2 = 12;
    /** 企业所得税：扩展列（如剩余可弥补亏损金额） */
    public static final int COL_EXTRA_3 = 13;
}
