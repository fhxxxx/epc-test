package com.envision.epc.module.taxledger.web;

import com.envision.epc.module.taxledger.application.service.TaxConfigService;
import com.envision.epc.module.taxledger.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tax-ledger/config")
public class TaxConfigController {
    private final TaxConfigService configService;

    @GetMapping("/company-code")
    public List<TaxCompanyCodeConfig> listCompanyCodeConfig() {
        return configService.listCompanyCodeConfig();
    }

    @PostMapping("/company-code")
    public TaxCompanyCodeConfig saveCompanyCodeConfig(@RequestBody TaxCompanyCodeConfig command) {
        return configService.saveCompanyCodeConfig(command);
    }

    @DeleteMapping("/company-code/{id}")
    public void deleteCompanyCodeConfig(@PathVariable Long id) {
        configService.deleteCompanyCodeConfig(id);
    }

    @GetMapping("/category")
    public List<TaxCategoryConfig> listCategoryConfig(@RequestParam(required = false) String companyCode) {
        return configService.listCategoryConfig(companyCode);
    }

    @PostMapping("/category")
    public TaxCategoryConfig saveCategoryConfig(@RequestBody TaxCategoryConfig command) {
        return configService.saveCategoryConfig(command);
    }

    @DeleteMapping("/category/{id}")
    public void deleteCategoryConfig(@PathVariable Long id) {
        configService.deleteCategoryConfig(id);
    }

    @GetMapping("/project")
    public List<TaxProjectConfig> listProjectConfig(@RequestParam(required = false) String companyCode) {
        return configService.listProjectConfig(companyCode);
    }

    @PostMapping("/project")
    public TaxProjectConfig saveProjectConfig(@RequestBody TaxProjectConfig command) {
        return configService.saveProjectConfig(command);
    }

    @DeleteMapping("/project/{id}")
    public void deleteProjectConfig(@PathVariable Long id) {
        configService.deleteProjectConfig(id);
    }

    @GetMapping("/vat-basic")
    public List<TaxVatBasicItemConfig> listVatBasicConfig(@RequestParam(required = false) String companyCode) {
        return configService.listVatBasicItemConfig(companyCode);
    }

    @PostMapping("/vat-basic")
    public TaxVatBasicItemConfig saveVatBasicConfig(@RequestBody TaxVatBasicItemConfig command) {
        return configService.saveVatBasicItemConfig(command);
    }

    @DeleteMapping("/vat-basic/{id}")
    public void deleteVatBasicConfig(@PathVariable Long id) {
        configService.deleteVatBasicItemConfig(id);
    }

    @GetMapping("/vat-special")
    public List<TaxVatSpecialItemConfig> listVatSpecialConfig(@RequestParam(required = false) String companyCode) {
        return configService.listVatSpecialItemConfig(companyCode);
    }

    @PostMapping("/vat-special")
    public TaxVatSpecialItemConfig saveVatSpecialConfig(@RequestBody TaxVatSpecialItemConfig command) {
        return configService.saveVatSpecialItemConfig(command);
    }

    @DeleteMapping("/vat-special/{id}")
    public void deleteVatSpecialConfig(@PathVariable Long id) {
        configService.deleteVatSpecialItemConfig(id);
    }
}
