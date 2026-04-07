package com.envision.epc.module.permission.application;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/22-13:39
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "custom.permission")
public class PermissionConfig {
    private List<String> admin;
    private List<String> divisions;
}
