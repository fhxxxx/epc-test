package com.envision.epc.module.taxledger.application.ledger.builder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
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
import java.util.List;

/**
 * 前序最终台账定位器（优先 FINAL_LEDGER 文件，其次成功 run 的最终产物）。
 * todo 后续确认
 */
@Component
@RequiredArgsConstructor
public class PreviousLedgerLocator {
    private final FileRecordMapper fileRecordMapper;
    private final LedgerRecordMapper ledgerRecordMapper;
    private final LedgerRunMapper ledgerRunMapper;
    private final LedgerRunArtifactMapper ledgerRunArtifactMapper;

    public PreviousLedgerRef find(String companyCode, String currentYearMonth) {
        String normalizedYm = normalizeYearMonth(currentYearMonth);
        FileRecord fromFinalLedgerFile = fileRecordMapper.selectOne(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getIsDeleted, 0)
                .eq(FileRecord::getCompanyCode, companyCode)
                .eq(FileRecord::getFileCategory, FileCategoryEnum.FINAL_LEDGER)
                .lt(FileRecord::getYearMonth, normalizedYm)
                .orderByDesc(FileRecord::getYearMonth)
                .orderByDesc(FileRecord::getId)
                .last("LIMIT 1"));
        if (fromFinalLedgerFile != null
                && fromFinalLedgerFile.getBlobPath() != null
                && !fromFinalLedgerFile.getBlobPath().isBlank()) {
            return new PreviousLedgerRef(fromFinalLedgerFile.getYearMonth(), fromFinalLedgerFile.getBlobPath());
        }

        List<LedgerRecord> previousRecords = ledgerRecordMapper.selectList(new LambdaQueryWrapper<LedgerRecord>()
                .eq(LedgerRecord::getIsDeleted, 0)
                .eq(LedgerRecord::getCompanyCode, companyCode)
                .lt(LedgerRecord::getYearMonth, normalizedYm)
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
            return new PreviousLedgerRef(record.getYearMonth(), artifact.getBlobPath());
        }
        return null;
    }

    public static String normalizeYearMonth(String yearMonth) {
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

    public record PreviousLedgerRef(String previousPeriodMonth, String ledgerBlobPath) {
    }
}
