package com.envision.bunny.infrastructure.async;

import com.envision.bunny.infrastructure.response.BizException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步配置包括异步线程池和异步方法异常捕获
 *
 * @author jingjing.dong
 * @since 2021/3/23-14:26
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {
    /**
     * 也可以使用spring.task.pool.xxx的配置进行配置
     */

    @Bean
    public TaskExecutor taskExecutor() {
        ExecutorService virtualExecutor =
                Executors.newThreadPerTaskExecutor(
                        Thread.ofVirtual()
                                .name("async-virtual-", 1)
                                .factory()
                );

        TaskExecutorAdapter adapter = new TaskExecutorAdapter(virtualExecutor);
        adapter.setTaskDecorator(new ContextCopyingDecorator());
        return adapter;
    }


    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            if (ex instanceof BizException) {
                throw (BizException) ex;
            }
            log.error("异步方法异常[{}],参数[{}]！", method.getName(), Arrays.toString(params), ex);
        };
    }
}
