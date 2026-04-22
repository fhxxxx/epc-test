package com.envision.epc.module.taxledger.application.parse.parser.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.envision.epc.module.taxledger.application.dto.VatChangeAppendixUploadDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.ParserValueUtils;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 增值税变动表附表解析器。
 * 对应sheet页：增值税变动表附表
 * 对应类别：FileCategoryEnum.VAT_CHANGE_APPENDIX
 */
@Component
public class VatChangeAppendixSheetParser implements SheetParser<VatChangeAppendixUploadDTO> {
    private static final String TARGET_LABEL = "本期抵减预缴";

    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.VAT_CHANGE_APPENDIX;
    }

    @Override
    public Class<VatChangeAppendixUploadDTO> resultType() {
        return VatChangeAppendixUploadDTO.class;
    }

    @Override
    public ParseResult<VatChangeAppendixUploadDTO> parse(InputStream inputStream, ParseContext context) {
        VatChangeAppendixUploadDTO dto = new VatChangeAppendixUploadDTO();
        ParseResult<VatChangeAppendixUploadDTO> result = ParseResult.<VatChangeAppendixUploadDTO>builder()
                .data(dto)
                .build();
        if (inputStream == null) {
            result.addIssue("EMPTY_FILE: inputStream is null");
            return result;
        }

        AtomicReference<BigDecimal> extracted = new AtomicReference<>();
        try {
            EasyExcelFactory.read(inputStream, new AnalysisEventListener<Map<Integer, String>>() {
                        @Override
                        public void invoke(Map<Integer, String> rowData, AnalysisContext analysisContext) {
                            if (rowData == null || rowData.isEmpty() || extracted.get() != null) {
                                return;
                            }
                            Integer labelCol = findLabelColumn(rowData, TARGET_LABEL);
                            if (labelCol == null) {
                                return;
                            }
                            BigDecimal amount = findFirstNumericOnRight(rowData, labelCol);
                            if (amount != null) {
                                extracted.set(amount);
                            }
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                            // no-op
                        }
                    })
                    .headRowNumber(0)
                    .sheet()
                    .doRead();
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }

        if (extracted.get() == null) {
            result.addIssue("增值税变动表附表：未找到“本期抵减预缴”金额");
            return result;
        }
        dto.setCurrentPeriodPrepaidDeduction(extracted.get());
        return result;
    }

    private static Integer findLabelColumn(Map<Integer, String> rowData, String targetLabel) {
        for (Map.Entry<Integer, String> entry : rowData.entrySet()) {
            if (CharSequenceUtil.equals(CharSequenceUtil.trim(entry.getValue()), targetLabel)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static BigDecimal findFirstNumericOnRight(Map<Integer, String> rowData, Integer labelCol) {
        List<Integer> sortedCols = rowData.keySet().stream().sorted().toList();
        for (Integer col : sortedCols) {
            if (col <= labelCol) {
                continue;
            }
            BigDecimal amount = ParserValueUtils.toBigDecimal(rowData.get(col));
            if (amount != null) {
                return amount;
            }
        }
        return null;
    }
}
