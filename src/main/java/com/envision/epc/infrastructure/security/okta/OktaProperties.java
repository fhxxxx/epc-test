package com.envision.epc.infrastructure.security.okta;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author jingjing.dong
 * @since 2021/4/1-11:06
 */
@ConfigurationProperties(prefix = "custom.login.okta")
@Setter
@Getter
public class OktaProperties {
    private String clientId;
    private String clientSecret;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userinfoEndpoint;
    private String redirectUri;
    private String scope;
}
