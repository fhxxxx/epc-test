package com.envision.epc.facade.dify;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wenjun.gu
 * @since 2025/8/30-15:09
 */
@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "custom.dify")
public class DifyConfig {
    private String endpoint;
    private String extractKey;
}
