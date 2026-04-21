package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.application.dto.ContractStampDutyLedgerItemDTO;
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
 * 合同印花税明细台账解析器。
 * 对应sheet页：合同印花税明细台账
 * 对应类别：FileCategoryEnum.CONTRACT_STAMP_DUTY_LEDGER
 */
@Component
public class ContractStampDutyLedgerSheetParser extends AbstractEasyExcelSheetParser<List<ContractStampDutyLedgerItemDTO>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.CONTRACT_STAMP_DUTY_LEDGER;
    }

    @Override
    protected int headerRowIndex() {
        return 0;
    }

    @Override
    protected int dataStartRowIndex() {
        return 1;
    }

    @Override
    public Class<List<ContractStampDutyLedgerItemDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<ContractStampDutyLedgerItemDTO>> cls = (Class<List<ContractStampDutyLedgerItemDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    protected List<ContractStampDutyLedgerItemDTO> mapToDto(HeaderData headerData,
                                                            BodyData bodyData,
                                                            ParseContext context,
                                                            ParseResult<List<ContractStampDutyLedgerItemDTO>> result) {
        List<ContractStampDutyLedgerItemDTO> rows = new ArrayList<>();
        for (List<String> row : bodyData.rows()) {
            String serialNo = findCell(row, headerData, "序号");
            String contractCode = findCell(row, headerData, "单号/合同编码");
            if ((serialNo == null || serialNo.isBlank()) && (contractCode == null || contractCode.isBlank())) {
                continue;
            }
            ContractStampDutyLedgerItemDTO dto = new ContractStampDutyLedgerItemDTO();
            dto.setSerialNo(serialNo);
            dto.setQuarter(findCell(row, headerData, "季度"));
            dto.setContractNoOrCode(contractCode);
            dto.setSupplier(findCell(row, headerData, "供应商"));
            dto.setContractContent(findCell(row, headerData, "合同内容"));
            dto.setContractAmountInclTax(ParserValueUtils.toBigDecimal(findCell(row, headerData, "合同金额(含税)", "合同金额（含税）")));
            dto.setTaxRate(ParserValueUtils.toBigDecimal(findCell(row, headerData, "税率")));
            dto.setContractAmount(ParserValueUtils.toBigDecimal(findCell(row, headerData, "合同金额")));
            dto.setStampDutyTaxItem(findCell(row, headerData, "印花税税目"));
            dto.setStampDutyTaxRate(ParserValueUtils.toBigDecimal(findCell(row, headerData, "印花税税率")));
            dto.setTaxableAmount(ParserValueUtils.toBigDecimal(findCell(row, headerData, "应纳税额(元)", "应纳税额（元）")));
            dto.setPreferentialRatio(ParserValueUtils.toBigDecimal(findCell(row, headerData, "优惠比例")));
            dto.setActualTaxPaid(ParserValueUtils.toBigDecimal(findCell(row, headerData, "实缴税额(元)", "实缴税额（元）")));
            dto.setDeclarationDate(findCell(row, headerData, "申报日期"));
            rows.add(dto);
        }
        return rows;
    }

    @Override
    protected EngineType engineType() {
        return EngineType.EASY_EXCEL;
    }
}
