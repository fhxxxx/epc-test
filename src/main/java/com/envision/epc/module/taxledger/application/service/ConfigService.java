package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.module.taxledger.domain.*;
import com.envision.epc.module.taxledger.infrastructure.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 配置表统一服务（5张配置表）
 */
@Service
@RequiredArgsConstructor
public class ConfigService {
    private final CompanyCodeConfigMapper companyCodeConfigMapper;
    private final TaxCategoryConfigMapper categoryConfigMapper;
    private final ProjectConfigMapper projectConfigMapper;
    private final VatBasicItemConfigMapper vatBasicItemConfigMapper;
    private final VatSpecialItemConfigMapper vatSpecialItemConfigMapper;

    /**
     * 查询公司代码配置
     */
    public List<CompanyCodeConfig> listCompanyCodeConfig() {
        return companyCodeConfigMapper.selectList(new LambdaQueryWrapper<CompanyCodeConfig>()
                .eq(CompanyCodeConfig::getIsDeleted, 0));
    }

    /**
     * 保存公司代码配置
     */
    public CompanyCodeConfig saveCompanyCodeConfig(CompanyCodeConfig config) {
        config.setIsDeleted(0);
        if (config.getId() == null || companyCodeConfigMapper.selectById(config.getId()) == null) {
            companyCodeConfigMapper.insert(config);
        } else {
            companyCodeConfigMapper.updateById(config);
        }
        return config;
    }

    /**
     * 删除公司代码配置（逻辑删除）
     */
    public void deleteCompanyCodeConfig(Long id) {
        CompanyCodeConfig config = companyCodeConfigMapper.selectById(id);
        if (config != null) {
            config.setIsDeleted(1);
            companyCodeConfigMapper.updateById(config);
        }
    }

    /**
     * 查询税目配置（通用+公司覆盖）
     */
    public List<TaxCategoryConfig> listCategoryConfig(String companyCode) {
        LambdaQueryWrapper<TaxCategoryConfig> wrapper = new LambdaQueryWrapper<TaxCategoryConfig>()
                .eq(TaxCategoryConfig::getIsDeleted, 0);
        if (StringUtils.hasText(companyCode)) {
            wrapper.and(w -> w.isNull(TaxCategoryConfig::getCompanyCode)
                    .or().eq(TaxCategoryConfig::getCompanyCode, companyCode));
        }
        wrapper.orderByAsc(TaxCategoryConfig::getSeqNo);
        return categoryConfigMapper.selectList(wrapper);
    }

    /**
     * 保存税目配置
     */
    public TaxCategoryConfig saveCategoryConfig(TaxCategoryConfig config) {
        config.setIsDeleted(0);
        if (config.getId() == null || categoryConfigMapper.selectById(config.getId()) == null) {
            categoryConfigMapper.insert(config);
        } else {
            categoryConfigMapper.updateById(config);
        }
        return config;
    }

    /**
     * 删除税目配置（逻辑删除）
     */
    public void deleteCategoryConfig(Long id) {
        TaxCategoryConfig config = categoryConfigMapper.selectById(id);
        if (config != null) {
            config.setIsDeleted(1);
            categoryConfigMapper.updateById(config);
        }
    }

    /**
     * 查询项目配置
     */
    public List<ProjectConfig> listProjectConfig(String companyCode) {
        return projectConfigMapper.selectList(new LambdaQueryWrapper<ProjectConfig>()
                .eq(ProjectConfig::getIsDeleted, 0)
                .eq(StringUtils.hasText(companyCode), ProjectConfig::getCompanyCode, companyCode));
    }

    /**
     * 保存项目配置
     */
    public ProjectConfig saveProjectConfig(ProjectConfig config) {
        config.setIsDeleted(0);
        if (config.getId() == null || projectConfigMapper.selectById(config.getId()) == null) {
            projectConfigMapper.insert(config);
        } else {
            projectConfigMapper.updateById(config);
        }
        return config;
    }

    /**
     * 删除项目配置（逻辑删除）
     */
    public void deleteProjectConfig(Long id) {
        ProjectConfig config = projectConfigMapper.selectById(id);
        if (config != null) {
            config.setIsDeleted(1);
            projectConfigMapper.updateById(config);
        }
    }

    /**
     * 查询增值税基础条目配置（通用+公司覆盖）
     */
    public List<VatBasicItemConfig> listVatBasicItemConfig(String companyCode) {
        LambdaQueryWrapper<VatBasicItemConfig> wrapper = new LambdaQueryWrapper<VatBasicItemConfig>()
                .eq(VatBasicItemConfig::getIsDeleted, 0);
        if (StringUtils.hasText(companyCode)) {
            wrapper.and(w -> w.isNull(VatBasicItemConfig::getCompanyCode)
                    .or().eq(VatBasicItemConfig::getCompanyCode, companyCode));
        }
        wrapper.orderByAsc(VatBasicItemConfig::getItemSeq);
        return vatBasicItemConfigMapper.selectList(wrapper);
    }

    /**
     * 保存增值税基础条目配置
     */
    public VatBasicItemConfig saveVatBasicItemConfig(VatBasicItemConfig config) {
        config.setIsDeleted(0);
        if (config.getId() == null || vatBasicItemConfigMapper.selectById(config.getId()) == null) {
            vatBasicItemConfigMapper.insert(config);
        } else {
            vatBasicItemConfigMapper.updateById(config);
        }
        return config;
    }

    /**
     * 删除增值税基础条目配置（逻辑删除）
     */
    public void deleteVatBasicItemConfig(Long id) {
        VatBasicItemConfig config = vatBasicItemConfigMapper.selectById(id);
        if (config != null) {
            config.setIsDeleted(1);
            vatBasicItemConfigMapper.updateById(config);
        }
    }

    /**
     * 查询增值税特殊条目配置
     */
    public List<VatSpecialItemConfig> listVatSpecialItemConfig(String companyCode) {
        return vatSpecialItemConfigMapper.selectList(new LambdaQueryWrapper<VatSpecialItemConfig>()
                .eq(VatSpecialItemConfig::getIsDeleted, 0)
                .eq(StringUtils.hasText(companyCode), VatSpecialItemConfig::getCompanyCode, companyCode)
                .orderByAsc(VatSpecialItemConfig::getItemSeq));
    }

    /**
     * 保存增值税特殊条目配置
     */
    public VatSpecialItemConfig saveVatSpecialItemConfig(VatSpecialItemConfig config) {
        config.setIsDeleted(0);
        if (config.getId() == null || vatSpecialItemConfigMapper.selectById(config.getId()) == null) {
            vatSpecialItemConfigMapper.insert(config);
        } else {
            vatSpecialItemConfigMapper.updateById(config);
        }
        return config;
    }

    /**
     * 删除增值税特殊条目配置（逻辑删除）
     */
    public void deleteVatSpecialItemConfig(Long id) {
        VatSpecialItemConfig config = vatSpecialItemConfigMapper.selectById(id);
        if (config != null) {
            config.setIsDeleted(1);
            vatSpecialItemConfigMapper.updateById(config);
        }
    }
}
