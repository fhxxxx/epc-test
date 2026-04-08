package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.module.taxledger.application.command.UpsertCompanyCommand;
import com.envision.epc.module.taxledger.domain.TaxCompany;
import com.envision.epc.module.taxledger.infrastructure.TaxCompanyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxCompanyService {
    private final TaxCompanyMapper companyMapper;
    private final TaxPermissionService permissionService;

    public List<TaxCompany> list() {
        return companyMapper.selectList(new LambdaQueryWrapper<TaxCompany>()
                .eq(TaxCompany::getIsDeleted, 0)
                .orderByAsc(TaxCompany::getCompanyCode));
    }

    public TaxCompany upsert(UpsertCompanyCommand command) {
        TaxCompany company;
        if (command.getId() != null) {
            company = companyMapper.selectById(command.getId());
        } else {
            company = companyMapper.selectOne(new LambdaQueryWrapper<TaxCompany>()
                    .eq(TaxCompany::getIsDeleted, 0)
                    .eq(TaxCompany::getCompanyCode, command.getCompanyCode()));
        }
        if (company == null) {
            company = new TaxCompany();
            company.setCompanyCode(command.getCompanyCode());
            company.setIsDeleted(0);
        }
        company.setCompanyName(command.getCompanyName());
        company.setFinanceBpAd(command.getFinanceBpAd());
        company.setFinanceBpName(command.getFinanceBpName());
        company.setFinanceBpEmail(command.getFinanceBpEmail());
        company.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        if (company.getId() == null) {
            companyMapper.insert(company);
        } else {
            companyMapper.updateById(company);
        }
        return company;
    }

    public void delete(Long id) {
        TaxCompany company = companyMapper.selectById(id);
        if (company == null) {
            return;
        }
        permissionService.checkCompanyAccess(company.getCompanyCode());
        company.setIsDeleted(1);
        companyMapper.updateById(company);
    }
}
