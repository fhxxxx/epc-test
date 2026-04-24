package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.dto.MonthlyTaxSectionDTO;
import com.envision.epc.module.taxledger.application.dto.PlAppendix23202355DTO;
import com.envision.epc.module.taxledger.application.dto.PrecheckSnapshotDTO;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileParseStatusEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import com.envision.epc.module.taxledger.infrastructure.FileRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 生成前置校验与快照组装
 */
@Service
@RequiredArgsConstructor
public class LedgerPrecheckService {
    private static final int WAIT_PARSE_MAX_SECONDS = 300;

    private final FileRecordMapper fileRecordMapper;
    private final FileParseOrchestratorService fileParseOrchestratorService;
    private final ParsedResultReader parsedResultReader;
    private final TaxLedgerService taxLedgerService;

    public PrecheckSnapshotDTO precheck(String companyCode, String periodMonth) {
        List<String> errors = new ArrayList<>();
        Map<FileCategoryEnum, FileRecord> latestByCategory = latestFileByCategory(companyCode, periodMonth);
        Set<FileCategoryEnum> required = requiredCategories(companyCode);
        List<PrecheckSnapshotDTO.InputItem> inputs = new ArrayList<>();
        Map<FileCategoryEnum, FileRecord> parsedFiles = new HashMap<>();

        for (FileCategoryEnum category : required) {
            FileRecord file = latestByCategory.get(category);
            if (file == null) {
                errors.add("缺少文件: " + categoryDisplayName(category));
                continue;
            }
            FileRecord parsed = ensureParseReady(file, category, errors);
            if (parsed == null) {
                continue;
            }
            parsedFiles.put(category, parsed);
            inputs.add(toInputItem(parsed));
        }

        if (!errors.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, String.join("; ", errors));
        }

        PrecheckSnapshotDTO snapshot = new PrecheckSnapshotDTO();
        snapshot.setCompanyCode(companyCode);
        snapshot.setPeriodMonth(periodMonth);
        snapshot.setRequiredCategories(required.stream().map(Enum::name).sorted().toList());
        snapshot.setInputs(inputs);
        snapshot.setFingerprint(UUID.randomUUID().toString().replace("-", ""));
        snapshot.setGeneratedAt(LocalDateTime.now());

        if (isCompany2320Or2355(companyCode)) {
            FileRecord n30File = parsedFiles.get(FileCategoryEnum.PL_APPENDIX_2320);
            FileRecord monthlyFile = parsedFiles.get(FileCategoryEnum.MONTHLY_SETTLEMENT_TAX);
            if (n30File == null || monthlyFile == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "缺少N30校验所需文件");
            }
            PlAppendix23202355DTO uploaded =
                    parsedResultReader.readParsedData(n30File.getParseResultBlobPath(), PlAppendix23202355DTO.class);
            List<MonthlyTaxSectionDTO> monthlyRows =
                    parsedResultReader.readParsedList(monthlyFile.getParseResultBlobPath(), MonthlyTaxSectionDTO.class);
            TaxLedgerService.N30ValidationResult validated = taxLedgerService.validateAndNormalizeN30(uploaded, monthlyRows);
            snapshot.setN30NormalizedData(validated.getData());
            snapshot.setValidationDetails(validated.getValidationDetails());
        }
        return snapshot;
    }

    private FileRecord ensureParseReady(FileRecord file,
                                        FileCategoryEnum category,
                                        List<String> errors) {
        FileRecord latest = refresh(file.getId());
        if (latest == null || latest.getIsDeleted() == 1) {
            errors.add("文件不存在: " + categoryDisplayName(category));
            return null;
        }
        if (latest.getParseStatus() == FileParseStatusEnum.FAILED) {
            errors.add("文件解析失败: " + categoryDisplayName(category) + " - " + safe(latest.getParseErrorMsg()));
            return null;
        }
        if (latest.getParseStatus() == FileParseStatusEnum.PENDING) {
            fileParseOrchestratorService.parseAsync(latest.getId(), "system");
        }
        if (latest.getParseStatus() == FileParseStatusEnum.PENDING
                || latest.getParseStatus() == FileParseStatusEnum.PARSING) {
            latest = waitParseTerminal(latest.getId());
        }
        if (latest == null || latest.getIsDeleted() == 1) {
            errors.add("文件不存在: " + categoryDisplayName(category));
            return null;
        }
        if (latest.getParseStatus() == FileParseStatusEnum.FAILED) {
            errors.add("文件解析失败: " + categoryDisplayName(category) + " - " + safe(latest.getParseErrorMsg()));
            return null;
        }
        if (latest.getParseStatus() != FileParseStatusEnum.SUCCESS
                || !StringUtils.hasText(latest.getParseResultBlobPath())) {
            errors.add("文件解析未完成: " + categoryDisplayName(category));
            return null;
        }
        return latest;
    }

    private FileRecord waitParseTerminal(Long fileId) {
        int waited = 0;
        while (waited < WAIT_PARSE_MAX_SECONDS) {
            FileRecord latest = refresh(fileId);
            if (latest == null || latest.getIsDeleted() == 1) {
                return latest;
            }
            if (latest.getParseStatus() == FileParseStatusEnum.SUCCESS
                    || latest.getParseStatus() == FileParseStatusEnum.FAILED) {
                return latest;
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "等待文件解析被中断");
            }
            waited++;
        }
        throw new BizException(ErrorCode.BAD_REQUEST, "等待文件解析超时");
    }

    private FileRecord refresh(Long id) {
        return fileRecordMapper.selectById(id);
    }

    private Map<FileCategoryEnum, FileRecord> latestFileByCategory(String companyCode, String periodMonth) {
        List<FileRecord> files = fileRecordMapper.selectList(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getIsDeleted, 0)
                .eq(FileRecord::getCompanyCode, companyCode)
                .eq(FileRecord::getYearMonth, periodMonth)
                .orderByDesc(FileRecord::getUpdateTime)
                .orderByDesc(FileRecord::getId));
        Map<FileCategoryEnum, FileRecord> map = new HashMap<>();
        for (FileRecord file : files) {
            if (file.getFileCategory() == null || map.containsKey(file.getFileCategory())) {
                continue;
            }
            map.put(file.getFileCategory(), file);
        }
        return map;
    }

    private Set<FileCategoryEnum> requiredCategories(String companyCode) {
        Set<FileCategoryEnum> required = EnumSet.of(
                FileCategoryEnum.BS,
                FileCategoryEnum.PL,
                FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE,
                FileCategoryEnum.VAT_OUTPUT,
                FileCategoryEnum.VAT_INPUT_CERT,
                FileCategoryEnum.VAT_CHANGE_APPENDIX,
                FileCategoryEnum.DL_INCOME,
                FileCategoryEnum.DL_OUTPUT,
                FileCategoryEnum.DL_INPUT,
                FileCategoryEnum.DL_INCOME_TAX,
                FileCategoryEnum.DL_OTHER
        );
        if (isCompany2320Or2355(companyCode)) {
            required.add(FileCategoryEnum.PL_APPENDIX_2320);
            required.add(FileCategoryEnum.MONTHLY_SETTLEMENT_TAX);
            required.add(FileCategoryEnum.STAMP_TAX);
            required.add(FileCategoryEnum.CUMULATIVE_PROJECT_TAX);
        } else {
            required.add(FileCategoryEnum.PL_APPENDIX_PROJECT);
            required.add(FileCategoryEnum.CONTRACT_STAMP_DUTY_LEDGER);
        }
        return required;
    }

    private PrecheckSnapshotDTO.InputItem toInputItem(FileRecord record) {
        PrecheckSnapshotDTO.InputItem item = new PrecheckSnapshotDTO.InputItem();
        item.setFileId(record.getId());
        item.setFileName(record.getFileName());
        item.setFileCategory(record.getFileCategory() == null ? null : record.getFileCategory().name());
        item.setParseStatus(record.getParseStatus() == null ? null : record.getParseStatus().name());
        item.setParseResultBlobPath(record.getParseResultBlobPath());
        item.setFileSize(record.getFileSize());
        return item;
    }

    private boolean isCompany2320Or2355(String companyCode) {
        return "2320".equals(companyCode) || "2355".equals(companyCode);
    }

    private String categoryDisplayName(FileCategoryEnum category) {
        if (category == null) {
            return "";
        }
        if (!StringUtils.hasText(category.getDisplayName())) {
            return category.name();
        }
        return category.getDisplayName();
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "unknown error";
    }
}

