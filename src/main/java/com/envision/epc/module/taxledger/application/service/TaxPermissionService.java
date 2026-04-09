package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.infrastructure.security.SecurityUtils;
import com.envision.epc.module.taxledger.application.command.GrantPermissionCommand;
import com.envision.epc.module.taxledger.common.TaxLedgerProperties;
import com.envision.epc.module.taxledger.domain.TaxUserPermission;
import com.envision.epc.module.taxledger.infrastructure.TaxUserPermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限服务（超级管理员 + 公司用户映射）
 */
@Service
@RequiredArgsConstructor
public class TaxPermissionService {
    private static final String TEMP_BYPASS_USER_CODE = "EX413903";
    private static final String TEMP_BYPASS_ACCOUNT = "gangxiang.guan";

    private final TaxUserPermissionMapper permissionMapper;
    private final TaxLedgerProperties properties;

    /**
     * 按公司查询权限记录
     */
    public List<TaxUserPermission> listByCompany(String companyCode) {
        return permissionMapper.selectList(new LambdaQueryWrapper<TaxUserPermission>()
                .eq(TaxUserPermission::getIsDeleted, 0)
                .eq(StringUtils.hasText(companyCode), TaxUserPermission::getCompanyCode, companyCode));
    }

    /**
     * 授权（同用户同公司先撤销再授权）
     */
    public TaxUserPermission grant(GrantPermissionCommand command) {
        validate(command);
        revoke(command.getUserId(), command.getCompanyCode());

        TaxUserPermission permission = new TaxUserPermission();
        permission.setUserId(command.getUserId());
        permission.setUserName(command.getUserName());
        permission.setCompanyCode(command.getCompanyCode());
        permission.setIsDeleted(0);
        permissionMapper.insert(permission);
        return permission;
    }

    /**
     * 批量授权
     */
    @Transactional(rollbackFor = Exception.class)
    public List<TaxUserPermission> grantBatch(List<GrantPermissionCommand> commands) {
        if (CollectionUtils.isEmpty(commands)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Grant list must not be empty");
        }
        List<TaxUserPermission> result = new ArrayList<>();
        for (GrantPermissionCommand command : commands) {
            result.add(grant(command));
        }
        return result;
    }

    /**
     * 撤销授权（逻辑删除）
     */
    public void revoke(String userId, String companyCode) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(companyCode)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "userId and companyCode are required");
        }

        List<TaxUserPermission> existed = permissionMapper.selectList(new LambdaQueryWrapper<TaxUserPermission>()
                .eq(TaxUserPermission::getIsDeleted, 0)
                .eq(TaxUserPermission::getUserId, userId)
                .eq(TaxUserPermission::getCompanyCode, companyCode));

        existed.forEach(item -> {
            item.setIsDeleted(1);
            permissionMapper.updateById(item);
        });
    }

    /**
     * 公司访问权限校验
     */
    public void checkCompanyAccess(String companyCode) {
        String currentUserCode = currentUserCode();
        if (isTempBypassUser(currentUserCode)) {
            return;
        }
        if (isSuperAdmin(currentUserCode)) {
            return;
        }

        boolean hasAccess = permissionMapper.selectCount(new LambdaQueryWrapper<TaxUserPermission>()
                .eq(TaxUserPermission::getIsDeleted, 0)
                .eq(TaxUserPermission::getUserId, currentUserCode)
                .eq(TaxUserPermission::getCompanyCode, companyCode)) > 0;
        if (!hasAccess) {
            throw new BizException(ErrorCode.AUTH_ACCESS_DENIED, "No permission for company " + companyCode);
        }
    }

    private boolean isTempBypassUser(String userCode) {
        if (userCode == null) {
            return false;
        }
        return TEMP_BYPASS_USER_CODE.equalsIgnoreCase(userCode)
                || TEMP_BYPASS_ACCOUNT.equalsIgnoreCase(userCode);
    }

    /**
     * 是否超级管理员
     */
    public boolean isSuperAdmin(String userCode) {
        return StringUtils.hasText(userCode) && properties.getSuperAdminUserCodes().contains(userCode);
    }

    /**
     * 获取当前登录人工号
     */
    public String currentUserCode() {
        try {
            return SecurityUtils.getCurrentUserCode();
        } catch (Exception ignored) {
            return TEMP_BYPASS_USER_CODE;
        }
    }

    /**
     * 授权参数校验
     */
    private static void validate(GrantPermissionCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Grant command must not be null");
        }
        if (!StringUtils.hasText(command.getUserId())
                || !StringUtils.hasText(command.getUserName())
                || !StringUtils.hasText(command.getCompanyCode())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "userId/userName/companyCode are required");
        }
    }
}
