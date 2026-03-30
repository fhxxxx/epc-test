package com.envision.bunny.infrastructure.notice.wecom;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author jingjing.dong
 * @since 2021/4/27-16:19
 */
@ConfigurationProperties(prefix = "custom.wecom")
@Getter
@Setter
class WecomProperties {
  private long agentId;
  private String clientSecret;
  private String msgEndpoint;
  private String approvalEndpoint;
  private String recallEndpoint;
}
