package com.envision.epc.infrastructure.notice.sms;

import com.envision.epc.facade.platform.PlatformRemote;
import com.envision.epc.infrastructure.notice.whitelist.Whitelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;

import jakarta.validation.Valid;

/**
 * 异步进行短信发送
 *
 * @author jingjing.dong
 * @since 2021/4/14-11:22
 */
@Component
@Validated
public class SmsUtils {
    @Autowired
    PlatformRemote platformRemote;
    @Autowired
    RestClient restClient;
    @Value("${custom.sms.endpoint}")
    String endpoint;

    @Async
    @Whitelist
    public void send(@Valid SmsBody body) {
        String token = platformRemote.getToken(Constants.TARGET_SERVICE);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(Constants.TOKEN_HEADER_NAME, token);
        body.addPrefix();
        HttpEntity<SmsBody> entity = new HttpEntity<>(body, headers);
        restClient.post()
                .uri(endpoint)
                .body(body)
                .headers(h->h.addAll(headers))
                .retrieve()
                .body(String.class);
    }
}
