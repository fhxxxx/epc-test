package com.envision.epc.module.taxledger.application.ledger.builder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.BsLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import com.envision.epc.module.taxledger.domain.LedgerArtifactTypeEnum;
import com.envision.epc.module.taxledger.domain.LedgerRecord;
import com.envision.epc.module.taxledger.domain.LedgerRun;
import com.envision.epc.module.taxledger.domain.LedgerRunArtifact;
import com.envision.epc.module.taxledger.domain.LedgerRunStatusEnum;
import com.envision.epc.module.taxledger.infrastructure.FileRecordMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRecordMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunArtifactMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


/**
 * 资产负债表（BS） 页数据构建器。
 */
@Component
@RequiredArgsConstructor
public class BsSheetDataBuilder implements LedgerSheetDataBuilder<BsLedgerSheetData> {
    private final FileRecordMapper fileRecordMapper;
    private final LedgerRecordMapper ledgerRecordMapper;
    private final LedgerRunMapper ledgerRunMapper;
    private final LedgerRunArtifactMapper ledgerRunArtifactMapper;

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.BS;
    }

    @Override
    public BsLedgerSheetData build(LedgerBuildContext ctx) {
        String normalizedYearMonth = normalizeYearMonth(ctx.getYearMonth());
        int month = YearMonth.parse(normalizedYearMonth).getMonthValue();
        String targetSheetName = month + "月BS";

        String bsBlobPath = loadCurrentRequiredBlobPath(ctx, FileCategoryEnum.BS, "缺少BS上传文件");
        String bsAppendixBlobPath = loadCurrentRequiredBlobPath(ctx, FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE, "缺少BS附表上传文件");

        PreviousLedgerCandidate previous = findPreviousLedgerCandidate(ctx.getCompanyCode(), normalizedYearMonth);
        if (month == 1 || previous == null || previous.ledgerBlobPath == null || previous.ledgerBlobPath.isBlank()) {
            return new BsLedgerSheetData(
                    BsLedgerSheetData.RenderMode.FIRST_BUILD,
                    targetSheetName,
                    null,
                    bsBlobPath,
                    bsAppendixBlobPath,
                    null
            );
        }

        return new BsLedgerSheetData(
                BsLedgerSheetData.RenderMode.APPEND_ON_PREVIOUS,
                targetSheetName,
                previous.preferredBsSheetName,
                bsBlobPath,
                bsAppendixBlobPath,
                previous.ledgerBlobPath
        );
    }

    private String loadCurrentRequiredBlobPath(LedgerBuildContext ctx, FileCategoryEnum category, String errMsg) {
        if (ctx.getFiles() == null || ctx.getFiles().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, errMsg);
        }
        FileRecord latest = ctx.getFiles().stream()
                .filter(Objects::nonNull)
                .filter(file -> file.getIsDeleted() != null && file.getIsDeleted() == 0)
                .filter(file -> category == file.getFileCategory())
                .max(Comparator.comparing(file -> file.getId() == null ? 0L : file.getId()))
                .orElse(null);
        if (latest == null || latest.getBlobPath() == null || latest.getBlobPath().isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, errMsg);
        }
        return latest.getBlobPath();
    }

    private PreviousLedgerCandidate findPreviousLedgerCandidate(String companyCode, String currentYearMonth) {
        FileRecord fromFinalLedgerFile = fileRecordMapper.selectOne(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getIsDeleted, 0)
                .eq(FileRecord::getCompanyCode, companyCode)
                .eq(FileRecord::getFileCategory, FileCategoryEnum.FINAL_LEDGER)
                .lt(FileRecord::getYearMonth, currentYearMonth)
                .orderByDesc(FileRecord::getYearMonth)
                .orderByDesc(FileRecord::getId)
                .last("LIMIT 1"));
        if (fromFinalLedgerFile != null && fromFinalLedgerFile.getBlobPath() != null && !fromFinalLedgerFile.getBlobPath().isBlank()) {
            PreviousLedgerCandidate candidate = new PreviousLedgerCandidate();
            candidate.previousPeriodMonth = fromFinalLedgerFile.getYearMonth();
            candidate.preferredBsSheetName = toMonthBsSheetName(fromFinalLedgerFile.getYearMonth());
            candidate.ledgerBlobPath = fromFinalLedgerFile.getBlobPath();
            return candidate;
        }

        List<LedgerRecord> previousRecords = ledgerRecordMapper.selectList(new LambdaQueryWrapper<LedgerRecord>()
                .eq(LedgerRecord::getIsDeleted, 0)
                .eq(LedgerRecord::getCompanyCode, companyCode)
                .lt(LedgerRecord::getYearMonth, currentYearMonth)
                .orderByDesc(LedgerRecord::getYearMonth)
                .orderByDesc(LedgerRecord::getId));

        for (LedgerRecord record : previousRecords) {
            LedgerRun run = ledgerRunMapper.selectOne(new LambdaQueryWrapper<LedgerRun>()
                    .eq(LedgerRun::getIsDeleted, 0)
                    .eq(LedgerRun::getLedgerId, record.getId())
                    .eq(LedgerRun::getStatus, LedgerRunStatusEnum.SUCCESS)
                    .orderByDesc(LedgerRun::getRunNo)
                    .orderByDesc(LedgerRun::getId)
                    .last("LIMIT 1"));
            if (run == null) {
                continue;
            }
            LedgerRunArtifact artifact = ledgerRunArtifactMapper.selectOne(new LambdaQueryWrapper<LedgerRunArtifact>()
                    .eq(LedgerRunArtifact::getIsDeleted, 0)
                    .eq(LedgerRunArtifact::getRunId, run.getId())
                    .eq(LedgerRunArtifact::getArtifactType, LedgerArtifactTypeEnum.FINAL_LEDGER)
                    .orderByDesc(LedgerRunArtifact::getId)
                    .last("LIMIT 1"));
            if (artifact == null || artifact.getBlobPath() == null || artifact.getBlobPath().isBlank()) {
                continue;
            }
            PreviousLedgerCandidate candidate = new PreviousLedgerCandidate();
            candidate.previousPeriodMonth = record.getYearMonth();
            candidate.preferredBsSheetName = toMonthBsSheetName(record.getYearMonth());
            candidate.ledgerBlobPath = artifact.getBlobPath();
            return candidate;
        }
        return null;
    }

    private String normalizeYearMonth(String yearMonth) {
        if (yearMonth == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "yearMonth is blank");
        }
        String normalized = yearMonth.trim();
        if (normalized.matches("^\\d{6}$")) {
            normalized = normalized.substring(0, 4) + "-" + normalized.substring(4, 6);
        }
        if (!normalized.matches("^\\d{4}-\\d{2}$")) {
            throw new BizException(ErrorCode.BAD_REQUEST, "invalid yearMonth format: " + yearMonth);
        }
        return YearMonth.parse(normalized).toString();
    }

    private String toMonthBsSheetName(String yearMonth) {
        String normalized = normalizeYearMonth(yearMonth);
        int month = YearMonth.parse(normalized).getMonthValue();
        return month + "月BS";
    }

    private static class PreviousLedgerCandidate {
        private String previousPeriodMonth;
        private String preferredBsSheetName;
        private String ledgerBlobPath;
    }
}

