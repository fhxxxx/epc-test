package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.infrastructure.security.SecurityUtils;
import com.envision.epc.module.taxledger.application.command.GrantPermissionCommand;
import com.envision.epc.module.taxledger.common.TaxLedgerProperties;
import com.envision.epc.module.taxledger.domain.PermissionLevelEnum;
import com.envision.epc.module.taxledger.domain.TaxUserPermission;
import com.envision.epc.module.taxledger.infrastructure.TaxUserPermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TaxPermissionService {
    private final TaxUserPermissionMapper permissionMapper;
    private final TaxLedgerProperties properties;

    public List<TaxUserPermission> listByCompany(String companyCode) {
        return permissionMapper.selectList(new LambdaQueryWrapper<TaxUserPermission>()
                .eq(TaxUserPermission::getIsDeleted, 0)
                .eq(companyCode != null, TaxUserPermission::getCompanyCode, companyCode));
    }

    public TaxUserPermission grant(GrantPermissionCommand command) {
        validate(command);
        revoke(command.getEmployeeId(), command.getCompanyCode());
        TaxUserPermission permission = new TaxUserPermission();
        permission.setUserId(command.getUserId());
        permission.setUserName(command.getUserName());
        permission.setEmployeeId(command.getEmployeeId());
        permission.setPermissionLevel(command.getPermissionLevel());
        permission.setCompanyCode(command.getCompanyCode());
        permission.setGrantedBy(currentUserCode());
        permission.setIsDeleted(0);
        permissionMapper.insert(permission);
        return permission;
    }

    public void revoke(String employeeId, String companyCode) {
        List<TaxUserPermission> existed = permissionMapper.selectList(new LambdaQueryWrapper<TaxUserPermission>()
                .eq(TaxUserPermission::getIsDeleted, 0)
                .eq(TaxUserPermission::getEmployeeId, employeeId)
                .eq(Objects.nonNull(companyCode), TaxUserPermission::getCompanyCode, companyCode));
        existed.forEach(item -> {
            item.setIsDeleted(1);
            permissionMapper.updateById(item);
        });
    }

    public void checkCompanyAccess(String companyCode) {
        if (isSuperAdmin(currentUserCode())) {
            return;
        }
        boolean hasAccess = permissionMapper.selectCount(new LambdaQueryWrapper<TaxUserPermission>()
                .eq(TaxUserPermission::getIsDeleted, 0)
                .eq(TaxUserPermission::getEmployeeId, currentUserCode())
                .eq(TaxUserPermission::getCompanyCode, companyCode)) > 0;
        if (!hasAccess) {
            throw new BizException(ErrorCode.AUTH_ACCESS_DENIED, "No permission for company " + companyCode);
        }
    }

    public boolean isSuperAdmin(String userCode) {
        if (userCode == null) {
            return false;
        }
        if (properties.getSuperAdminUserCodes().contains(userCode)) {
            return true;
        }
        return permissionMapper.selectCount(new LambdaQueryWrapper<TaxUserPermission>()
                .eq(TaxUserPermission::getIsDeleted, 0)
                .eq(TaxUserPermission::getEmployeeId, userCode)
                .eq(TaxUserPermission::getPermissionLevel, PermissionLevelEnum.SUPER_ADMIN)) > 0;
    }

    public String currentUserCode() {
        try {
            return SecurityUtils.getCurrentUserCode();
        } catch (Exception ignored) {
            return "system";
        }
    }

    private static void validate(GrantPermissionCommand command) {
        if (command.getPermissionLevel() == PermissionLevelEnum.SUPER_ADMIN && command.getCompanyCode() != null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "SUPER_ADMIN must not bind company code");
        }
        if (command.getPermissionLevel() != PermissionLevelEnum.SUPER_ADMIN && command.getCompanyCode() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Company level permission requires company code");
        }
    }
}
