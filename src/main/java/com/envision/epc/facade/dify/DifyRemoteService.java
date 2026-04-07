package com.envision.epc.facade.dify;

import com.envision.epc.infrastructure.log.AvoidLog;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wenjun.gu
 * @since 2025/8/30-15:12
 */
@Service
@EnableConfigurationProperties(DifyConfig.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DifyRemoteService {
    private final DifyConfig difyConfig;
    private final RestTemplate restTemplate;

    @AvoidLog
    @Retryable
    public String extract(String instruction, String structure, String content) {
        String url = difyConfig.getEndpoint() + "/v1/chat-messages";
        String apiKey = difyConfig.getExtractKey();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", Map.of(
                "instruction", instruction,
                "structure", structure
        ));
        requestBody.put("auto_generate_name", false);
        requestBody.put("query", content);
        requestBody.put("response_mode", "blocking");
        requestBody.put("user", "it-chatenv-extractcenter");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, requestEntity, JsonNode.class);

        JsonNode body = response.getBody();
        if (body == null || !body.has("answer")) {
            throw new BizException(ErrorCode.REMOTE_ERROR, "Dify extract return empty");
        }

        return body.get("answer").asText();
    }
}
