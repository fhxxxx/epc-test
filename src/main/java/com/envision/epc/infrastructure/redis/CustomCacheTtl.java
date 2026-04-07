package com.envision.epc.infrastructure.redis;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * @author jingjing.dong
 * @since 2021/12/11-22:17
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.cache")
public class CustomCacheTtl {
    private Map<String, Duration> custom;
}
