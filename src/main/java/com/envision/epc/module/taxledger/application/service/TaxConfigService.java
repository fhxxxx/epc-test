package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.module.taxledger.domain.*;
import com.envision.epc.module.taxledger.infrastructure.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxConfigService {
    private final TaxCompanyCodeConfigMapper companyCodeConfigMapper;
    private final TaxCategoryConfigMapper categoryConfigMapper;
    private final TaxProjectConfigMapper projectConfigMapper;
    private final TaxVatBasicItemConfigMapper vatBasicItemConfigMapper;
    private final TaxVatSpecialItemConfigMapper vatSpecialItemConfigMapper;

    public List<TaxCompanyCodeConfig> listCompanyCodeConfig() {
        return companyCodeConfigMapper.selectList(new LambdaQueryWrapper<TaxCompanyCodeConfig>()
                .eq(TaxCompanyCodeConfig::getIsDeleted, 0));
    }

    public TaxCompanyCodeConfig saveCompanyCodeConfig(TaxCompanyCodeConfig config) {
        config.setIsDeleted(0);
        if (config.getId() == null || companyCodeConfigMapper.selectById(config.getId()) == null) {
            companyCodeConfigMapper.insert(config);
        } else {
            companyCodeConfigMapper.updateById(config);
        }
        return config;
    }

    public void deleteCompanyCodeConfig(Long id) {
        TaxCompanyCodeConfig config = companyCodeConfigMapper.selectById(id);
        if (config != null) {
            config.setIsDeleted(1);
            companyCodeConfigMapper.updateById(config);
        }
    }

    public List<TaxCategoryConfig> listCategoryConfig(String companyCode) {
        return categoryConfigMapper.selectList(new LambdaQueryWrapper<TaxCategoryConfig>()
                .eq(TaxCategoryConfig::getIsDeleted, 0)
                .and(wrapper -> wrapper.isNull(TaxCategoryConfig::getCompanyCode).or().eq(TaxCategoryConfig::getCompanyCode, companyCode))
                .orderByAsc(TaxCategoryConfig::getSeqNo));
    }

    public TaxCategoryConfig saveCategoryConfig(TaxCategoryConfig config) {
        config.setIsDeleted(0);
        if (config.getId() == null || categoryConfigMapper.selectById(config.getId()) == null) {
            categoryConfigMapper.insert(config);
        } else {
            categoryConfigMapper.updateById(config);
        }
        return config;
    }

    public void deleteCategoryConfig(Long id) {
        TaxCategoryConfig config = categoryConfigMapper.selectById(id);
        if (config != null) {
            config.setIsDeleted(1);
            categoryConfigMapper.updateById(config);
        }
    }

    public List<TaxProjectConfig> listProjectConfig(String companyCode) {
        return projectConfigMapper.selectList(new LambdaQueryWrapper<TaxProjectConfig>()
                .eq(TaxProjectConfig::getIsDeleted, 0)
                .eq(companyCode != null, TaxProjectConfig::getCompanyCode, companyCode));
    }

    public TaxProjectConfig saveProjectConfig(TaxProjectConfig config) {
        config.setIsDeleted(0);
        if (config.getId() == null || projectConfigMapper.selectById(config.getId()) == null) {
            projectConfigMapper.insert(config);
        } else {
            projectConfigMapper.updateById(config);
        }
        return config;
    }

    public void deleteProjectConfig(Long id) {
        TaxProjectConfig config = projectConfigMapper.selectById(id);
        if (config != null) {
            config.setIsDeleted(1);
            projectConfigMapper.updateById(config);
        }
    }

    public List<TaxVatBasicItemConfig> listVatBasicItemConfig(String companyCode) {
        return vatBasicItemConfigMapper.selectList(new LambdaQueryWrapper<TaxVatBasicItemConfig>()
                .eq(TaxVatBasicItemConfig::getIsDeleted, 0)
                .and(wrapper -> wrapper.isNull(TaxVatBasicItemConfig::getCompanyCode).or().eq(TaxVatBasicItemConfig::getCompanyCode, companyCode))
                .orderByAsc(TaxVatBasicItemConfig::getItemSeq));
    }

    public TaxVatBasicItemConfig saveVatBasicItemConfig(TaxVatBasicItemConfig config) {
        config.setIsDeleted(0);
        if (config.getId() == null || vatBasicItemConfigMapper.selectById(config.getId()) == null) {
            vatBasicItemConfigMapper.insert(config);
        } else {
            vatBasicItemConfigMapper.updateById(config);
        }
        return config;
    }

    public void deleteVatBasicItemConfig(Long id) {
        TaxVatBasicItemConfig config = vatBasicItemConfigMapper.selectById(id);
        if (config != null) {
            config.setIsDeleted(1);
            vatBasicItemConfigMapper.updateById(config);
        }
    }

    public List<TaxVatSpecialItemConfig> listVatSpecialItemConfig(String companyCode) {
        return vatSpecialItemConfigMapper.selectList(new LambdaQueryWrapper<TaxVatSpecialItemConfig>()
                .eq(TaxVatSpecialItemConfig::getIsDeleted, 0)
                .eq(companyCode != null, TaxVatSpecialItemConfig::getCompanyCode, companyCode)
                .orderByAsc(TaxVatSpecialItemConfig::getItemSeq));
    }

    public TaxVatSpecialItemConfig saveVatSpecialItemConfig(TaxVatSpecialItemConfig config) {
        config.setIsDeleted(0);
        if (config.getId() == null || vatSpecialItemConfigMapper.selectById(config.getId()) == null) {
            vatSpecialItemConfigMapper.insert(config);
        } else {
            vatSpecialItemConfigMapper.updateById(config);
        }
        return config;
    }

    public void deleteVatSpecialItemConfig(Long id) {
        TaxVatSpecialItemConfig config = vatSpecialItemConfigMapper.selectById(id);
        if (config != null) {
            config.setIsDeleted(1);
            vatSpecialItemConfigMapper.updateById(config);
        }
    }
}
