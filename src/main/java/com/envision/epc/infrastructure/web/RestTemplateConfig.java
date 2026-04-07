package com.envision.epc.infrastructure.web;

import com.envision.epc.infrastructure.log.RestLogInterceptor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author jingjing.dong
 * @since 2021/4/8-16:01
 */
@Configuration
@Slf4j
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(closeableHttpClient());
//        OkHttp3ClientHttpRequestFactory factory = new OkHttp3ClientHttpRequestFactory(okHttpClient());
        BufferingClientHttpRequestFactory bufferingClientHttpRequestFactory = new BufferingClientHttpRequestFactory(factory);
        RestTemplate restTemplate = new RestTemplate(bufferingClientHttpRequestFactory);
        restTemplate.setInterceptors(List.of(new RestLogInterceptor()));
        //StringHttpMessageConverter是第二个，所以index是1，默认使用ISO-8859-1编码，此处修改为UTF-8
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }

    @Bean
    public RestClient restClient() {
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(closeableHttpClient());
        return RestClient.builder()
                .requestFactory(factory)
                .requestInterceptor(new RestLogInterceptor())
                .messageConverters(converters -> {
                    converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
                })
                .build();
    }

    public CloseableHttpClient closeableHttpClient(){
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setMaxConnTotal(200)
                        .setMaxConnPerRoute(50)
                        .setConnectionConfigResolver(route ->
                                ConnectionConfig.custom()
                                        .setConnectTimeout(Timeout.ofSeconds(30))
                                        .build()
                        )
                        .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMinutes(30))
                .build();

        HttpRequestRetryStrategy retryStrategy =
                new DefaultHttpRequestRetryStrategy(
                        3,
                        TimeValue.ofSeconds(1L)
                );
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setRetryStrategy(retryStrategy)
                .disableAutomaticRetries()
                .build();
    }

    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                //对一些Https的请求，有的需要增加此配置，比如绩效目标系统的接口
                //.sslSocketFactory(sslSocketFactory(), x509TrustManager())
                .retryOnConnectionFailure(false)
                .connectionPool(pool())
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.MINUTES)
                .writeTimeout(30,TimeUnit.SECONDS)
                .addInterceptor(new RetryInterceptor(3))
                .build();
    }

    /**
     * Create a new connection pool with tuning parameters appropriate for a single-user application.
     * The tuning parameters in this pool are subject to change in future OkHttp releases. Currently
     */
    public ConnectionPool pool() {
        return new ConnectionPool(20, 5, TimeUnit.MINUTES);
    }
}
