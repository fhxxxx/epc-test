package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.module.taxledger.application.dto.PlStatementRowDTO;
import com.envision.epc.module.taxledger.application.ledger.SummaryQuarterSnapshot;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import com.envision.epc.module.taxledger.infrastructure.FileRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SummaryQuarterDataPreparer {
    private static final String MAIN_REVENUE_ITEM = "主营业务收入";

    private final FileRecordMapper fileRecordMapper;
    private final ParsedResultReader parsedResultReader;

    public SummaryQuarterSnapshot prepare(String companyCode, String yearMonth) {
        YearMonth target = parseYearMonth(yearMonth);
        List<String> quarterMonths = quarterMonths(target);
        Map<String, BigDecimal> plMainRevenueByMonth = new LinkedHashMap<>();
        List<String> missingMonths = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (String month : quarterMonths) {
            FileRecord plFile = latestPlFile(companyCode, month);
            if (plFile == null) {
                missingMonths.add(month);
                warnings.add("summary quarter preload: missing PL file, month=" + month);
                plMainRevenueByMonth.put(month, null);
                continue;
            }
            if (plFile.getParseResultBlobPath() == null || plFile.getParseResultBlobPath().isBlank()) {
                missingMonths.add(month);
                warnings.add("summary quarter preload: PL parse result missing, month=" + month + ", fileId=" + plFile.getId());
                plMainRevenueByMonth.put(month, null);
                continue;
            }
            try {
                List<PlStatementRowDTO> plRows = parsedResultReader.readParsedList(plFile.getParseResultBlobPath(), PlStatementRowDTO.class);
                BigDecimal revenue = sumMainBusinessRevenue(plRows);
                if (revenue == null) {
                    missingMonths.add(month);
                    warnings.add("summary quarter preload: PL main revenue not found, month=" + month + ", fileId=" + plFile.getId());
                }
                plMainRevenueByMonth.put(month, revenue);
            } catch (Exception ex) {
                missingMonths.add(month);
                warnings.add("summary quarter preload: PL parse read failed, month=" + month + ", fileId=" + plFile.getId()
                        + ", reason=" + ex.getMessage());
                plMainRevenueByMonth.put(month, null);
            }
        }

        return SummaryQuarterSnapshot.builder()
                .quarterMonths(quarterMonths)
                .plMainRevenueByMonth(plMainRevenueByMonth)
                .missingMonths(missingMonths)
                .warnings(warnings)
                .build();
    }

    private FileRecord latestPlFile(String companyCode, String yearMonth) {
        return fileRecordMapper.selectOne(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getIsDeleted, 0)
                .eq(FileRecord::getCompanyCode, companyCode)
                .eq(FileRecord::getYearMonth, yearMonth)
                .eq(FileRecord::getFileCategory, FileCategoryEnum.PL)
                .orderByDesc(FileRecord::getId)
                .last("LIMIT 1"));
    }

    private BigDecimal sumMainBusinessRevenue(List<PlStatementRowDTO> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        boolean hit = false;
        for (PlStatementRowDTO row : rows) {
            if (row == null || row.getItemName() == null) {
                continue;
            }
            String item = row.getItemName().replace(" ", "").trim();
            if (!item.contains(MAIN_REVENUE_ITEM)) {
                continue;
            }
            hit = true;
            if (row.getCurrentPeriodAmount() != null) {
                total = total.add(row.getCurrentPeriodAmount());
            }
        }
        return hit ? total : null;
    }

    private YearMonth parseYearMonth(String yearMonth) {
        if (yearMonth == null) {
            throw new IllegalArgumentException("yearMonth is null");
        }
        String text = yearMonth.trim();
        if (text.matches("^\\d{6}$")) {
            text = text.substring(0, 4) + "-" + text.substring(4, 6);
        }
        return YearMonth.parse(text);
    }

    private List<String> quarterMonths(YearMonth ym) {
        int month = ym.getMonthValue();
        int quarterStart = ((month - 1) / 3) * 3 + 1;
        YearMonth m1 = YearMonth.of(ym.getYear(), quarterStart);
        return List.of(m1.toString(), m1.plusMonths(1).toString(), m1.plusMonths(2).toString());
    }
}

