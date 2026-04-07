package com.envision.epc.facade.azure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wenjun.gu
 * @since 2025/8/21-17:46
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "custom.azure.layout")
public class LayoutConfig {
    private String submitEndpoint;
    private String queryEndpoint;
    private String key;
}
