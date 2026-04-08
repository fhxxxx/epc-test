package com.envision.epc.module.taxledger.web;

import com.envision.epc.module.taxledger.application.service.TaxConfigService;
import com.envision.epc.module.taxledger.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 配置管理接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/tax-ledger/config")
public class TaxConfigController {
    private final TaxConfigService configService;

    /**
     * 查询公司代码配置
     */
    @GetMapping("/company-code")
    public List<TaxCompanyCodeConfig> listCompanyCodeConfig() {
        return configService.listCompanyCodeConfig();
    }

    /**
     * 保存公司代码配置
     */
    @PostMapping("/company-code")
    public TaxCompanyCodeConfig saveCompanyCodeConfig(@RequestBody TaxCompanyCodeConfig command) {
        return configService.saveCompanyCodeConfig(command);
    }

    /**
     * 删除公司代码配置
     */
    @DeleteMapping("/company-code/{id}")
    public void deleteCompanyCodeConfig(@PathVariable Long id) {
        configService.deleteCompanyCodeConfig(id);
    }

    /**
     * 查询税目配置
     */
    @GetMapping("/category")
    public List<TaxCategoryConfig> listCategoryConfig(@RequestParam(required = false) String companyCode) {
        return configService.listCategoryConfig(companyCode);
    }

    /**
     * 保存税目配置
     */
    @PostMapping("/category")
    public TaxCategoryConfig saveCategoryConfig(@RequestBody TaxCategoryConfig command) {
        return configService.saveCategoryConfig(command);
    }

    /**
     * 删除税目配置
     */
    @DeleteMapping("/category/{id}")
    public void deleteCategoryConfig(@PathVariable Long id) {
        configService.deleteCategoryConfig(id);
    }

    /**
     * 查询项目配置
     */
    @GetMapping("/project")
    public List<TaxProjectConfig> listProjectConfig(@RequestParam(required = false) String companyCode) {
        return configService.listProjectConfig(companyCode);
    }

    /**
     * 保存项目配置
     */
    @PostMapping("/project")
    public TaxProjectConfig saveProjectConfig(@RequestBody TaxProjectConfig command) {
        return configService.saveProjectConfig(command);
    }

    /**
     * 删除项目配置
     */
    @DeleteMapping("/project/{id}")
    public void deleteProjectConfig(@PathVariable Long id) {
        configService.deleteProjectConfig(id);
    }

    /**
     * 查询增值税基础条目配置
     */
    @GetMapping("/vat-basic")
    public List<TaxVatBasicItemConfig> listVatBasicConfig(@RequestParam(required = false) String companyCode) {
        return configService.listVatBasicItemConfig(companyCode);
    }

    /**
     * 保存增值税基础条目配置
     */
    @PostMapping("/vat-basic")
    public TaxVatBasicItemConfig saveVatBasicConfig(@RequestBody TaxVatBasicItemConfig command) {
        return configService.saveVatBasicItemConfig(command);
    }

    /**
     * 删除增值税基础条目配置
     */
    @DeleteMapping("/vat-basic/{id}")
    public void deleteVatBasicConfig(@PathVariable Long id) {
        configService.deleteVatBasicItemConfig(id);
    }

    /**
     * 查询增值税特殊条目配置
     */
    @GetMapping("/vat-special")
    public List<TaxVatSpecialItemConfig> listVatSpecialConfig(@RequestParam(required = false) String companyCode) {
        return configService.listVatSpecialItemConfig(companyCode);
    }

    /**
     * 保存增值税特殊条目配置
     */
    @PostMapping("/vat-special")
    public TaxVatSpecialItemConfig saveVatSpecialConfig(@RequestBody TaxVatSpecialItemConfig command) {
        return configService.saveVatSpecialItemConfig(command);
    }

    /**
     * 删除增值税特殊条目配置
     */
    @DeleteMapping("/vat-special/{id}")
    public void deleteVatSpecialConfig(@PathVariable Long id) {
        configService.deleteVatSpecialItemConfig(id);
    }
}
