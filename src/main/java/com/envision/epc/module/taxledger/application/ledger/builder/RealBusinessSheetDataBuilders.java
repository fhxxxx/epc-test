package com.envision.epc.module.taxledger.application.ledger.builder;

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
import com.envision.epc.module.taxledger.application.dto.VatTableOneCumulativeOutputItemDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.data.BusinessSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

@Component
class BsSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.Bs> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.BS; }
    @Override public BusinessSheetData.Bs build(LedgerBuildContext ctx) {
        return new BusinessSheetData.Bs(readList(ctx, FileCategoryEnum.BS, BsStatementRowDTO.class));
    }
}

@Component
class PlSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.Pl> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.PL; }
    @Override public BusinessSheetData.Pl build(LedgerBuildContext ctx) {
        return new BusinessSheetData.Pl(readList(ctx, FileCategoryEnum.PL, PlStatementRowDTO.class));
    }
}

@Component
class BsAppendixSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.BsAppendix> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.BS_APPENDIX; }
    @Override public BusinessSheetData.BsAppendix build(LedgerBuildContext ctx) {
        return new BusinessSheetData.BsAppendix(readList(ctx, FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE, BsAppendixUploadDTO.class));
    }
}

@Component
class PlAppendix2320SheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.PlAppendix2320> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.PL_APPENDIX_2320; }
    @Override public BusinessSheetData.PlAppendix2320 build(LedgerBuildContext ctx) {
        return new BusinessSheetData.PlAppendix2320(readObject(ctx, FileCategoryEnum.PL_APPENDIX_2320, PlAppendix23202355DTO.class));
    }
}

@Component
class PlAppendixProjectSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.PlAppendixProject> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.PL_APPENDIX_PROJECT; }
    @Override public BusinessSheetData.PlAppendixProject build(LedgerBuildContext ctx) {
        return new BusinessSheetData.PlAppendixProject(readList(ctx, FileCategoryEnum.PL_APPENDIX_PROJECT, PlAppendixProjectCompanyUploadDTO.class));
    }
}

@Component
class StampTaxSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.StampTax> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.STAMP_TAX; }
    @Override public BusinessSheetData.StampTax build(LedgerBuildContext ctx) {
        return new BusinessSheetData.StampTax(readList(ctx, FileCategoryEnum.STAMP_TAX, StampDutySummaryRowDTO.class));
    }
}

@Component
class StampTaxProjectSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.StampTaxProject> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.STAMP_TAX_PROJECT; }
    @Override public BusinessSheetData.StampTaxProject build(LedgerBuildContext ctx) {
        return new BusinessSheetData.StampTaxProject(readList(ctx, FileCategoryEnum.STAMP_TAX_PROJECT, StampDutySummaryRowDTO.class));
    }
}

@Component
class VatOutputSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.VatOutputSource> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.VAT_OUTPUT; }
    @Override public BusinessSheetData.VatOutputSource build(LedgerBuildContext ctx) {
        return new BusinessSheetData.VatOutputSource(
                ctx.getParsedDataGateway().openSourceWorkbook(FileCategoryEnum.VAT_OUTPUT),
                FileCategoryEnum.VAT_OUTPUT.getTargetSheetName()
        );
    }
}

@Component
class VatInputCertSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.VatInputCertSource> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.VAT_INPUT_CERT; }
    @Override public BusinessSheetData.VatInputCertSource build(LedgerBuildContext ctx) {
        return new BusinessSheetData.VatInputCertSource(
                ctx.getParsedDataGateway().openSourceWorkbook(FileCategoryEnum.VAT_INPUT_CERT),
                FileCategoryEnum.VAT_INPUT_CERT.getTargetSheetName()
        );
    }
}

@Component
class CumulativeProjectTaxSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.CumulativeProjectTax> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.CUMULATIVE_PROJECT_TAX; }
    @Override public BusinessSheetData.CumulativeProjectTax build(LedgerBuildContext ctx) {
        return new BusinessSheetData.CumulativeProjectTax(readByCatalog(ctx, FileCategoryEnum.CUMULATIVE_PROJECT_TAX));
    }
}

@Component
class VatChangeAppendixSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.VatChangeAppendix> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.VAT_CHANGE_APPENDIX; }
    @Override public BusinessSheetData.VatChangeAppendix build(LedgerBuildContext ctx) {
        return new BusinessSheetData.VatChangeAppendix(readObject(ctx, FileCategoryEnum.VAT_CHANGE_APPENDIX, VatChangeAppendixUploadDTO.class));
    }
}

@Component
class ContractStampDutyLedgerSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.ContractStampDutyLedger> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.CONTRACT_STAMP_DUTY_LEDGER; }
    @Override public BusinessSheetData.ContractStampDutyLedger build(LedgerBuildContext ctx) {
        return new BusinessSheetData.ContractStampDutyLedger(readList(ctx, FileCategoryEnum.CONTRACT_STAMP_DUTY_LEDGER, ContractStampDutyLedgerItemDTO.class));
    }
}

@Component
class ProjectCumulativeDeclarationSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.ProjectCumulativeDeclaration> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.PROJECT_CUMULATIVE_DECLARATION; }
    @Override public BusinessSheetData.ProjectCumulativeDeclaration build(LedgerBuildContext ctx) {
        return new BusinessSheetData.ProjectCumulativeDeclaration(readObject(ctx, FileCategoryEnum.PROJECT_CUMULATIVE_DECLARATION, ProjectCumulativeDeclarationSheetDTO.class));
    }
}

@Component
class ProjectCumulativePaymentSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.ProjectCumulativePayment> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.PROJECT_CUMULATIVE_PAYMENT; }
    @Override public BusinessSheetData.ProjectCumulativePayment build(LedgerBuildContext ctx) {
        return new BusinessSheetData.ProjectCumulativePayment(readObject(ctx, FileCategoryEnum.PROJECT_CUMULATIVE_PAYMENT, ProjectCumulativePaymentSheetDTO.class));
    }
}

@Component
class CumulativeTaxSummary23202355SheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.CumulativeTaxSummary23202355> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.CUMULATIVE_TAX_SUMMARY_2320_2355; }
    @Override public BusinessSheetData.CumulativeTaxSummary23202355 build(LedgerBuildContext ctx) {
        return new BusinessSheetData.CumulativeTaxSummary23202355(readList(ctx, FileCategoryEnum.CUMULATIVE_TAX_SUMMARY_2320_2355, CumulativeTaxSummary23202355ColumnDTO.class));
    }
}

@Component
class VatChangeSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.VatChange> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.VAT_CHANGE; }
    @Override public BusinessSheetData.VatChange build(LedgerBuildContext ctx) {
        return new BusinessSheetData.VatChange(readList(ctx, FileCategoryEnum.VAT_CHANGE, VatChangeRowDTO.class));
    }
}

@Component
class VatTableOneCumulativeOutputSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.VatTableOneCumulativeOutput> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.VAT_TABLE_ONE_CUMULATIVE_OUTPUT; }
    @Override public BusinessSheetData.VatTableOneCumulativeOutput build(LedgerBuildContext ctx) {
        return new BusinessSheetData.VatTableOneCumulativeOutput(readList(ctx, FileCategoryEnum.VAT_TABLE_ONE_CUMULATIVE_OUTPUT, VatTableOneCumulativeOutputItemDTO.class));
    }
}

@Component
class TaxAccountingDifferenceMonitorSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.TaxAccountingDifferenceMonitor> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR; }
    @Override public BusinessSheetData.TaxAccountingDifferenceMonitor build(LedgerBuildContext ctx) {
        return new BusinessSheetData.TaxAccountingDifferenceMonitor(readList(ctx, FileCategoryEnum.TAX_ACCOUNTING_DIFFERENCE_MONITOR, TaxAccountingDifferenceMonitor23202355ItemDTO.class));
    }
}

@Component
class UninvoicedMonitorSheetDataBuilder extends AbstractParsedBusinessSheetDataBuilder<BusinessSheetData.UninvoicedMonitor> {
    @Override protected LedgerSheetCode code() { return LedgerSheetCode.UNINVOICED_MONITOR; }
    @Override public BusinessSheetData.UninvoicedMonitor build(LedgerBuildContext ctx) {
        return new BusinessSheetData.UninvoicedMonitor(readList(ctx, FileCategoryEnum.UNINVOICED_MONITOR, UninvoicedMonitorItemDTO.class));
    }
}
