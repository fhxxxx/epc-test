package com.envision.epc.module.taxledger.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 税务台账模块配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "custom.taxledger")
public class TaxLedgerProperties {
    /**
     * 超级管理员工号列表（可选）
     */
    private List<String> superAdminUserCodes = new ArrayList<>();
}
