package com.envision.bunny.infrastructure.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.Locale;

import static org.springframework.web.servlet.i18n.SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME;

/**
 * @author jingjing.dong
 * @since 2021/5/20-16:59
 */
public class HttpSessionUtils {
    static final String IS_FROM_WXWORK = "IS-FROM-WXWORK";

    public static <T> T get(String name) {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (sra != null) {
            HttpSession session = sra.getRequest().getSession();
            return (T) session.getAttribute(name);
        } else {
            return null;
        }
    }

    public static <T> void set(String name, T value) {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (sra != null) {
            HttpSession session = sra.getRequest().getSession();
            session.setAttribute(name, value);
        }
    }

    public static void setLocale(Locale locale) {
        HttpSessionUtils.set(LOCALE_SESSION_ATTRIBUTE_NAME, locale);
    }

    public static void setFromWxwork(boolean fromWxwork) {
        HttpSessionUtils.set(IS_FROM_WXWORK, fromWxwork);
    }

    public static boolean getFromWxwork() {
        return Boolean.TRUE.equals(HttpSessionUtils.get(IS_FROM_WXWORK));
    }

}

