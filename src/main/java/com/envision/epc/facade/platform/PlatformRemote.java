package com.envision.epc.facade.platform;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.infrastructure.util.ApplicationContextUtils;
import com.envision.epc.infrastructure.util.RestClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author jingjing.dong
 * @since 2021/4/8-16:52
 */
@Component
@Slf4j(topic = "env platform utils")
@EnableConfigurationProperties(TokenReqBody.class)
public class PlatformRemote {
    @Autowired
    RestClient restClient;

    /**
     * 方便的从数据湖获取数据，因为数据湖的返回数据结构是固定的
     *
     * @param tgtSvc 目标的服务标记，以获取Token
     * @param reqUrl 具体请求的Url,调用请使用UriComponentsBuilder构建完整的URL
     * @param <T>    一个Function，返回的对象类型
     * @return 返回的对象的集合
     */
    public <T> List<T> fetchFromDataLake(String tgtSvc, String reqUrl, Function<JsonNode, T> func) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(Constants.TOKEN_HEADER_NAME, getToken(tgtSvc));
        try {
            return pagingQuery(reqUrl, headers, func);
        } catch (UnsupportedEncodingException e) {
            throw new BizException(ErrorCode.FETCH_FROM_DATALAKE_FAILED);
        }
    }

    /**
     * 从微服务网关获取Token,由于微服务网关有Token缓存机制，因此无需自行缓存Token;
     * 由于挂接的服务，返回的结构体各异，因此需要获取到Token后自行解析
     *
     * @param tgtSvc 目标的服务标记
     * @return 请求的Token, 访问微服务时需加到Header -> X-ENP-AUTH
     */
    public String getToken(String tgtSvc) {
        TokenReqBody reqBody = ApplicationContextUtils.getBean(TokenReqBody.class);
        reqBody.setTgtSvc(tgtSvc);
        JsonNode resp = restClient.post().uri(reqBody.domain + Constants.TOKEN_REQ_PATH)
                .body(reqBody)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);
        String token = Objects.requireNonNull(resp).get(Constants.TOKEN_FIELD_NAME).asText();
        log.info("get platform token:[{}] for service [{}]", token, tgtSvc);
        return token;
    }

    /**
     * 因为平台的Header是统一的，因此加一个方法，内部封装了获取token以及设置header
     * @param tgtSvc 目标的服务标记
     * @return 请求的Token, 访问微服务时需加到Header -> X-ENP-AUTH
     */
    public HttpHeaders getPlatformReqHeader(String tgtSvc) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(Constants.TOKEN_HEADER_NAME, getToken(tgtSvc));
        return headers;
    }

    private <T> List<T> pagingQuery(String url, HttpHeaders headers, Function<JsonNode, T> func) throws UnsupportedEncodingException {
        boolean hasNextLink;
        PltResp response;
        String nextLink;
        List<T> ans = Lists.newArrayList();
        do {
            response = RestClientUtils.getWithHeaders(URLDecoder.decode(url, "UTF-8"), headers, PltResp.class);
            ans.addAll(convert(response, func));
            if (Objects.isNull(response.getLinks())) {
                hasNextLink = false;
                continue;
            }
            nextLink = response.getLinks().getNext();
            if (Objects.isNull(nextLink)) {
                hasNextLink = false;
            } else {
                hasNextLink = true;
                url = nextLink;
            }
        } while (hasNextLink);
        return ans;
    }

    private static <T> List<T> convert(PltResp result, Function<JsonNode, T> func) {
        List<JsonNode> respData = result.getData();
        if (respData.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> results = Lists.newArrayList();
        for (JsonNode node : respData) {
            JsonNode data = node.get("attributes");
            results.add(func.apply(data));
        }
        return results;
    }
}
