package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.module.taxledger.application.command.UpsertCompanyCommand;
import com.envision.epc.module.taxledger.domain.TaxCompany;
import com.envision.epc.module.taxledger.infrastructure.TaxCompanyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公司管理服务
 */
@Service
@RequiredArgsConstructor
public class TaxCompanyService {
    private final TaxCompanyMapper companyMapper;
    private final TaxPermissionService permissionService;

    /**
     * 查询公司列表
     */
    public List<TaxCompany> list() {
        return companyMapper.selectList(new LambdaQueryWrapper<TaxCompany>()
                .eq(TaxCompany::getIsDeleted, 0)
                .orderByAsc(TaxCompany::getCompanyCode));
    }

    /**
     * 新增或更新公司
     */
    public TaxCompany upsert(UpsertCompanyCommand command) {
        TaxCompany company;
        // 先按ID查，ID为空时按companyCode查
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
        // 同步可编辑字段
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

    /**
     * 删除公司（逻辑删除）
     */
    public void delete(Long id) {
        TaxCompany company = companyMapper.selectById(id);
        if (company == null) {
            return;
        }
        // 删除前先做公司权限校验
        permissionService.checkCompanyAccess(company.getCompanyCode());
        company.setIsDeleted(1);
        companyMapper.updateById(company);
    }
}
