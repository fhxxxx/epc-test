package com.envision.bunny.infrastructure.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 对RestTemplate进行一个简单的封装,主要针对一些不方便的使用
 * @author jingjing.dong
 * @since 2021/4/9-15:52
 */
@Component
public class RestUtils {
    private static RestTemplate restTemplate;
    public RestUtils(@Autowired RestTemplate restTemplate) {
        RestUtils.restTemplate = restTemplate;
    }

    // ----------------------------------GET-------------------------------------------------------
    /**
     * 自定义请求头的GET请求调用方式
     *
     * @param url           请求URL
     * @param headers 请求头封装Map
     * @param responseType  返回对象类型
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T getWithHeaders(String url, Map<String, String> headers, Class<T> responseType){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAll(headers);
        return getWithHeaders(url,httpHeaders,responseType);
    }
    /**
     * 自定义请求头的GET请求调用方式
     *
     * @param url           请求URL
     * @param headers 请求头封装Map
     * @param responseType  返回对象类型
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T getWithHeaders(String url, Map<String, String> headers, Class<T> responseType,
                                       Object... uriVariables){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAll(headers);
        return getWithHeaders(url,httpHeaders,responseType,uriVariables);
    }
    /**
     * 自定义请求头的GET请求调用方式
     *
     * @param url           请求URL
     * @param headers 请求头封装Map
     * @param responseType  返回对象类型
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T getWithHeaders(String url, Map<String, String> headers, Class<T> responseType, Map<String, Object> uriVariables){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAll(headers);
        return getWithHeaders(url,httpHeaders,responseType, uriVariables);
    }
    /**
     * 自定义请求头的GET请求调用方式
     *
     * @param url           请求URL
     * @param headers 请求头封装对象
     * @param responseType  返回对象类型
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T getWithHeaders(String url, HttpHeaders headers, Class<T> responseType, Object... uriVariables){
        return restTemplate.exchange(url,HttpMethod.GET,new HttpEntity<>(headers),responseType,uriVariables).getBody();
    }
    /**
     * 自定义请求头的GET请求调用方式
     *
     * @param url           请求URL
     * @param headers 请求头封装对象
     * @param responseType  返回对象类型
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T getWithHeaders(String url, HttpHeaders headers, Class<T> responseType, Map<String, ?> uriVariables){
        return restTemplate.exchange(url,HttpMethod.GET,new HttpEntity<>(headers),responseType,uriVariables).getBody();
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
        return putForObject(url, HttpEntity.EMPTY, responseType, uriVariables);
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
        HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody);
        return putForObject(url, requestEntity, responseType, uriVariables);
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
        HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody);
        return putForObject(url, requestEntity, responseType, uriVariables);
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
        return putForObject(url, httpHeaders, requestBody, responseType, uriVariables);
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
    public static <T> T putForObject(String url, HttpHeaders headers, Object requestBody, Class<T> responseType,
                                              Object... uriVariables) throws RestClientException {
        HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);
        return putForObject(url, requestEntity, responseType, uriVariables);
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
        return putForObject(url, httpHeaders, requestBody, responseType, uriVariables);
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
    public static <T> T putForObject(String url, HttpHeaders headers, Object requestBody, Class<T> responseType,
                                              Map<String, ?> uriVariables) throws RestClientException {
        HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);
        return putForObject(url, requestEntity, responseType, uriVariables);
    }

    /**
     * 自定义请求头和请求体的PUT请求调用方式
     *
     * @param url           请求URL
     * @param requestEntity 请求头和请求体封装对象
     * @param responseType  返回对象类型
     * @param uriVariables  URL中的变量，按顺序依次对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T putForObject(String url, HttpEntity<?> requestEntity, Class<T> responseType,
                                              Object... uriVariables) throws RestClientException {
        return restTemplate.exchange(url, HttpMethod.PUT, requestEntity, responseType, uriVariables).getBody();
    }

    /**
     * 自定义请求头和请求体的PUT请求调用方式
     *
     * @param url           请求URL
     * @param requestEntity 请求头和请求体封装对象
     * @param responseType  返回对象类型
     * @param uriVariables  URL中的变量，与Map中的key对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T putForObject(String url, HttpEntity<?> requestEntity, Class<T> responseType,
                                              Map<String, ?> uriVariables) throws RestClientException {
        return restTemplate.exchange(url, HttpMethod.PUT, requestEntity, responseType, uriVariables).getBody();
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
        return delete(url, HttpEntity.EMPTY, responseType, uriVariables);
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
        return delete(url, HttpEntity.EMPTY, responseType, uriVariables);
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
        HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody);
        return delete(url, requestEntity, responseType, uriVariables);
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
        HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody);
        return delete(url, requestEntity, responseType, uriVariables);
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
        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
        return delete(url, requestEntity, responseType, uriVariables);
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
        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
        return delete(url, requestEntity, responseType, uriVariables);
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
     * @param uriVariables URL中的变量，按顺序依次对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T delete(String url, HttpHeaders headers, Object requestBody, Class<T> responseType,
                                        Object... uriVariables) throws RestClientException {
        HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);
        return delete(url, requestEntity, responseType, uriVariables);
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
     * 带请求头的DELETE请求调用方式
     *
     * @param url          请求URL
     * @param headers      请求头参数
     * @param requestBody  请求参数体
     * @param responseType 返回对象类型
     * @param uriVariables URL中的变量，与Map中的key对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T delete(String url, HttpHeaders headers, Object requestBody, Class<T> responseType,
                                        Map<String, ?> uriVariables) throws RestClientException {
        HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);
        return delete(url, requestEntity, responseType, uriVariables);
    }

    /**
     * 自定义请求头和请求体的DELETE请求调用方式
     *
     * @param url           请求URL
     * @param requestEntity 请求头和请求体封装对象
     * @param responseType  返回对象类型
     * @param uriVariables  URL中的变量，按顺序依次对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T delete(String url, HttpEntity<?> requestEntity, Class<T> responseType,
                                        Object... uriVariables) throws RestClientException {
        return restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, responseType, uriVariables).getBody();
    }

    /**
     * 自定义请求头和请求体的DELETE请求调用方式
     *
     * @param url           请求URL
     * @param requestEntity 请求头和请求体封装对象
     * @param responseType  返回对象类型
     * @param uriVariables  URL中的变量，与Map中的key对应
     * @return ResponseEntity 响应对象封装类
     */
    public static <T> T delete(String url, HttpEntity<?> requestEntity, Class<T> responseType,
                                        Map<String, ?> uriVariables) throws RestClientException {
        return restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, responseType, uriVariables).getBody();
    }
}
