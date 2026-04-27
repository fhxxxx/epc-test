package com.envision.epc.module.taxledger.application.ledger;

import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 台账真实业务 Sheet 定义
 */
@Getter
@RequiredArgsConstructor
public enum LedgerSheetCode {
    BS(FileCategoryEnum.BS),
    PL(FileCategoryEnum.PL),
    BS_APPENDIX(FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE),
    PL_APPENDIX_2320(FileCategoryEnum.PL_APPENDIX_2320),
    PL_APPENDIX_PROJECT(FileCategoryEnum.PL_APPENDIX_PROJECT),
    STAMP_TAX(FileCategoryEnum.STAMP_TAX),
    STAMP_TAX_PROJECT(FileCategoryEnum.STAMP_TAX_PROJECT),
    VAT_OUTPUT(FileCategoryEnum.VAT_OUTPUT),
    VAT_INPUT_CERT(FileCategoryEnum.VAT_INPUT_CERT),
    CUMULATIVE_PROJECT_TAX(FileCategoryEnum.CUMULATIVE_PROJECT_TAX),
    VAT_CHANGE_APPENDIX(FileCategoryEnum.VAT_CHANGE_APPENDIX),
    CONTRACT_STAMP_DUTY_LEDGER(FileCategoryEnum.CONTRACT_STAMP_DUTY_LEDGER),
    PROJECT_CUMULATIVE_DECLARATION(FileCategoryEnum.PROJECT_CUMULATIVE_DECLARATION),
    PROJECT_CUMULATIVE_PAYMENT(FileCategoryEnum.PROJECT_CUMULATIVE_PAYMENT),
    CUMULATIVE_TAX_SUMMARY_2320_2355(FileCategoryEnum.CUMULATIVE_TAX_SUMMARY_2320_2355),
    VAT_CHANGE(FileCategoryEnum.VAT_CHANGE),
    VAT_TABLE_ONE_CUMULATIVE_OUTPUT(FileCategoryEnum.VAT_TABLE_ONE_CUMULATIVE_OUTPUT),
    TAX_ACCOUNTING_DIFFERENCE_MONITOR(FileCategoryEnum.TAX_ACCOUNTING_DIFFERENCE_MONITOR),
    UNINVOICED_MONITOR(FileCategoryEnum.UNINVOICED_MONITOR),
    SUMMARY(FileCategoryEnum.SUMMARY);

    private final FileCategoryEnum fileCategory;

    public String getSheetName() {
        return fileCategory == null || fileCategory.getTargetSheetName() == null
                ? name()
                : fileCategory.getTargetSheetName().trim();
    }

    public String getDisplayName() {
        return fileCategory == null ? name() : fileCategory.getDisplayName();
    }

    public SheetGenerationMode getMode() {
        return switch (this) {
            case VAT_OUTPUT, VAT_INPUT_CERT -> SheetGenerationMode.COPY_FROM_SOURCE;
            default -> SheetGenerationMode.COMPUTED;
        };
    }
}
