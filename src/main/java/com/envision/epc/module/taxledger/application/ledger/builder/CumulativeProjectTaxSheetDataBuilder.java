package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.CumulativeProjectTaxLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Objects;

/**
 * 累计项目税收明细表 页数据构建器。
 * 仅返回源文件路径，避免在 builder 阶段加载整本Workbook。
 */
@Component
public class CumulativeProjectTaxSheetDataBuilder implements LedgerSheetDataBuilder<CumulativeProjectTaxLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.CUMULATIVE_PROJECT_TAX;
    }

    @Override
    public CumulativeProjectTaxLedgerSheetData build(LedgerBuildContext ctx) {
        if (ctx.getFiles() == null || ctx.getFiles().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装Sheet数据失败: 累计项目税收明细表，缺少输入文件");
        }
        FileRecord latest = ctx.getFiles().stream()
                .filter(Objects::nonNull)
                .filter(file -> file.getIsDeleted() != null && file.getIsDeleted() == 0)
                .filter(file -> file.getFileCategory() == FileCategoryEnum.CUMULATIVE_PROJECT_TAX)
                .max(Comparator.comparing(file -> file.getId() == null ? 0L : file.getId()))
                .orElse(null);
        if (latest == null || latest.getBlobPath() == null || latest.getBlobPath().isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装Sheet数据失败: 累计项目税收明细表，缺少输入文件类别=累计项目税收明细表");
        }
        return new CumulativeProjectTaxLedgerSheetData(
                latest.getBlobPath(),
                FileCategoryEnum.CUMULATIVE_PROJECT_TAX.getTargetSheetName());
    }
}

