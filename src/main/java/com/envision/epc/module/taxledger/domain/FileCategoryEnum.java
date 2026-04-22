package com.envision.epc.module.taxledger.domain;

import com.envision.epc.module.taxledger.application.parse.EngineType;

/**
 * 文件类别枚举
 */
public enum FileCategoryEnum {
    BS("资产负债表（BS）", true, EngineType.EASY_EXCEL, "资产负债表"),
    PL("利润表（PL）", true, EngineType.EASY_EXCEL, "利润表"),
    BS_APPENDIX_TAX_PAYABLE("BS附表-应交税费科目余额表", true, EngineType.EASY_EXCEL, "BS附表"),
    PL_APPENDIX_2320("PL附表（2320/2355公司）", false, EngineType.ASPOSE, "PL附表-2320、2355"),
    PL_APPENDIX_PROJECT("PL附表（项目公司）", true, EngineType.EASY_EXCEL, "PL附表-项目公司"),
    STAMP_TAX("印花税明细", true, EngineType.EASY_EXCEL, "印花税明细-2320、2355"),
    VAT_OUTPUT("增值税销项", true, EngineType.ASPOSE, "增值税销项"),
    VAT_INPUT_CERT("增值税进项认证清单", true, EngineType.EASY_EXCEL, "增值税进项认证清单"),
    CUMULATIVE_PROJECT_TAX("累计项目税收明细表", true, EngineType.ASPOSE, "累计项目税收明细表202512"),
    VAT_CHANGE_APPENDIX("增值税变动表附表", true, EngineType.ASPOSE, "增值税变动表附表"),
    CONTRACT_STAMP_DUTY_LEDGER("合同印花税明细台账", true, EngineType.EASY_EXCEL, "合同印花税明细台账"),
    MONTHLY_SETTLEMENT_TAX("睿景景程月结数据表-报税", true, EngineType.ASPOSE, "睿景景程月结数据表-报税"),
    PROJECT_CUMULATIVE_DECLARATION("项目累计申报", false, EngineType.ASPOSE, "项目累计申报"),
    PROJECT_CUMULATIVE_PAYMENT("项目累计缴纳", false, EngineType.ASPOSE, "项目累计缴纳"),
    SUMMARY("summary表", false, EngineType.ASPOSE, "Summary "),
    CUMULATIVE_TAX_SUMMARY_2320_2355("累计税金汇总表-2320、2355", false, EngineType.ASPOSE, "累计税金汇总表-2320、2355"),
    VAT_CHANGE("增值税变动表", false, EngineType.ASPOSE, "增值税变动表"),
    VAT_TABLE_ONE_CUMULATIVE_OUTPUT("增值税表一 累计销项-2320、2355", false, EngineType.ASPOSE, "增值税表一 累计销项-2320、2355"),
    TAX_ACCOUNTING_DIFFERENCE_MONITOR("账税差异监控-2320、2355", false, EngineType.ASPOSE, "账税差异监控-2320、2355"),
    UNINVOICED_MONITOR("未开票数监控", false, EngineType.ASPOSE, "未开票数监控"),
    DL_INCOME("收入明细", true, EngineType.EASY_EXCEL, null),
    DL_OUTPUT("销项明细", true, EngineType.EASY_EXCEL, null),
    DL_INPUT("进项明细", true, EngineType.EASY_EXCEL, null),
    DL_INCOME_TAX("所得税明细", true, EngineType.EASY_EXCEL, null),
    DL_OTHER("其他科目明细", true, EngineType.EASY_EXCEL, null);

    private final String displayName;
    private final boolean manualUpload;
    private final EngineType preferredEngine;
    private final String targetSheetName;

    FileCategoryEnum(String displayName,
                     boolean manualUpload,
                     EngineType preferredEngine,
                     String targetSheetName) {
        this.displayName = displayName;
        this.manualUpload = manualUpload;
        this.preferredEngine = preferredEngine;
        this.targetSheetName = targetSheetName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isManualUpload() {
        return manualUpload;
    }

    public EngineType getPreferredEngine() {
        return preferredEngine;
    }

    public String getTargetSheetName() {
        return targetSheetName;
    }
}
