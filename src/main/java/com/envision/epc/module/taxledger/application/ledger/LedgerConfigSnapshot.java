package com.envision.epc.module.taxledger.application.ledger;

import com.envision.epc.module.taxledger.domain.CompanyCodeConfig;
import com.envision.epc.module.taxledger.domain.ProjectConfig;
import com.envision.epc.module.taxledger.domain.TaxCategoryConfig;
import com.envision.epc.module.taxledger.domain.VatBasicItemConfig;
import lombok.Builder;
import lombok.Value;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 台账构建过程预加载的配置快照
 */
@Value
@Builder
public class LedgerConfigSnapshot {
    String companyCode;
    CompanyCodeConfig currentCompany;
    List<CompanyCodeConfig> companyCodeConfigs;
    List<TaxCategoryConfig> taxCategoryConfigs;
    List<ProjectConfig> projectConfigs;
    List<VatBasicItemConfig> vatBasicItemConfigs;

    public Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("companyCode", companyCode);
        result.put("hasCurrentCompany", currentCompany != null);
        result.put("companyCodeConfigCount", safeSize(companyCodeConfigs));
        result.put("taxCategoryConfigCount", safeSize(taxCategoryConfigs));
        result.put("projectConfigCount", safeSize(projectConfigs));
        result.put("vatBasicItemConfigCount", safeSize(vatBasicItemConfigs));
        return result;
    }

    private int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }
}

