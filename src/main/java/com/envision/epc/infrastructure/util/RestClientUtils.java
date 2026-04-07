package com.envision.epc.infrastructure.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * 对RestTemplate进行一个简单的封装,主要针对一些不方便的使用
 *
 * @author jingjing.dong
 * @since 2021/4/9-15:52
 */
@Component
public class RestClientUtils {
    private static RestClient restClient;

    public RestClientUtils(@Autowired RestClient restClient) {
        RestClientUtils.restClient = restClient;
    }

    // ----------------------------------GET-------------------------------------------------------

    /**
     * 自定义请求头的GET请求调用方式
     *
     * @param url          请求URL
     * @param headers      请求头封装Map
     * @param responseType 返回对象类型
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T getWithHeaders(String url, Map<String, String> headers, Class<T> responseType) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAll(headers);
        return getWithHeaders(url, httpHeaders, responseType);
    }

    /**
     * 自定义请求头的GET请求调用方式
     *
     * @param url          请求URL
     * @param headers      请求头封装Map
     * @param responseType 返回对象类型
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T getWithHeaders(String url, Map<String, String> headers, Class<T> responseType,
                                       Object... uriVariables) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAll(headers);
        return getWithHeaders(url, httpHeaders, responseType, uriVariables);
    }

    /**
     * 自定义请求头的GET请求调用方式
     *
     * @param url          请求URL
     * @param headers      请求头封装Map
     * @param responseType 返回对象类型
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T getWithHeaders(String url, Map<String, String> headers, Class<T> responseType, Map<String, Object> uriVariables) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAll(headers);
        return getWithHeaders(url, httpHeaders, responseType, uriVariables);
    }

    /**
     * 自定义请求头的GET请求调用方式
     *
     * @param url          请求URL
     * @param headers      请求头封装对象
     * @param responseType 返回对象类型
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T getWithHeaders(String url, HttpHeaders headers, Class<T> responseType, Object... uriVariables) {
        return restClient.get().uri(url, uriVariables)
                .headers(h -> h.addAll(headers))
                .retrieve()
                .body(responseType);
    }

    /**
     * 自定义请求头的GET请求调用方式
     *
     * @param url          请求URL
     * @param headers      请求头封装对象
     * @param responseType 返回对象类型
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T getWithHeaders(String url, HttpHeaders headers, Class<T> responseType, Map<String, ?> uriVariables) {
        return restClient.get().uri(url, uriVariables)
                .headers(h -> h.addAll(headers))
                .retrieve()
                .body(responseType);
    }
    // ----------------------------------PUT-------------------------------------------------------

    /**
     * PUT请求调用方式
     *
     * @param url          请求URL
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，按顺序依次对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T putForObject(String url, Class<T> responseType, Object... uriVariables)
            throws RestClientException {
        return putForObject(url, HttpHeaders.EMPTY,null, responseType, uriVariables);
    }

    /**
     * PUT请求调用方式
     *
     * @param url          请求URL
     * @param requestBody  请求参数体
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，按顺序依次对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T putForObject(String url, Object requestBody, Class<T> responseType, Object... uriVariables)
            throws RestClientException {
        return putForObject(url, HttpHeaders.EMPTY, requestBody, responseType, uriVariables);
    }

    /**
     * PUT请求调用方式
     *
     * @param url          请求URL
     * @param requestBody  请求参数体
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，与Map中的key对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T putForObject(String url, Object requestBody, Class<T> responseType, Map<String, ?> uriVariables)
            throws RestClientException {
        return putForObject(url,HttpHeaders.EMPTY, requestBody, responseType, uriVariables);
    }

    /**
     * 带请求头的PUT请求调用方式
     *
     * @param url          请求URL
     * @param headers      请求头参数
     * @param requestBody  请求参数体
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，按顺序依次对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T putForObject(String url, Map<String, String> headers, Object requestBody, Class<T> responseType,
                                     Object... uriVariables) throws RestClientException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAll(headers);
        return putForObject(url,  httpHeaders, requestBody,responseType, uriVariables);
    }


    /**
     * 带请求头的PUT请求调用方式
     *
     * @param url          请求URL
     * @param headers      请求头参数
     * @param requestBody  请求参数体
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，与Map中的key对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T putForObject(String url, Map<String, String> headers, Object requestBody, Class<T> responseType,
                                     Map<String, ?> uriVariables) throws RestClientException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAll(headers);
        return putForObject(url,  httpHeaders, requestBody,responseType, uriVariables);
    }


    /**
     * 自定义请求头和请求体的PUT请求调用方式
     *
     * @param url          请求URL
     * @param requestBody  请求体对象
     * @param headers      请求头参数
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，按顺序依次对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T putForObject(String url, HttpHeaders headers, Object requestBody, Class<T> responseType,
                                     Object... uriVariables) throws RestClientException {
        return restClient.put()
                .uri(url, uriVariables)
                .headers(h -> h.addAll(headers))
                .body(requestBody)
                .retrieve()
                .body(responseType);
    }

    /**
     * 自定义请求头和请求体的PUT请求调用方式
     *
     * @param url          请求URL
     * @param requestBody  请求体对象
     * @param headers      请求头参数
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，与Map中的key对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T putForObject(String url, HttpHeaders headers, Object requestBody, Class<T> responseType,
                                     Map<String, ?> uriVariables) throws RestClientException {
        return restClient.put()
                .uri(url, uriVariables)
                .headers(h -> h.addAll(headers))
                .body(requestBody)
                .retrieve()
                .body(responseType);
    }

    // ----------------------------------DELETE-------------------------------------------------------

    /**
     * DELETE请求调用方式
     *
     * @param url          请求URL
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，按顺序依次对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T delete(String url, Class<T> responseType, Object... uriVariables)
            throws RestClientException {
        return delete(url, HttpHeaders.EMPTY,null, responseType, uriVariables);
    }

    /**
     * DELETE请求调用方式
     *
     * @param url          请求URL
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，与Map中的key对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T delete(String url, Class<T> responseType, Map<String, ?> uriVariables)
            throws RestClientException {
        return delete(url, HttpHeaders.EMPTY,null, responseType, uriVariables);
    }

    /**
     * DELETE请求调用方式
     *
     * @param url          请求URL
     * @param requestBody  请求参数体
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，按顺序依次对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T delete(String url, Object requestBody, Class<T> responseType, Object... uriVariables)
            throws RestClientException {
        return delete(url,HttpHeaders.EMPTY, requestBody, responseType, uriVariables);
    }

    /**
     * DELETE请求调用方式
     *
     * @param url          请求URL
     * @param requestBody  请求参数体
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，与Map中的key对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T delete(String url, Object requestBody, Class<T> responseType,
                               Map<String, ?> uriVariables) throws RestClientException {
        return delete(url, HttpHeaders.EMPTY, requestBody, responseType, uriVariables);
    }

    /**
     * 带请求头的DELETE请求调用方式
     *
     * @param url          请求URL
     * @param headers      请求头参数
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，按顺序依次对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T delete(String url, Map<String, String> headers, Class<T> responseType,
                               Object... uriVariables) throws RestClientException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAll(headers);
        return delete(url, httpHeaders, responseType, uriVariables);
    }

    /**
     * 带请求头的DELETE请求调用方式
     *
     * @param url          请求URL
     * @param headers      请求头参数
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，按顺序依次对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T delete(String url, HttpHeaders headers, Class<T> responseType, Object... uriVariables)
            throws RestClientException {
        return delete(url, headers, null, responseType, uriVariables);
    }

    /**
     * 带请求头的DELETE请求调用方式
     *
     * @param url          请求URL
     * @param headers      请求头参数
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，与Map中的key对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T delete(String url, Map<String, String> headers, Class<T> responseType,
                               Map<String, ?> uriVariables) throws RestClientException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAll(headers);
        return delete(url, httpHeaders, responseType, uriVariables);
    }

    /**
     * 带请求头的DELETE请求调用方式
     *
     * @param url          请求URL
     * @param headers      请求头参数
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，与Map中的key对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T delete(String url, HttpHeaders headers, Class<T> responseType,
                               Map<String, ?> uriVariables) throws RestClientException {
        return delete(url, headers, null, responseType, uriVariables);
    }

    /**
     * 带请求头的DELETE请求调用方式
     *
     * @param url          请求URL
     * @param headers      请求头参数
     * @param requestBody  请求参数体
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，按顺序依次对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T delete(String url, Map<String, String> headers, Object requestBody,
                               Class<T> responseType, Object... uriVariables) throws RestClientException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAll(headers);
        return delete(url, httpHeaders, requestBody, responseType, uriVariables);
    }


    /**
     * 带请求头的DELETE请求调用方式
     *
     * @param url          请求URL
     * @param headers      请求头参数
     * @param requestBody  请求参数体
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，与Map中的key对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T delete(String url, Map<String, String> headers, Object requestBody,
                               Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAll(headers);
        return delete(url, httpHeaders, requestBody, responseType, uriVariables);
    }

    /**
     * 自定义请求头和请求体的DELETE请求调用方式
     *
     * @param url           请求URL
     * @param requestBody  请求体对象
     * @param headers      请求头参数
     * @param responseType  返回对象类型
     * @param uriVariables  URL中的变量，按顺序依次对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T delete(String url, HttpHeaders headers, Object requestBody, Class<T> responseType,
                               Object... uriVariables) throws RestClientException {
        return restClient.method(HttpMethod.DELETE)
                .uri( url, uriVariables)
                .body(requestBody)
                .headers(h->h.addAll( headers))
                .retrieve()
                .body(responseType);    }

    /**
     * 自定义请求头和请求体的DELETE请求调用方式
     *
     * @param url           请求URL
     * @param requestBody  请求体对象
     * @param headers      请求头参数
     * @param responseType  返回对象类型
     * @param uriVariables  URL中的变量，与Map中的key对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T delete(String url, HttpHeaders headers, Object requestBody, Class<T> responseType,
                               Map<String, ?> uriVariables) throws RestClientException {
        return restClient.method(HttpMethod.DELETE)
                .uri( url,uriVariables)
                .body(requestBody)
                .headers(h->h.addAll( headers))
                .retrieve()
                .body(responseType);
    }
}
