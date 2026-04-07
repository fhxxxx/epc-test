package com.envision.epc.facade.ip;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author chaoyue.zhao1
 * @since 2025/12/19-14:00
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "custom.ip")
public class IpConfig {
    private String host;
    private String path;
}
