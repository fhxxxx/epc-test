package com.envision.epc.module.taxledger.application.service;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.dto.SummarySheetDTO;
import com.envision.epc.module.taxledger.domain.FileRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Summary 页数据聚合器。
 */
@Service
public class SummarySheetDataAssembler {

    public SummarySheetDTO assemble(String companyCode,
                                    String yearMonth,
                                    List<FileRecord> files,
                                    Map<String, Object> nodeOutputs) {
        if (isBlank(companyCode) || isBlank(yearMonth)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "companyCode/yearMonth cannot be blank for summary assemble");
        }

        SummarySheetDTO dto = new SummarySheetDTO();
        dto.setCompanyName(companyCode);
        dto.setLedgerPeriod(normalizeYearMonth(yearMonth));
        dto.setDeclarationDate(LocalDate.now().toString());

        List<SummarySheetDTO.StampDutyItem> stampRows = buildStampDutyRows(nodeOutputs);
        dto.setStampDutyRows(stampRows);
        dto.setCommonTaxRows(new ArrayList<>());
        dto.setCorporateIncomeTaxRows(new ArrayList<>());

        SummarySheetDTO.FinalTotalItem finalTotal = new SummarySheetDTO.FinalTotalItem();
        finalTotal.setTotalTitle("合计");
        finalTotal.setDeclaredTotal(sumStampDeclared(stampRows));
        finalTotal.setBookTotal(BigDecimal.ZERO);
        dto.setFinalTotal(finalTotal);
        return dto;
    }

    @SuppressWarnings("unchecked")
    private List<SummarySheetDTO.StampDutyItem> buildStampDutyRows(Map<String, Object> nodeOutputs) {
        List<SummarySheetDTO.StampDutyItem> result = new ArrayList<>();
        if (nodeOutputs == null || nodeOutputs.isEmpty()) {
            return result;
        }
        Object n20Obj = nodeOutputs.get("N20");
        if (!(n20Obj instanceof Map<?, ?> n20Map)) {
            return result;
        }
        Object outputDataObj = n20Map.get("outputData");
        if (!(outputDataObj instanceof Map<?, ?> outputData)) {
            return result;
        }
        Object stampObj = outputData.get("stampDutyNon23202355");
        if (!(stampObj instanceof Map<?, ?> stampMap)) {
            return result;
        }
        Object rowsObj = stampMap.get("rows");
        if (!(rowsObj instanceof List<?> rows)) {
            return result;
        }

        for (Object rowObj : rows) {
            if (!(rowObj instanceof Map<?, ?> rowMap)) {
                continue;
            }
            String serialNo = asString(rowMap.get("serialNo"));
            if ("合计".equals(serialNo)) {
                continue;
            }
            SummarySheetDTO.StampDutyItem item = new SummarySheetDTO.StampDutyItem();
            item.setSeqNo(parseInt(serialNo));
            item.setTaxType("印花税");
            item.setTaxItem(asString(rowMap.get("taxItem")));
            item.setTaxBasisDesc("");
            item.setTaxBaseQuarter(asBigDecimal(rowMap.get("taxableBasis")));
            item.setLevyRatio(null);
            item.setTaxRate(asBigDecimal(rowMap.get("taxRate")));
            item.setActualTaxPayable(asBigDecimal(rowMap.get("taxPayableAmount")));
            item.setTaxBaseMonth1(null);
            item.setTaxBaseMonth2(null);
            item.setTaxBaseMonth3(null);
            item.setVarianceReason("");
            result.add(item);
        }
        return result;
    }

    private BigDecimal sumStampDeclared(List<SummarySheetDTO.StampDutyItem> rows) {
        BigDecimal sum = BigDecimal.ZERO;
        for (SummarySheetDTO.StampDutyItem row : rows) {
            if (row == null || row.getActualTaxPayable() == null) {
                continue;
            }
            sum = sum.add(row.getActualTaxPayable());
        }
        return sum;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        String text = asString(value);
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.replace(",", ""));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value).trim();
    }

    private String normalizeYearMonth(String yearMonth) {
        String text = yearMonth.trim();
        if (text.matches("^\\d{6}$")) {
            return text.substring(0, 4) + "-" + text.substring(4, 6);
        }
        return text;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
