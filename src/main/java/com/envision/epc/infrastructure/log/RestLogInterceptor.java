package com.envision.epc.infrastructure.log;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.envision.epc.infrastructure.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author jingjing.dong
 * @since 2021/4/8-17:52
 */
@Slf4j(topic="call out")
public class RestLogInterceptor implements ClientHttpRequestInterceptor {
    private static final String TRACE_ID_HEADER = "x-trace-id";
    private static final String TRACE_ID_MDC = "trace_id";

    @NotNull
    @Override
    public ClientHttpResponse intercept(@NotNull HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String traceId = MDC.get(TRACE_ID_MDC);
        if (StringUtils.isNotBlank(traceId)) {
            request.getHeaders().add(TRACE_ID_HEADER, traceId);
        }
        long startTime = System.currentTimeMillis();
        ClientHttpResponse response = execution.execute(request, body);
        long costTime = System.currentTimeMillis() - startTime;
        byte[] responseBodyBytes = StreamUtils.copyToByteArray(response.getBody());
        StringBuilder respBody = new StringBuilder();
        //当然图片、文件一类的就可以省
        final MediaType respContentType = response.getHeaders().getContentType();
        if (Objects.nonNull(respContentType)) {
            if (isReadable(respContentType.toString())) {
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(responseBodyBytes),
                        StandardCharsets.UTF_8))) {
                    String line = bufferedReader.readLine();
                    while (line != null) {
                        respBody.append(line);
                        line = bufferedReader.readLine();
                    }
                }
            }
        }
        final MediaType reqContentType = request.getHeaders().getContentType();
        //当然图片、文件一类的就可以省了
        if (Objects.nonNull(reqContentType)) {
            if (!isReadable(reqContentType.toString())) {
                body = new byte[]{};
            }
        }

        log.info(JsonUtils.toJsonStr(RestLog.builder().costTime(costTime).reqHeaders(request.getHeaders()).method(request.getMethod().toString()).
                respHeaders(response.getHeaders()).reqUrl(request.getURI().toString()).reqBody(new String(body, StandardCharsets.UTF_8)).respBody(respBody.toString()).respStatus(response.getStatusCode().value()).build()));
        return new BufferingClientHttpResponseWrapper(response, responseBodyBytes);
    }

    private boolean isReadable(String contentType) {
        if (contentType.startsWith("text")) {
            return true;
        }
        if (contentType.contains("json")) {
            return true;
        }
        if (contentType.contains("xml")) {
            return true;
        }
        return contentType.contains("html");
    }
}
