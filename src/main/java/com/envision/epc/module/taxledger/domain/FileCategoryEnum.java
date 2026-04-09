package com.envision.epc.module.taxledger.domain;

/**
 * 文件类别枚举
 */
public enum FileCategoryEnum {
    BS("资产负债表（BS）"),
    PL("利润表（PL）"),
    BS_APPENDIX_TAX_PAYABLE("BS附表-应交税费科目余额表"),
    PL_APPENDIX_2320("PL附表（2320/2355公司）"),
    PL_APPENDIX_PROJECT("PL附表（项目公司）"),
    STAMP_TAX("印花税明细"),
    VAT_OUTPUT("增值税销项"),
    VAT_INPUT_CERT("增值税进项认证清单"),
    CUMULATIVE_PROJECT_TAX("累计项目税收明细表"),
    DL_INCOME("收入明细"),
    DL_OUTPUT("销项明细"),
    DL_INPUT("进项明细"),
    DL_INCOME_TAX("所得税明细"),
    DL_OTHER("其他科目明细");

    private final String displayName;

    FileCategoryEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
