package com.envision.bunny.infrastructure.util;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ObjectUtil;
import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author jingjing.dong
 * @since 2024/5/21-19:34
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "custom")
@Data
public class NetUtils {
    private List<String> vdiIps;
    static final String MSG_TEMPLATE = "应用名 : {}, 环境 : {}, 操作人 : {}, 操作类型 : {}, 编码 : {}, 信息 : {}, 操作时间 : {}";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String USER_AGENT_HEADER = "user-agent";
    private static final Cache<String, String> REMOTE_IP_CACHE = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(100, TimeUnit.MINUTES)
            .build();
    private static final Cache<String, Boolean> IS_FROM_WECOM_CACHE = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(100, TimeUnit.MINUTES)
            .build();
    private static final Cache<String, Boolean> IS_FROM_PC_CACHE = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(100, TimeUnit.MINUTES)
            .build();
    private static List<String> VDI_IP_LIST;

    @PostConstruct
    public void init() {
        VDI_IP_LIST = vdiIps;
    }

    public static boolean isFromVdi(String ip) {
        for (String vdiIp : VDI_IP_LIST) {
            if (NetUtil.isInRange(ip, vdiIp)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNotFromVdi(String ip) {
        return !isFromVdi(ip);
    }

    public static String getRemoteIp(HttpServletRequest request) {
        final String forwardedForStr = request.getHeader(X_FORWARDED_FOR);
        if (CharSequenceUtil.isEmpty(forwardedForStr)) {
            throw new BizException(ErrorCode.INVALID_FORWARDED_HEADER);
        }
        final String cacheValue = REMOTE_IP_CACHE.getIfPresent(forwardedForStr);
        if (ObjectUtil.isNotNull(cacheValue)) {
            return cacheValue;
        }
        log.info("X Forwarded For Header:[{}]", forwardedForStr);
        final List<String> ips = CharSequenceUtil.split(forwardedForStr, StrPool.COMMA);
        final Environment environment = ApplicationContextUtils.getBean(Environment.class);
        String thunderServiceName = environment.getProperty("THUNDER_SERVICE_NAME");
        String remoteIp;
        if (CharSequenceUtil.isEmpty(thunderServiceName) || ips.size() == 1) {
            remoteIp = CharSequenceUtil.trim(ips.get(ips.size() - 1));
        } else {
            remoteIp = CharSequenceUtil.trim(ips.get(ips.size() - 2));
        }
        log.info("X Forwarded For Header:[{}],Remote Ip:[{}],From Thunder3.0:[{}]", forwardedForStr, remoteIp, CharSequenceUtil.isNotEmpty(thunderServiceName));
        if (Validator.isIpv4(remoteIp)) {
            REMOTE_IP_CACHE.put(forwardedForStr, remoteIp);
            return remoteIp;
        }
        throw new BizException(ErrorCode.INVALID_FORWARDED_HEADER);
    }

    public static boolean isFromWecom(HttpServletRequest request) {
        String userAgent = request.getHeader(USER_AGENT_HEADER);
        if (CharSequenceUtil.isEmpty(userAgent)) {
            return false;
        }
        final Boolean cacheValue = IS_FROM_WECOM_CACHE.getIfPresent(userAgent);
        if (ObjectUtil.isNotNull(cacheValue)) {
            return cacheValue;
        }
        log.info("User Agent:[{}]", userAgent);
        final String lowerCaseStr = userAgent.toLowerCase();
        boolean result = lowerCaseStr.contains("wxwork") && lowerCaseStr.contains("micromessenger");
        IS_FROM_WECOM_CACHE.put(userAgent, result);
        return result;
    }

    public static boolean isFromPc(HttpServletRequest request) {
        String userAgent = request.getHeader(USER_AGENT_HEADER);
        if (CharSequenceUtil.isEmpty(userAgent)) {
            return true;
        }
        final Boolean cacheValue = IS_FROM_PC_CACHE.getIfPresent(userAgent);
        if (ObjectUtil.isNotNull(cacheValue)) {
            return cacheValue;
        }
        boolean result = true;
        final String lowerCaseStr = userAgent.toLowerCase();
        if (lowerCaseStr.contains("wxwork") && lowerCaseStr.contains("micromessenger") && !lowerCaseStr.contains("windows")
                && !lowerCaseStr.contains("macintosh") && CharSequenceUtil.containsAny(lowerCaseStr, "phone", "pad", "pod", "android", "mobile")) {
                result = false;
            }

        IS_FROM_PC_CACHE.put(userAgent, result);
        return result;
    }
}
