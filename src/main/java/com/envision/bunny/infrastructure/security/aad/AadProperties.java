package com.envision.bunny.infrastructure.security.aad;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author jingjing.dong
 * @since 2021/4/1-11:06
 */
@ConfigurationProperties(prefix = "custom.login.aad")
@Setter
@Getter
public class AadProperties {
    private String clientId;
    private String clientSecret;
    private String tenantId;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userinfoEndpoint;
    private String redirectUri;
    private String scope;
}
