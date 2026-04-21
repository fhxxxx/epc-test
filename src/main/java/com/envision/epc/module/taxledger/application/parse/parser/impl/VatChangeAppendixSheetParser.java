package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.application.parse.EngineType;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.ParseSeverity;
import com.envision.epc.module.taxledger.application.parse.parser.AbstractAsposeSheetParser;
import com.envision.epc.module.taxledger.application.parse.parser.ParserValueUtils;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 增值税变动表附表解析器。
 * 对应sheet页：增值税变动表附表
 * 对应类别：FileCategoryEnum.VAT_CHANGE_APPENDIX
 */
@Component
public class VatChangeAppendixSheetParser extends AbstractAsposeSheetParser<List<Map<String, BigDecimal>>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.VAT_CHANGE_APPENDIX;
    }

    @Override
    public Class<List<Map<String, BigDecimal>>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<Map<String, BigDecimal>>> cls = (Class<List<Map<String, BigDecimal>>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    protected List<Map<String, BigDecimal>> mapToDto(HeaderData headerData,
                                                     BodyData bodyData,
                                                     ParseContext context,
                                                     ParseResult<List<Map<String, BigDecimal>>> result) {
        for (List<String> row : bodyData.rows()) {
            for (int i = 0; i < row.size(); i++) {
                if (normalize(row.get(i)).contains(normalize("本期抵减预缴"))) {
                    BigDecimal value = null;
                    for (int j = i + 1; j < row.size(); j++) {
                        value = ParserValueUtils.toBigDecimal(row.get(j));
                        if (value != null) {
                            break;
                        }
                    }
                    if (value == null) {
                        value = BigDecimal.ZERO;
                    }
                    return List.of(Map.of("currentPeriodPrepaidDeduction", value));
                }
            }
        }
        result.addIssue(ParseSeverity.WARN, "KEY_NOT_FOUND", "currentPeriodPrepaidDeduction not found");
        return List.of();
    }

    @Override
    protected EngineType engineType() {
        return EngineType.ASPOSE;
    }
}
