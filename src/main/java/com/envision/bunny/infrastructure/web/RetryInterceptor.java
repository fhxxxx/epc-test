package com.envision.bunny.infrastructure.web;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * 重试拦截器
 *
 * @author jingjing.dong
 * @since 2022/11/25-21:34
 */
@Slf4j(topic = "rest template retry")
public class RetryInterceptor implements Interceptor {
    //最大重试次数
    private final int maxRetry;


    public RetryInterceptor(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    @SneakyThrows
    @NotNull
    @Override
    public Response intercept(Chain chain) {
        //假如设置为3次重试的话，则最大可能请求4次（默认1次+3次重试）
        int retryNum = 0;
        Request request = chain.request();
        Response response = chain.proceed(request);
        while (!response.isSuccessful() && retryNum < maxRetry) {
            TimeUnit.MILLISECONDS.sleep(1000);
            retryNum++;
            log.info("request {} retry for {} time", request.url(), retryNum);
            response.close();
            response = chain.proceed(request);
        }
        return response;
    }
}