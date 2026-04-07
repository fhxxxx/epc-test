package com.envision.epc.infrastructure.async;

import org.slf4j.MDC;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.task.TaskDecorator;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Locale;
import java.util.Map;

/**
 * @author wenjun.gu
 * @since 2021/12/24-13:04
 */
public class ContextCopyingDecorator implements TaskDecorator {

    /**
     * 启用新线程时, 将 request, security, mdc 属性复制进去
     */
    @Override
    public Runnable decorate(Runnable runnable) {
        try {
            RequestAttributes context = RequestContextHolder.currentRequestAttributes();
            Map<String,String> mdc = MDC.getCopyOfContextMap();
            Locale locale = LocaleContextHolder.getLocale();
            return () -> {
                try {
                    RequestContextHolder.setRequestAttributes(context);
                    MDC.setContextMap(mdc);
                    LocaleContextHolder.setLocale(locale);
                    runnable.run();
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                    MDC.clear();
                    LocaleContextHolder.resetLocaleContext();
                }
            };
        } catch (IllegalStateException e) {
            return runnable;
        }
    }
}
