package com.envision.epc.module.taxledger.domain;

/**
 * 文件类别枚举
 */
public enum FileCategoryEnum {
    BS("资产负债表（BS）", true, true, "资产负债表", FileCategoryScopeEnum.ALL),
    PL("利润表（PL）", true, true, "利润表", FileCategoryScopeEnum.ALL),
    BS_APPENDIX_TAX_PAYABLE("BS附表", true, true, "BS附表", FileCategoryScopeEnum.ALL),
    PL_APPENDIX_2320("PL附表（2320/2355公司）", true, true, "PL附表-2320、2355", FileCategoryScopeEnum.RJJC_2320_2355),
    PL_APPENDIX_PROJECT("PL附表（项目公司）", true, true, "PL附表-项目公司", FileCategoryScopeEnum.PROJECT),
    STAMP_TAX("印花税明细-2320、2355", true, true, "印花税明细-2320、2355", FileCategoryScopeEnum.RJJC_2320_2355),
    STAMP_TAX_PROJECT("印花税明细--非2320、2355", false, true, "印花税明细--非2320、2355", FileCategoryScopeEnum.PROJECT),
    VAT_OUTPUT("增值税销项", true, true, "增值税销项", FileCategoryScopeEnum.ALL),
    VAT_INPUT_CERT("增值税进项认证清单", true, true, "增值税进项认证清单", FileCategoryScopeEnum.ALL),
    CUMULATIVE_PROJECT_TAX("累计项目税收明细表", true, true, "累计项目税收明细表202512", FileCategoryScopeEnum.RJJC_2320_2355),
    VAT_CHANGE_APPENDIX("增值税变动表附表", true, true, "增值税变动表附表", FileCategoryScopeEnum.ALL),
    CONTRACT_STAMP_DUTY_LEDGER("合同印花税明细台账", true, true, "合同印花税明细台账", FileCategoryScopeEnum.PROJECT),
    MONTHLY_SETTLEMENT_TAX("睿景景程月结数据表-报税", true, false, "睿景景程月结数据表-报税", FileCategoryScopeEnum.RJJC_2320_2355),
    PREINVOICE_ACCRUAL_REVERSAL_2320_2355("预开票收入计提及冲回统计（2320、2355）", true, false, "预开票收入计提及冲回统计（2320、2355）", FileCategoryScopeEnum.RJJC_2320_2355),
    PROJECT_CUMULATIVE_DECLARATION("项目累计申报", false, true, "项目累计申报", FileCategoryScopeEnum.ALL),
    PROJECT_CUMULATIVE_PAYMENT("项目累计缴纳", false, true, "项目累计缴纳", FileCategoryScopeEnum.ALL),
    SUMMARY("summary表", false, true, "Summary ", FileCategoryScopeEnum.ALL),
    CUMULATIVE_TAX_SUMMARY_2320_2355("累计税金汇总表-2320、2355", false, true, "累计税金汇总表-2320、2355", FileCategoryScopeEnum.RJJC_2320_2355),
    VAT_CHANGE("增值税变动表", false, true, "增值税变动表", FileCategoryScopeEnum.ALL),
    VAT_TABLE_ONE_CUMULATIVE_OUTPUT("增值税表一 累计销项-2320、2355", false, true, "增值税表一 累计销项-2320、2355", FileCategoryScopeEnum.RJJC_2320_2355),
    TAX_ACCOUNTING_DIFFERENCE_MONITOR("账税差异监控-2320、2355", false, true, "账税差异监控-2320、2355", FileCategoryScopeEnum.RJJC_2320_2355),
    UNINVOICED_MONITOR("未开票数监控", false, true, "未开票数监控", FileCategoryScopeEnum.ALL),
    FINAL_LEDGER("最终台账", false, false, null, FileCategoryScopeEnum.ALL),
    DL_INCOME("收入明细", true, false, null, FileCategoryScopeEnum.ALL),
    DL_OUTPUT("销项明细", true, false, null, FileCategoryScopeEnum.ALL),
    DL_INPUT("进项明细", true, false, null, FileCategoryScopeEnum.ALL),
    DL_INCOME_TAX("所得税明细", true, false, null, FileCategoryScopeEnum.ALL),
    DL_OTHER("其他科目明细", true, false, null, FileCategoryScopeEnum.ALL);

    private final String displayName;
    private final boolean manualUpload;
    /**
     * 是否在最终台账中展示该sheet
     * false 表示只参与中间计算/取数
     */
    private final boolean visibleInFinalLedger;
    private final String targetSheetName;
    private final FileCategoryScopeEnum scope;

    FileCategoryEnum(String displayName,
                     boolean manualUpload,
                     boolean visibleInFinalLedger,
                     String targetSheetName,
                     FileCategoryScopeEnum scope) {
        this.displayName = displayName;
        this.manualUpload = manualUpload;
        this.visibleInFinalLedger = visibleInFinalLedger;
        this.targetSheetName = targetSheetName;
        this.scope = scope;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isManualUpload() {
        return manualUpload;
    }

    public String getTargetSheetName() {
        return targetSheetName;
    }

    public boolean isVisibleInFinalLedger() {
        return visibleInFinalLedger;
    }

    public FileCategoryScopeEnum getScope() {
        return scope;
    }

    public boolean isAllowedForCompany(String companyCode) {
        if (scope == FileCategoryScopeEnum.ALL) {
            return true;
        }
        boolean rjjc = isCompany2320Or2355(companyCode);
        return scope == FileCategoryScopeEnum.RJJC_2320_2355 ? rjjc : !rjjc;
    }

    private static boolean isCompany2320Or2355(String companyCode) {
        return "2320".equals(companyCode) || "2355".equals(companyCode);
    }
}
