package com.envision.bunny.infrastructure.notice.alarm;


import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ObjectUtil;
import com.envision.bunny.infrastructure.notice.wecom.WecomUtils;
import com.envision.bunny.infrastructure.notice.wecom.msg.TextMsgBody;
import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.envision.bunny.infrastructure.util.ApplicationContextUtils;
import com.envision.bunny.infrastructure.util.redis.KeyOps;
import com.envision.bunny.infrastructure.util.redis.ObjectOps;
import org.springframework.core.env.Environment;
import org.springframework.security.web.firewall.RequestRejectedException;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author jingjing.dong
 * @since 2021/10/8-14:59
 */
public class WecomAlarm extends AppenderBase<ILoggingEvent> {

    static final String MSG_TEMPLATE = "<div class=\"blue\">应用:</div>{}<div class=\"blue\">时间:</div>{}<div class=\"blue\">Logger " +
            "Name:</div> {}\n<div class=\"blue\">消息:</div> {}<div class=\"blue\">线程:</div>{}<div class=\"blue\">MDC:</div>{}";

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        if (needIgnore(iLoggingEvent)) {
            return;
        }
        final WecomUtils utils = ApplicationContextUtils.getBean(WecomUtils.class);
        final Environment environment = ApplicationContextUtils.getBean(Environment.class);
        List<String> watcher = CharSequenceUtil.split(environment.getProperty("custom.alarm.watcher"), StrPool.COMMA);
        String appName = environment.getProperty("spring.application.name") + "  " + environment.getProperty("SPRING_PROFILES_ACTIVE") + "  " + environment.getProperty("CLOUD_AWS_REGION_STATIC");
        String msg = assembleMsg(iLoggingEvent);
        String content = CharSequenceUtil.format(MSG_TEMPLATE, appName, Convert.toLocalDateTime(iLoggingEvent.getTimeStamp()).toString(), iLoggingEvent.getLoggerName(),
                msg, iLoggingEvent.getThreadName(), iLoggingEvent.getMDCPropertyMap().toString());
        if (KeyOps.hasKey(msg)) {
            return;
        }
//        utils.sendTextMsg(TextMsgBody.builder().sendTo(watcher).content(content).build());
        ObjectOps.setEx(msg, "", 60, TimeUnit.SECONDS);
    }

    private boolean needIgnore(ILoggingEvent iLoggingEvent) {
        IThrowableProxy iThrowableProxy = iLoggingEvent.getThrowableProxy();
        if (iThrowableProxy instanceof ThrowableProxy) {
            ThrowableProxy throwableProxy = (ThrowableProxy) iThrowableProxy;
            Throwable throwable = throwableProxy.getThrowable();
            if (throwable instanceof BizException) {
                BizException e = (BizException) throwable;
                return e.getCode() == ErrorCode.IGNORE_ALARM.getCode();
            }
        } else if (iThrowableProxy instanceof Throwable) {
            Throwable throwable = (Throwable) iThrowableProxy;
            return throwable instanceof RequestRejectedException;
        }
        return false;
    }

    private String assembleMsg(ILoggingEvent iLoggingEvent) {
        IThrowableProxy iThrowableProxy = iLoggingEvent.getThrowableProxy();
        String msg = CharSequenceUtil.format("logger:[{}]", iLoggingEvent.getFormattedMessage().replace("\"", "\\\""));
        if (ObjectUtil.isNotNull(iThrowableProxy)) {
            return msg + CharSequenceUtil.format(",detail:[{}]", iThrowableProxy.getClassName() + ":" + iThrowableProxy.getMessage());
        }
        return msg;
    }
}


