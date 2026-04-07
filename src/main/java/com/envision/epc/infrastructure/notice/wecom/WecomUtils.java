package com.envision.epc.infrastructure.notice.wecom;

import com.envision.epc.facade.platform.PlatformRemote;
import com.envision.epc.infrastructure.notice.wecom.approval.ApprovalCreatedBody;
import com.envision.epc.infrastructure.notice.wecom.approval.ApprvalUpdatedBody;
import com.envision.epc.infrastructure.notice.wecom.msg.*;
import com.envision.epc.infrastructure.notice.whitelist.Whitelist;
import com.envision.epc.infrastructure.util.RestClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * @author jingjing.dong
 * @since 2021/4/27-16:02
 */
@Component
@EnableConfigurationProperties(WecomProperties.class)
public class WecomUtils {
    @Autowired
    WecomProperties properties;
    @Autowired
    RestClient restClient;
    @Autowired
    PlatformRemote platformRemote;

    @Whitelist
    public void sendTextMsg(TextMsgBody body) {
        TextEnrichedBody enrichedBody = new TextEnrichedBody(properties.getAgentId(),properties.getClientSecret(),body);
        restClient.post()
                .uri(properties.getMsgEndpoint())
                .body(enrichedBody)
                .retrieve()
                .body(JsonNode.class);
    }

    @Whitelist
    public void sendTextcardMsg(TextcardMsgBody body) {
        TextcardEnrichedBody enrichedBody = new TextcardEnrichedBody(properties.getAgentId(), properties.getClientSecret(), body);
        restClient.post()
                .uri(properties.getMsgEndpoint())
                .body(enrichedBody)
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode recallMsg(String msgid) {
        final RecallBody recallBody = RecallBody.builder().msgid(msgid).agentid(properties.getAgentId()).secrect(properties.getClientSecret()).build();
        return restClient.post()
                .uri(properties.getMsgEndpoint())
                .body(recallBody)
                .retrieve()
                .body(JsonNode.class);
    }

    public void createApproval(ApprovalCreatedBody body){
        String token = platformRemote.getToken(Constants.TARGET_SERVICE);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(Constants.TOKEN_HEADER_NAME, token);
        restClient.post()
                .uri(properties.getMsgEndpoint())
                .body(body)
                .headers(h->h.addAll( headers))
                .retrieve()
                .body(JsonNode.class);

    }
    public void syncApproval(ApprvalUpdatedBody body){
        String token = platformRemote.getToken(Constants.TARGET_SERVICE);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(Constants.TOKEN_HEADER_NAME, token);
        HttpEntity<ApprvalUpdatedBody> entity = new HttpEntity<>(body, headers);
        RestClientUtils.putForObject(properties.getApprovalEndpoint(), entity, String.class);
    }
}
