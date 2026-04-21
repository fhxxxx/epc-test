package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.application.dto.VatOutputSheetUploadDTO;
import com.envision.epc.module.taxledger.application.parse.EngineType;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.ParseSeverity;
import com.envision.epc.module.taxledger.application.parse.parser.AbstractAsposeSheetParser;
import com.envision.epc.module.taxledger.application.parse.parser.ParserValueUtils;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 增值税销项解析器（双section）。
 * 对应sheet页：增值税销项
 * 对应类别：FileCategoryEnum.VAT_OUTPUT
 */
@Component
public class VatOutputSheetParser extends AbstractAsposeSheetParser<VatOutputSheetUploadDTO> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.VAT_OUTPUT;
    }

    @Override
    public Class<VatOutputSheetUploadDTO> resultType() {
        return VatOutputSheetUploadDTO.class;
    }

    @Override
    protected VatOutputSheetUploadDTO mapToDto(HeaderData headerData,
                                               BodyData bodyData,
                                               ParseContext context,
                                               ParseResult<VatOutputSheetUploadDTO> result) {
        VatOutputSheetUploadDTO dto = new VatOutputSheetUploadDTO();
        List<VatOutputSheetUploadDTO.InvoiceDetailItem> detailItems = new ArrayList<>();
        List<VatOutputSheetUploadDTO.TaxRateSummaryItem> summaryItems = new ArrayList<>();

        for (List<String> row : bodyData.rows()) {
            String invoiceCode = findCell(row, headerData, "发票代码");
            String invoiceNo = findCell(row, headerData, "发票号码");
            if (!invoiceCode.isBlank() || !invoiceNo.isBlank()) {
                VatOutputSheetUploadDTO.InvoiceDetailItem detail = new VatOutputSheetUploadDTO.InvoiceDetailItem();
                detail.setSerialNo(findCell(row, headerData, "序号"));
                detail.setInvoiceCode(invoiceCode);
                detail.setInvoiceNo(invoiceNo);
                detail.setDigitalInvoiceNo(findCell(row, headerData, "数电发票号码"));
                detail.setSellerTaxpayerId(findCell(row, headerData, "销方识别号"));
                detail.setSellerName(findCell(row, headerData, "销方名称"));
                detail.setBuyerTaxpayerId(findCell(row, headerData, "购方识别号"));
                detail.setBuyerName(findCell(row, headerData, "购方名称"));
                detail.setInvoiceDate(findCell(row, headerData, "开票日期"));
                detail.setTaxClassificationCode(findCell(row, headerData, "税收分类编码"));
                detail.setSpecificBusinessType(findCell(row, headerData, "特定业务类型"));
                detailItems.add(detail);
                continue;
            }

            String invoiceStatus = findCell(row, headerData, "发票状态");
            String taxRate = findCell(row, headerData, "税率/征收率");
            if (!invoiceStatus.isBlank() || !taxRate.isBlank()) {
                VatOutputSheetUploadDTO.TaxRateSummaryItem summary = new VatOutputSheetUploadDTO.TaxRateSummaryItem();
                summary.setSerialNo(findCell(row, headerData, "序号"));
                summary.setInvoiceStatus(invoiceStatus);
                summary.setTaxRateOrLevyRate(ParserValueUtils.toBigDecimal(taxRate));
                summary.setBlueInvoiceAmount(ParserValueUtils.toBigDecimal(findCell(row, headerData, "开具蓝字发票金额")));
                summary.setBlueInvoiceTaxAmount(ParserValueUtils.toBigDecimal(findCell(row, headerData, "开具蓝字发票税额")));
                summary.setCanceledBlueInvoiceAmount(ParserValueUtils.toBigDecimal(findCell(row, headerData, "作废蓝字发票金额")));
                summary.setCanceledBlueInvoiceTaxAmount(ParserValueUtils.toBigDecimal(findCell(row, headerData, "作废蓝字发票税额")));
                summary.setRedInvoiceAmount(ParserValueUtils.toBigDecimal(findCell(row, headerData, "开具红字发票金额")));
                summary.setRedInvoiceTaxAmount(ParserValueUtils.toBigDecimal(findCell(row, headerData, "开具红字发票税额")));
                summary.setCanceledRedInvoiceAmount(ParserValueUtils.toBigDecimal(findCell(row, headerData, "作废红字发票金额")));
                summary.setCanceledRedInvoiceTaxAmount(ParserValueUtils.toBigDecimal(findCell(row, headerData, "作废红字发票税额")));
                summaryItems.add(summary);
            }
        }

        if (detailItems.isEmpty() && summaryItems.isEmpty()) {
            result.addIssue(ParseSeverity.WARN, "NO_DATA", "No invoice detail or tax-rate summary row parsed");
        }
        dto.setInvoiceDetails(detailItems);
        dto.setTaxRateSummaries(summaryItems);
        return dto;
    }

    @Override
    protected EngineType engineType() {
        return EngineType.ASPOSE;
    }
}
