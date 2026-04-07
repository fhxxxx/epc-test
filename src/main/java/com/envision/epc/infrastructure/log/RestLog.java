package com.envision.epc.infrastructure.log;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpHeaders;

/**
 * @author jingjing.dong
 * @since 2021/4/9-11:41
 */
@Data
@Builder
class RestLog {
    private String reqUrl;
    private String method;
    private HttpHeaders reqHeaders;
    private HttpHeaders respHeaders;
    private String reqBody;
    private String respBody;
    private long costTime;
    private int respStatus;
}
