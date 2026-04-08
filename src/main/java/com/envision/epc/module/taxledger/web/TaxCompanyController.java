package com.envision.epc.module.taxledger.web;

import com.envision.epc.module.taxledger.application.command.UpsertCompanyCommand;
import com.envision.epc.module.taxledger.application.service.TaxCompanyService;
import com.envision.epc.module.taxledger.domain.TaxCompany;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 公司管理接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/tax-ledger/companies")
public class TaxCompanyController {
    private final TaxCompanyService companyService;

    /**
     * 查询公司列表
     */
    @GetMapping
    public List<TaxCompany> list() {
        return companyService.list();
    }

    /**
     * 新增或更新公司
     */
    @PostMapping
    public TaxCompany upsert(@RequestBody UpsertCompanyCommand command) {
        return companyService.upsert(command);
    }

    /**
     * 删除公司（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        companyService.delete(id);
    }
}
