package com.envision.bunny.infrastructure.notice.whitelist;

import cn.hutool.core.text.CharSequenceUtil;
import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 白名单检查
 *
 * @author jingjing.dong
 * @since 2021/5/27-10:48
 */
@Aspect
@Component
@Profile({"dev", "uat", "local"})
@ConfigurationProperties(prefix = "custom")
@Slf4j
public class WhitelistAspect {
    @Setter
    private Map<String, List<String>> whitelist;

    /**
     * 自定义发送消息后的切点
     */
    @Pointcut("@annotation(com.envision.bunny.infrastructure.notice.whitelist.Whitelist)")
    private void pointcut() {
    }

    /**
     * 对非生产环境进行发送前的白名单检查
     */
    @Before("pointcut()")
    public void doBefore(JoinPoint jPoint) {
        Object[] args = jPoint.getArgs();
        WhitelistChecking checking = (WhitelistChecking) args[0];
        List<String> sendTo = checking.getSendTo();
        checkWhitelist(sendTo, whitelist.get(checking.getMsgType()));
    }

    private void checkWhitelist(List<String> sendTo, List<String> whitelist) {
        log.info("whitelist :[{}],send to list :[{}]", CharSequenceUtil.join("|",whitelist),CharSequenceUtil.join("|",sendTo));
        if (!new HashSet<>(whitelist).containsAll(sendTo)) {
            throw new BizException(ErrorCode.CHECK_MESSAGE_WHITELIST_FAIL);
        }
    }
}
