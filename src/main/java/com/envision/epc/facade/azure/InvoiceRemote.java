package com.envision.epc.facade.azure;


import com.envision.epc.infrastructure.log.AvoidLog;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * @author wenjun.gu
 * @since 2025/8/21-17:47
 */
@Service
@EnableConfigurationProperties(InvoiceConfig.class)
public class InvoiceRemote {
    private final InvoiceConfig config;
    private final RestTemplate restTemplate;

    @Autowired
    public InvoiceRemote(InvoiceConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    @AvoidLog
    @Retryable
    public String runInvoice(String fileUrl, Integer startPage, Integer endPage) {
        String endpoint = String.format(config.getSubmitEndpoint(), String.format("%s-%s", startPage, endPage));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Ocp-Apim-Subscription-Key", config.getKey());
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("urlSource", fileUrl), headers);

        ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getHeaders().getFirst("apim-request-id");
        } else {
            throw new BizException(ErrorCode.BAD_REQUEST, "Run layout failed");
        }
    }

    @AvoidLog
    @Retryable
    public JsonNode getResult(String requestId) {
        String endpoint = String.format(config.getQueryEndpoint(), requestId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Ocp-Apim-Subscription-Key", config.getKey());
        ResponseEntity<JsonNode> response = restTemplate.exchange(endpoint, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new BizException(ErrorCode.BAD_REQUEST, "Get layout result failed");
        }
    }


}
