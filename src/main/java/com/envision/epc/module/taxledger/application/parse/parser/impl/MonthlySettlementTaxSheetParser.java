package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.application.dto.MonthlyTaxSectionDTO;
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
 * 睿景景程月结数据表-报税解析器。
 * 对应sheet页：睿景景程月结数据表-报税
 * 对应类别：FileCategoryEnum.MONTHLY_SETTLEMENT_TAX
 */
@Component
public class MonthlySettlementTaxSheetParser extends AbstractAsposeSheetParser<List<MonthlyTaxSectionDTO>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.MONTHLY_SETTLEMENT_TAX;
    }

    @Override
    public Class<List<MonthlyTaxSectionDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<MonthlyTaxSectionDTO>> cls = (Class<List<MonthlyTaxSectionDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    protected List<MonthlyTaxSectionDTO> mapToDto(HeaderData headerData,
                                                  BodyData bodyData,
                                                  ParseContext context,
                                                  ParseResult<List<MonthlyTaxSectionDTO>> result) {
        List<MonthlyTaxSectionDTO> sections = new ArrayList<>();
        for (List<String> row : bodyData.rows()) {
            String title = row.isEmpty() ? "" : row.get(0);
            if (title == null || title.isBlank()) {
                continue;
            }
            MonthlyTaxSectionDTO dto = new MonthlyTaxSectionDTO();
            dto.setTitle(title);
            dto.setCost(ParserValueUtils.toBigDecimal(findCell(row, headerData, "成本")));
            dto.setIncome(ParserValueUtils.toBigDecimal(findCell(row, headerData, "收入")));
            dto.setOutputTax(ParserValueUtils.toBigDecimal(findCell(row, headerData, "销项")));
            dto.setInvoicedIncome(ParserValueUtils.toBigDecimal(findCell(row, headerData, "已开票收入")));
            dto.setInvoicedTaxAmount(ParserValueUtils.toBigDecimal(findCell(row, headerData, "已开票税额")));
            dto.setUninvoicedIncome(ParserValueUtils.toBigDecimal(findCell(row, headerData, "未开票收入")));
            dto.setUninvoicedTaxAmount(ParserValueUtils.toBigDecimal(findCell(row, headerData, "未开票税额")));
            sections.add(dto);
        }
        if (sections.isEmpty()) {
            result.addIssue(ParseSeverity.WARN, "NO_SECTION_DATA", "monthly tax section rows not parsed");
        }
        return sections;
    }

    @Override
    protected EngineType engineType() {
        return EngineType.ASPOSE;
    }
}
