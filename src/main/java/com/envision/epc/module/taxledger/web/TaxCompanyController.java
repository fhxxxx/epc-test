package com.envision.epc.module.taxledger.web;

import com.envision.epc.module.taxledger.application.command.UpsertCompanyCommand;
import com.envision.epc.module.taxledger.application.service.TaxCompanyService;
import com.envision.epc.module.taxledger.domain.TaxCompany;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tax-ledger/companies")
public class TaxCompanyController {
    private final TaxCompanyService companyService;

    @GetMapping
    public List<TaxCompany> list() {
        return companyService.list();
    }

    @PostMapping
    public TaxCompany upsert(@RequestBody UpsertCompanyCommand command) {
        return companyService.upsert(command);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        companyService.delete(id);
    }
}
