package com.envision.epc.module.taxledger.excel.renderer;

import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.data.BusinessSheetData;
import org.springframework.stereotype.Component;

@Component
class BsSheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.Bs> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.BS; }
}

@Component
class PlSheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.Pl> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.PL; }
}

@Component
class BsAppendixSheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.BsAppendix> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.BS_APPENDIX; }
}

@Component
class PlAppendix2320SheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.PlAppendix2320> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.PL_APPENDIX_2320; }
}

@Component
class PlAppendixProjectSheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.PlAppendixProject> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.PL_APPENDIX_PROJECT; }
}

@Component
class StampTaxSheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.StampTax> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.STAMP_TAX; }
}

@Component
class StampTaxProjectSheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.StampTaxProject> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.STAMP_TAX_PROJECT; }
}

@Component
class VatOutputSheetRenderer extends AbstractSourceSheetCopyRenderer<BusinessSheetData.VatOutputSource> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.VAT_OUTPUT; }
}

@Component
class VatInputCertSheetRenderer extends AbstractSourceSheetCopyRenderer<BusinessSheetData.VatInputCertSource> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.VAT_INPUT_CERT; }
}

@Component
class CumulativeProjectTaxSheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.CumulativeProjectTax> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.CUMULATIVE_PROJECT_TAX; }
}

@Component
class VatChangeAppendixSheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.VatChangeAppendix> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.VAT_CHANGE_APPENDIX; }
}

@Component
class ContractStampDutyLedgerSheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.ContractStampDutyLedger> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.CONTRACT_STAMP_DUTY_LEDGER; }
}

@Component
class ProjectCumulativeDeclarationSheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.ProjectCumulativeDeclaration> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.PROJECT_CUMULATIVE_DECLARATION; }
}

@Component
class ProjectCumulativePaymentSheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.ProjectCumulativePayment> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.PROJECT_CUMULATIVE_PAYMENT; }
}

@Component
class CumulativeTaxSummary23202355SheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.CumulativeTaxSummary23202355> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.CUMULATIVE_TAX_SUMMARY_2320_2355; }
}

@Component
class VatChangeSheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.VatChange> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.VAT_CHANGE; }
}

@Component
class VatTableOneCumulativeOutputSheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.VatTableOneCumulativeOutput> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.VAT_TABLE_ONE_CUMULATIVE_OUTPUT; }
}

@Component
class TaxAccountingDifferenceMonitorSheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.TaxAccountingDifferenceMonitor> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR; }
}

@Component
class UninvoicedMonitorSheetRenderer extends AbstractParsedBusinessSheetRenderer<BusinessSheetData.UninvoicedMonitor> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.UNINVOICED_MONITOR; }
}
