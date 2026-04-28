package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.SourceCopyLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;

import java.util.Comparator;
import java.util.Objects;

/**
 * 纯复制页 Builder 抽象基类。
 */
public abstract class AbstractSourceCopySheetDataBuilder implements LedgerSheetDataBuilder<SourceCopyLedgerSheetData> {
    protected abstract FileCategoryEnum sourceCategory();

    protected String sourceSheetName() {
        return sourceCategory().getTargetSheetName();
    }

    protected boolean fallbackToFirstSheet() {
        return true;
    }

    @Override
    public SourceCopyLedgerSheetData build(LedgerBuildContext ctx) {
        LedgerSheetCode code = support();
        FileCategoryEnum category = sourceCategory();
        if (ctx.getFiles() == null || ctx.getFiles().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装Sheet数据失败: " + code.getDisplayName()
                    + "，缺少输入文件类别=" + category.getDisplayName());
        }

        FileRecord latest = ctx.getFiles().stream()
                .filter(Objects::nonNull)
                .filter(file -> file.getIsDeleted() != null && file.getIsDeleted() == 0)
                .filter(file -> category == file.getFileCategory())
                .max(Comparator.comparing(file -> file.getId() == null ? 0L : file.getId()))
                .orElse(null);
        if (latest == null || latest.getBlobPath() == null || latest.getBlobPath().isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装Sheet数据失败: " + code.getDisplayName()
                    + "，缺少输入文件类别=" + category.getDisplayName());
        }

        return new SourceCopyLedgerSheetData(code, latest.getBlobPath(), sourceSheetName(), fallbackToFirstSheet());
    }
}

