package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileParseStatusEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import com.envision.epc.module.taxledger.infrastructure.FileRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pre-generation validation
 */
@Service
@RequiredArgsConstructor
public class LedgerValidationService {
    private final FileRecordMapper fileRecordMapper;

    public List<String> validate(String companyCode, String yearMonth) {
        List<String> errors = new ArrayList<>();
        Map<FileCategoryEnum, FileRecord> latestByCategory = latestFileByCategory(companyCode, yearMonth);
        Set<FileCategoryEnum> required = requiredCategories(companyCode);

        for (FileCategoryEnum category : required) {
            FileRecord file = latestByCategory.get(category);
            if (file == null) {
                errors.add("Missing file: " + category.name());
                continue;
            }
            if (file.getParseStatus() != FileParseStatusEnum.SUCCESS) {
                errors.add("File parse not ready: " + category.name());
            }
        }
        return errors;
    }

    private Set<FileCategoryEnum> requiredCategories(String companyCode) {
        Set<FileCategoryEnum> required = EnumSet.of(
                FileCategoryEnum.BS,
                FileCategoryEnum.PL,
                FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE,
                FileCategoryEnum.STAMP_TAX,
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
        } else {
            required.add(FileCategoryEnum.PL_APPENDIX_PROJECT);
            required.add(FileCategoryEnum.CUMULATIVE_PROJECT_TAX);
            required.add(FileCategoryEnum.CONTRACT_STAMP_DUTY_LEDGER);
        }
        return required;
    }

    private Map<FileCategoryEnum, FileRecord> latestFileByCategory(String companyCode, String yearMonth) {
        List<FileRecord> files = fileRecordMapper.selectList(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getIsDeleted, 0)
                .eq(FileRecord::getCompanyCode, companyCode)
                .eq(FileRecord::getYearMonth, yearMonth)
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

    private boolean isCompany2320Or2355(String companyCode) {
        return "2320".equals(companyCode) || "2355".equals(companyCode);
    }
}