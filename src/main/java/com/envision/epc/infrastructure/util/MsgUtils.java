package com.envision.epc.infrastructure.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * @author jingjing.dong
 * @since 2021/3/21-18:33
 */
@Component
public class MsgUtils {
    private static MessageSource messageSource;

    public MsgUtils(@Autowired MessageSource messageSource) {
        MsgUtils.messageSource = messageSource;
    }

//    public static String getMessage(String code) {
//        return getMessage(code, new Object[]{});
//    }

//    public static String getMessage(String code, String defaultMessage) {
//        return getMessage(code, defaultMessage);
//    }

//    public static String getMessage(String code, String defaultMessage, Locale locale) {
//        return getMessage(code, defaultMessage, locale);
//    }

    public static String getMessage(String code, Locale locale) {
        return getMessage(code, "", locale);
    }

    public static String getMessage(String code, Object... args) {
        return getMessage(code, "", args);
    }

    public static String getMessage(String code, Locale locale, Object... args) {
        return getMessage(code, "", locale, args);
    }

    public static String getMessage(String code, String defaultMessage, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return getMessage(code, defaultMessage, locale, args);
    }

    public static String getMessage(String code, String defaultMessage, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }
}
