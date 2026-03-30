package com.envision.bunny.facade.platform;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;

/**
 * 平台那边已经进行了Token缓存 直接获取即可
 *
 * @return Plt Token
 */
/*    public String getPlatformToken(String svcPrn) {
    String appId = env.getProperty("platform.token.app-id");
    String secret = env.getProperty("platform.token.secret");
    String grantType = env.getProperty("platform.token.grant-type");
    String userId = env.getProperty("platform.token.user-id");
    String url = env.getProperty("platform.token.url");
    String headerName = env.getProperty("platform.token.header");
    PlatformTokenReqBody body = PlatformTokenReqBody.builder().appId(appId)
            .appSecret(secret).grantType(grantType).tgtSvc(svcPrn).userId(userId).build();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<PlatformTokenReqBody> entity = new HttpEntity<>(body, headers);
    JsonNode resp = restTemplate.postForObject(url, entity, JsonNode.class);
    String token = resp.get(headerName).asText();
    // log.info("get platform token:[{}] for service [{}]", token, svcPrn);
    return token;
}*/

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ConfigurationProperties(prefix = "custom.platform.token")
@Setter
@Getter
@Scope("prototype")
@JsonIgnoreProperties
class TokenReqBody {
    String grantType;
    String appId;
    String appSecret;
    String tgtSvc;
    String userId;
    String domain;
}
