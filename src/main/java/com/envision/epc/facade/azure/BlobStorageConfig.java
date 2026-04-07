package com.envision.epc.facade.azure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wenjun.gu
 * @since 2025/8/14-16:45
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "custom.azure.blob")
public class BlobStorageConfig {
    private String endpoint;
    private String accountName;
    private String accountKey;
    private String containerName;
}
