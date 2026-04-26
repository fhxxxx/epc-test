package com.envision.epc.module.taxledger.application.ledger.data;

import com.envision.epc.module.taxledger.application.dto.BsAppendixUploadDTO;
import com.envision.epc.module.taxledger.application.dto.BsStatementRowDTO;
import com.envision.epc.module.taxledger.application.dto.ContractStampDutyLedgerItemDTO;
import com.envision.epc.module.taxledger.application.dto.CumulativeTaxSummary23202355ColumnDTO;
import com.envision.epc.module.taxledger.application.dto.PlAppendix23202355DTO;
import com.envision.epc.module.taxledger.application.dto.PlAppendixProjectCompanyUploadDTO;
import com.envision.epc.module.taxledger.application.dto.PlStatementRowDTO;
import com.envision.epc.module.taxledger.application.dto.ProjectCumulativeDeclarationSheetDTO;
import com.envision.epc.module.taxledger.application.dto.ProjectCumulativePaymentSheetDTO;
import com.envision.epc.module.taxledger.application.dto.StampDutySummaryRowDTO;
import com.envision.epc.module.taxledger.application.dto.TaxAccountingDifferenceMonitor23202355ItemDTO;
import com.envision.epc.module.taxledger.application.dto.UninvoicedMonitorItemDTO;
import com.envision.epc.module.taxledger.application.dto.VatChangeAppendixUploadDTO;
import com.envision.epc.module.taxledger.application.dto.VatChangeRowDTO;
import com.envision.epc.module.taxledger.application.dto.VatInputCertificationItemDTO;
import com.envision.epc.module.taxledger.application.dto.VatOutputSheetUploadDTO;
import com.envision.epc.module.taxledger.application.dto.VatTableOneCumulativeOutputItemDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Getter;

import java.util.Collection;
import java.util.List;

/**
 * 真实业务 Sheet 数据对象定义。
 */
public final class BusinessSheetData {
    private BusinessSheetData() {
    }

    @Getter
    public abstract static class ParsedSheetData<T> implements LedgerSheetData {
        private final LedgerSheetCode sheetCode;
        private final T payload;

        protected ParsedSheetData(LedgerSheetCode sheetCode, T payload) {
            this.sheetCode = sheetCode;
            this.payload = payload;
        }

        @Override
        public LedgerSheetCode sheetCode() {
            return sheetCode;
        }

        @Override
        public Integer rowCount() {
            if (payload == null) {
                return 0;
            }
            if (payload instanceof Collection<?> collection) {
                return collection.size();
            }
            return 1;
        }
    }

    public static final class Bs extends ParsedSheetData<List<BsStatementRowDTO>> {
        public Bs(List<BsStatementRowDTO> payload) { super(LedgerSheetCode.BS, payload); }
    }

    public static final class Pl extends ParsedSheetData<List<PlStatementRowDTO>> {
        public Pl(List<PlStatementRowDTO> payload) { super(LedgerSheetCode.PL, payload); }
    }

    public static final class BsAppendix extends ParsedSheetData<List<BsAppendixUploadDTO>> {
        public BsAppendix(List<BsAppendixUploadDTO> payload) { super(LedgerSheetCode.BS_APPENDIX, payload); }
    }

    public static final class PlAppendix2320 extends ParsedSheetData<PlAppendix23202355DTO> {
        public PlAppendix2320(PlAppendix23202355DTO payload) { super(LedgerSheetCode.PL_APPENDIX_2320, payload); }
    }

    public static final class PlAppendixProject extends ParsedSheetData<List<PlAppendixProjectCompanyUploadDTO>> {
        public PlAppendixProject(List<PlAppendixProjectCompanyUploadDTO> payload) { super(LedgerSheetCode.PL_APPENDIX_PROJECT, payload); }
    }

    public static final class StampTax extends ParsedSheetData<List<StampDutySummaryRowDTO>> {
        public StampTax(List<StampDutySummaryRowDTO> payload) { super(LedgerSheetCode.STAMP_TAX, payload); }
    }

    public static final class StampTaxProject extends ParsedSheetData<List<StampDutySummaryRowDTO>> {
        public StampTaxProject(List<StampDutySummaryRowDTO> payload) { super(LedgerSheetCode.STAMP_TAX_PROJECT, payload); }
    }

    public static final class VatOutput extends ParsedSheetData<VatOutputSheetUploadDTO> {
        public VatOutput(VatOutputSheetUploadDTO payload) { super(LedgerSheetCode.VAT_OUTPUT, payload); }
    }

    public static final class VatInputCert extends ParsedSheetData<List<VatInputCertificationItemDTO>> {
        public VatInputCert(List<VatInputCertificationItemDTO> payload) { super(LedgerSheetCode.VAT_INPUT_CERT, payload); }
    }

    public static final class CumulativeProjectTax extends ParsedSheetData<Object> {
        public CumulativeProjectTax(Object payload) { super(LedgerSheetCode.CUMULATIVE_PROJECT_TAX, payload); }
    }

    public static final class VatChangeAppendix extends ParsedSheetData<VatChangeAppendixUploadDTO> {
        public VatChangeAppendix(VatChangeAppendixUploadDTO payload) { super(LedgerSheetCode.VAT_CHANGE_APPENDIX, payload); }
    }

    public static final class ContractStampDutyLedger extends ParsedSheetData<List<ContractStampDutyLedgerItemDTO>> {
        public ContractStampDutyLedger(List<ContractStampDutyLedgerItemDTO> payload) { super(LedgerSheetCode.CONTRACT_STAMP_DUTY_LEDGER, payload); }
    }

    public static final class ProjectCumulativeDeclaration extends ParsedSheetData<ProjectCumulativeDeclarationSheetDTO> {
        public ProjectCumulativeDeclaration(ProjectCumulativeDeclarationSheetDTO payload) {
            super(LedgerSheetCode.PROJECT_CUMULATIVE_DECLARATION, payload);
        }
    }

    public static final class ProjectCumulativePayment extends ParsedSheetData<ProjectCumulativePaymentSheetDTO> {
        public ProjectCumulativePayment(ProjectCumulativePaymentSheetDTO payload) {
            super(LedgerSheetCode.PROJECT_CUMULATIVE_PAYMENT, payload);
        }
    }

    public static final class CumulativeTaxSummary23202355 extends ParsedSheetData<List<CumulativeTaxSummary23202355ColumnDTO>> {
        public CumulativeTaxSummary23202355(List<CumulativeTaxSummary23202355ColumnDTO> payload) {
            super(LedgerSheetCode.CUMULATIVE_TAX_SUMMARY_2320_2355, payload);
        }
    }

    public static final class VatChange extends ParsedSheetData<List<VatChangeRowDTO>> {
        public VatChange(List<VatChangeRowDTO> payload) { super(LedgerSheetCode.VAT_CHANGE, payload); }
    }

    public static final class VatTableOneCumulativeOutput extends ParsedSheetData<List<VatTableOneCumulativeOutputItemDTO>> {
        public VatTableOneCumulativeOutput(List<VatTableOneCumulativeOutputItemDTO> payload) {
            super(LedgerSheetCode.VAT_TABLE_ONE_CUMULATIVE_OUTPUT, payload);
        }
    }

    public static final class TaxAccountingDifferenceMonitor extends ParsedSheetData<List<TaxAccountingDifferenceMonitor23202355ItemDTO>> {
        public TaxAccountingDifferenceMonitor(List<TaxAccountingDifferenceMonitor23202355ItemDTO> payload) {
            super(LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR, payload);
        }
    }

    public static final class UninvoicedMonitor extends ParsedSheetData<List<UninvoicedMonitorItemDTO>> {
        public UninvoicedMonitor(List<UninvoicedMonitorItemDTO> payload) { super(LedgerSheetCode.UNINVOICED_MONITOR, payload); }
    }
}

