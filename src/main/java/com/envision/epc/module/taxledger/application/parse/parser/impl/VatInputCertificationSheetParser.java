package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.application.dto.VatInputCertificationItemDTO;
import com.envision.epc.module.taxledger.application.parse.EngineType;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.AbstractEasyExcelSheetParser;
import com.envision.epc.module.taxledger.application.parse.parser.ParserValueUtils;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 增值税进项认证清单解析器。
 * 对应sheet页：增值税进项认证清单
 * 对应类别：FileCategoryEnum.VAT_INPUT_CERT
 */
@Component
public class VatInputCertificationSheetParser extends AbstractEasyExcelSheetParser<List<VatInputCertificationItemDTO>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.VAT_INPUT_CERT;
    }

    @Override
    protected int headerRowIndex() {
        return 2;
    }

    @Override
    protected int dataStartRowIndex() {
        return 3;
    }

    @Override
    public Class<List<VatInputCertificationItemDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<VatInputCertificationItemDTO>> cls = (Class<List<VatInputCertificationItemDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    protected List<VatInputCertificationItemDTO> mapToDto(HeaderData headerData,
                                                          BodyData bodyData,
                                                          ParseContext context,
                                                          ParseResult<List<VatInputCertificationItemDTO>> result) {
        List<VatInputCertificationItemDTO> rows = new ArrayList<>();
        for (List<String> row : bodyData.rows()) {
            String serialNo = findCell(row, headerData, "序号");
            String invoiceNo = findCell(row, headerData, "发票号码");
            if ((serialNo == null || serialNo.isBlank()) && (invoiceNo == null || invoiceNo.isBlank())) {
                continue;
            }
            VatInputCertificationItemDTO dto = new VatInputCertificationItemDTO();
            dto.setSerialNo(serialNo);
            dto.setSelectionStatus(findCell(row, headerData, "勾选状态"));
            dto.setInvoiceSource(findCell(row, headerData, "发票来源"));
            dto.setTransferToDomesticProofNo(findCell(row, headerData, "转内销证明编号"));
            dto.setDigitalInvoiceNo(findCell(row, headerData, "数电票号码"));
            dto.setInvoiceCode(findCell(row, headerData, "发票代码"));
            dto.setInvoiceNo(invoiceNo);
            dto.setInvoiceDate(findCell(row, headerData, "开票日期"));
            dto.setSellerTaxpayerId(findCell(row, headerData, "销售方纳税人识别号"));
            dto.setSellerTaxpayerName(findCell(row, headerData, "销售方纳税人名称"));
            dto.setAmount(ParserValueUtils.toBigDecimal(findCell(row, headerData, "金额")));
            dto.setTaxAmount(ParserValueUtils.toBigDecimal(findCell(row, headerData, "税额")));
            dto.setEffectiveDeductibleTaxAmount(ParserValueUtils.toBigDecimal(findCell(row, headerData, "有效抵扣税额")));
            dto.setInvoiceType(findCell(row, headerData, "票种"));
            dto.setInvoiceTypeTag(findCell(row, headerData, "票种标签"));
            dto.setInvoiceStatus(findCell(row, headerData, "发票状态"));
            dto.setSelectionTime(findCell(row, headerData, "勾选时间"));
            dto.setInvoiceRiskLevel(findCell(row, headerData, "发票风险等级"));
            dto.setRiskStatus(findCell(row, headerData, "风险状态"));
            rows.add(dto);
        }
        return rows;
    }

    @Override
    protected EngineType engineType() {
        return EngineType.EASY_EXCEL;
    }
}
