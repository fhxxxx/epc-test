package com.envision.bunny.demo.capability.notice;

import com.envision.bunny.infrastructure.notice.mail.MailBody;
import com.envision.bunny.infrastructure.notice.mail.MailUtils;
import com.envision.bunny.infrastructure.notice.wecom.WecomUtils;
import com.envision.bunny.infrastructure.notice.wecom.msg.TextMsgBody;
import com.envision.bunny.infrastructure.notice.sms.SmsBody;
import com.envision.bunny.infrastructure.notice.sms.SmsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * 消息通知
 *
 * @author jingjing.dong
 * @since 2021/4/15-18:07
 */
@RestController
@RequestMapping("/notice")
public class ValidationAndNoticeAndAsyncTest {
    private static final String SUCCESS = "Success";
    @Autowired
    MailUtils mailUtils;
    @Autowired
    SmsUtils smsUtils;
    @Autowired
    WecomUtils wecomUtils;

    /**
     * 发送邮件
     * @param address 邮件地址|jingjing.dong@envision-energy.com
     */
    @GetMapping("/mail")
    public String test(String address) {
        MailBody body = MailBody.builder().addBcc(address)
                .subject("TestSubject").content("TestText").build();
        mailUtils.send(body);
        return SUCCESS;
    }

    /**
     * 发短信
     * @param num 手机号|17051066161
     */
    @GetMapping("/sms")
    public String test2(String num){
        SmsBody body = SmsBody.builder().businessId("03").addNum(num).content("TestMessage").build();
        smsUtils.send(body);
        return SUCCESS;
    }

    /**
     * 发企业微信
     * @param to 接收人|59930
     * @param content 内容|你好
     */
    @GetMapping("/wecom")
    public String send(String to, String content) {
        TextMsgBody body = TextMsgBody.builder().sendTo(Collections.singletonList(to)).content(content).build();
        wecomUtils.sendTextMsg(body);
        return SUCCESS;
    }
}
