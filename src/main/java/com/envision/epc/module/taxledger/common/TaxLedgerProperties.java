package com.envision.epc.module.taxledger.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "custom.taxledger")
public class TaxLedgerProperties {
    /**
     * Optional bootstrapped super admins. If empty, super admin is resolved only by DB permission.
     */
    private List<String> superAdminUserCodes = new ArrayList<>();
}
