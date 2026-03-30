package com.envision.bunny.demo.integration.es.application;

import cn.hutool.core.text.CharSequenceUtil;
import com.envision.bunny.demo.integration.es.domain.Faq;
import com.envision.bunny.infrastructure.util.RestClientUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jingjing.dong
 * @since 2021/6/20-10:03
 */
@Service
public class ElasticSearchRemote {
    private static final String AUTHORIZATION = "Authorization";
    @Autowired
    RestClient restClient;
    @Value("https://vpc-social-recruit-prd-kzetemnja4uku3daceywyw6nnq.cn-north-1.es.amazonaws.com.cn/chatbot_v1/_doc/{id}")
    String endpoint;

    @Value("https://vpc-social-recruit-prd-kzetemnja4uku3daceywyw6nnq.cn-north-1.es.amazonaws.com.cn/chatbot_v1/_search?q=question:{query}&size=1")
    String maxScoreEndpoint;

    @Value("https://vpc-social-recruit-prd-kzetemnja4uku3daceywyw6nnq.cn-north-1.es.amazonaws.com.cn/chatbot_v1/_search")
    String searchEndpoint;
    final public static String ES_TOKEN = "Basic cmVjcnVpdGFkbWluOiNcVTtkYnQsRS92ZUlWOG0=";

    @Async
    public void putDoc(Faq doc) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, ES_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        RestClientUtils.putForObject(endpoint, new HttpEntity<>(doc, headers), String.class, doc.getId());
    }

    public double getMaxScore(String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, ES_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        final EsResponce withHeaders = RestClientUtils.getWithHeaders(maxScoreEndpoint, headers, EsResponce.class, query);
        return withHeaders.getHits().getMax_score();
    }

    public List<Faq> search(String query, double maxScore) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, ES_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String queryBody = "{\n" +
                "  \"query\": {\n" +
                "    \"match\": {\n" +
                "      \"question\": \"{}\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"min_score\":{}\n" +
                "}";
        final EsResponce withHeaders = restClient.post()
                .uri(searchEndpoint)
                .body(CharSequenceUtil.format(queryBody, query, maxScore))
                .headers(h -> h.addAll(headers))
                .retrieve()
                .body(EsResponce.class);
        return withHeaders.getHits().getHits().stream().map(hit -> hit.get_source()).collect(Collectors.toList());
    }


}
