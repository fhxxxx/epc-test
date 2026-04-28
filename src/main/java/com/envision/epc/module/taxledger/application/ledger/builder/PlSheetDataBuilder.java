package com.envision.epc.module.taxledger.application.ledger.builder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.dto.PlAppendix23202355DTO;
import com.envision.epc.module.taxledger.application.dto.PlAppendixProjectCompanyUploadDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.PlLedgerSheetData;
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
 * 利润表（PL） 页数据构建器。
 */
@Component
@RequiredArgsConstructor
public class PlSheetDataBuilder implements LedgerSheetDataBuilder<PlLedgerSheetData> {
    private final FileRecordMapper fileRecordMapper;
    private final LedgerRecordMapper ledgerRecordMapper;
    private final LedgerRunMapper ledgerRunMapper;
    private final LedgerRunArtifactMapper ledgerRunArtifactMapper;
    private final PlAppendix2320SheetDataBuilder plAppendix2320SheetDataBuilder;
    private final PlAppendixProjectSheetDataBuilder plAppendixProjectSheetDataBuilder;

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.PL;
    }

    @Override
    public PlLedgerSheetData build(LedgerBuildContext ctx) {
        String normalizedYearMonth = normalizeYearMonth(ctx.getYearMonth());
        int month = YearMonth.parse(normalizedYearMonth).getMonthValue();
        String targetSheetName = month + "月PL";
        String currentPlBlobPath = loadCurrentRequiredBlobPath(ctx, FileCategoryEnum.PL, "缺少PL上传文件");
        PreviousLedgerCandidate previous = findPreviousLedgerCandidate(ctx.getCompanyCode(), normalizedYearMonth);
        boolean hasPrevious = previous != null && previous.ledgerBlobPath != null && !previous.ledgerBlobPath.isBlank();

        boolean is2320Or2355 = isCompany2320Or2355(ctx.getCompanyCode());
        PlAppendix23202355DTO appendix2320Data = null;
        List<PlAppendixProjectCompanyUploadDTO> appendixProjectData = null;
        if (is2320Or2355) {
            appendix2320Data = plAppendix2320SheetDataBuilder.build(ctx).getPayload();
        } else {
            appendixProjectData = plAppendixProjectSheetDataBuilder.build(ctx).getPayload();
        }

        if (month == 1 || !hasPrevious) {
            return new PlLedgerSheetData(
                    PlLedgerSheetData.RenderMode.FIRST_BUILD,
                    targetSheetName,
                    null,
                    currentPlBlobPath,
                    null,
                    appendix2320Data,
                    appendixProjectData
            );
        }

        return new PlLedgerSheetData(
                PlLedgerSheetData.RenderMode.APPEND_ON_PREVIOUS,
                targetSheetName,
                previous.preferredPlSheetName,
                currentPlBlobPath,
                previous.ledgerBlobPath,
                appendix2320Data,
                appendixProjectData
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
            candidate.preferredPlSheetName = toMonthPlSheetName(fromFinalLedgerFile.getYearMonth());
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
            candidate.preferredPlSheetName = toMonthPlSheetName(record.getYearMonth());
            candidate.ledgerBlobPath = artifact.getBlobPath();
            return candidate;
        }
        return null;
    }

    private boolean isCompany2320Or2355(String companyCode) {
        return "2320".equals(companyCode) || "2355".equals(companyCode);
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

    private String toMonthPlSheetName(String yearMonth) {
        String normalized = normalizeYearMonth(yearMonth);
        int month = YearMonth.parse(normalized).getMonthValue();
        return month + "月PL";
    }

    private static class PreviousLedgerCandidate {
        private String previousPeriodMonth;
        private String preferredPlSheetName;
        private String ledgerBlobPath;
    }
}

