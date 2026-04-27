package com.envision.epc.module.taxledger.application.ledger;

import com.envision.epc.module.taxledger.application.dto.BsAppendixUploadDTO;
import com.envision.epc.module.taxledger.application.dto.BsStatementRowDTO;
import com.envision.epc.module.taxledger.application.dto.ContractStampDutyLedgerItemDTO;
import com.envision.epc.module.taxledger.application.dto.CumulativeTaxSummary23202355ColumnDTO;
import com.envision.epc.module.taxledger.application.dto.DlInputParsedDTO;
import com.envision.epc.module.taxledger.application.dto.DlOtherParsedDTO;
import com.envision.epc.module.taxledger.application.dto.DlOutputParsedDTO;
import com.envision.epc.module.taxledger.application.dto.MonthlySettlementTaxParsedDTO;
import com.envision.epc.module.taxledger.application.dto.PlAppendix23202355DTO;
import com.envision.epc.module.taxledger.application.dto.PlAppendixProjectCompanyUploadDTO;
import com.envision.epc.module.taxledger.application.dto.PlStatementRowDTO;
import com.envision.epc.module.taxledger.application.dto.ProjectCumulativeDeclarationSheetDTO;
import com.envision.epc.module.taxledger.application.dto.ProjectCumulativePaymentSheetDTO;
import com.envision.epc.module.taxledger.application.dto.StampDutySummaryRowDTO;
import com.envision.epc.module.taxledger.application.dto.TaxAccountingDifferenceMonitor23202355ItemDTO;
import com.envision.epc.module.taxledger.application.dto.UninvoicedMonitorItemDTO;
import com.envision.epc.module.taxledger.application.dto.VatInputCertParsedDTO;
import com.envision.epc.module.taxledger.application.dto.VatChangeAppendixUploadDTO;
import com.envision.epc.module.taxledger.application.dto.VatChangeRowDTO;
import com.envision.epc.module.taxledger.application.dto.VatOutputSheetUploadDTO;
import com.envision.epc.module.taxledger.application.dto.VatTableOneCumulativeOutputItemDTO;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 解析结果类型目录（静态映射）
 */
public final class ParsedResultTypeCatalog {
    public enum Shape {
        OBJECT,
        LIST
    }

    public record Entry(Shape shape, Class<?> valueType) {
    }

    private static final Map<FileCategoryEnum, Entry> CATALOG = new EnumMap<>(FileCategoryEnum.class);

    static {
        registerList(FileCategoryEnum.BS, BsStatementRowDTO.class);
        registerList(FileCategoryEnum.PL, PlStatementRowDTO.class);
        registerList(FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE, BsAppendixUploadDTO.class);
        registerObject(FileCategoryEnum.PL_APPENDIX_2320, PlAppendix23202355DTO.class);
        registerList(FileCategoryEnum.PL_APPENDIX_PROJECT, PlAppendixProjectCompanyUploadDTO.class);
        registerList(FileCategoryEnum.STAMP_TAX, StampDutySummaryRowDTO.class);
        registerObject(FileCategoryEnum.VAT_OUTPUT, VatOutputSheetUploadDTO.class);
        registerObject(FileCategoryEnum.VAT_INPUT_CERT, VatInputCertParsedDTO.class);
        registerObject(FileCategoryEnum.CUMULATIVE_PROJECT_TAX, Object.class);
        registerObject(FileCategoryEnum.VAT_CHANGE_APPENDIX, VatChangeAppendixUploadDTO.class);
        registerList(FileCategoryEnum.CONTRACT_STAMP_DUTY_LEDGER, ContractStampDutyLedgerItemDTO.class);
        registerObject(FileCategoryEnum.MONTHLY_SETTLEMENT_TAX, MonthlySettlementTaxParsedDTO.class);
        registerObject(FileCategoryEnum.PREINVOICE_ACCRUAL_REVERSAL_2320_2355, Object.class);
        registerObject(FileCategoryEnum.PROJECT_CUMULATIVE_DECLARATION, ProjectCumulativeDeclarationSheetDTO.class);
        registerObject(FileCategoryEnum.PROJECT_CUMULATIVE_PAYMENT, ProjectCumulativePaymentSheetDTO.class);
        registerList(FileCategoryEnum.CUMULATIVE_TAX_SUMMARY_2320_2355, CumulativeTaxSummary23202355ColumnDTO.class);
        registerList(FileCategoryEnum.VAT_CHANGE, VatChangeRowDTO.class);
        registerList(FileCategoryEnum.VAT_TABLE_ONE_CUMULATIVE_OUTPUT, VatTableOneCumulativeOutputItemDTO.class);
        registerList(FileCategoryEnum.TAX_ACCOUNTING_DIFFERENCE_MONITOR, TaxAccountingDifferenceMonitor23202355ItemDTO.class);
        registerList(FileCategoryEnum.UNINVOICED_MONITOR, UninvoicedMonitorItemDTO.class);
        registerObject(FileCategoryEnum.DL_INCOME, Object.class);
        registerObject(FileCategoryEnum.DL_OUTPUT, DlOutputParsedDTO.class);
        registerObject(FileCategoryEnum.DL_INPUT, DlInputParsedDTO.class);
        registerObject(FileCategoryEnum.DL_INCOME_TAX, Object.class);
        registerObject(FileCategoryEnum.DL_OTHER, DlOtherParsedDTO.class);
    }

    private ParsedResultTypeCatalog() {
    }

    public static Entry get(FileCategoryEnum category) {
        return CATALOG.get(category);
    }

    public static boolean supports(FileCategoryEnum category) {
        return CATALOG.containsKey(category);
    }

    public static Set<FileCategoryEnum> categories() {
        return CATALOG.keySet();
    }

    private static void registerObject(FileCategoryEnum category, Class<?> valueType) {
        CATALOG.put(category, new Entry(Shape.OBJECT, valueType));
    }

    private static void registerList(FileCategoryEnum category, Class<?> elementType) {
        CATALOG.put(category, new Entry(Shape.LIST, elementType));
    }
}
