package com.envision.epc.infrastructure.security.wecom;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author jingjing.dong
 * @since 2021/4/1-11:06
 */
@ConfigurationProperties(prefix = "custom.login.wecom")
@Setter
@Getter
public class WecomProperties {
    private String clientSecret;
    private String userinfoEndpoint;
    private String redirectUri;
    private String authorizationEndpoint;
    private String appId;
    private String agentId;
    private String scope;

}
