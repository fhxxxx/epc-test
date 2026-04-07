package com.envision.epc.infrastructure.notice.mail;

import com.envision.epc.infrastructure.notice.whitelist.Whitelist;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * 异步的进行邮件发送
 * @author jingjing.dong
 * @since 2021/4/14-11:52
 */
@Component
@Validated
@Slf4j
public class MailUtils {
    @Value("${spring.mail.username}")
    String mailFrom;
    @Autowired
    JavaMailSender javaMailSender;

    @Async
    @Whitelist
    public void send(@Valid MailBody body) {
        try {
            MimeMessage mimeMailMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMailMessage, true, "UTF-8");
            mimeMessageHelper.setFrom(mailFrom);
            mimeMessageHelper.setTo(body.getTo().toArray(new String[0]));
            mimeMessageHelper.setCc(body.getCc().toArray(new String[0]));
            mimeMessageHelper.setBcc(body.getBcc().toArray(new String[0]));
            mimeMessageHelper.setSubject(body.getSubject());
            mimeMessageHelper.setText(body.getContent(), body.isTextHtml());
            //文件路径
            for (File file : body.getAttachments()) {
                String filePath = file.getPath();
                mimeMessageHelper.addAttachment(MimeUtility.encodeWord(filePath.substring(filePath.lastIndexOf("/") + 1), "UTF-8", "B"),
                        file);
            }
            javaMailSender.send(mimeMailMessage);
            for (int i = 0; i < body.getAttachments().size(); i++) {
                File file = body.getAttachments().get(i);
                if (!file.delete()) {
                    log.error("文件删除失败");
                }
            }
            body.getAttachments().clear();
            body.setAttachments(null);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new BizException(ErrorCode.MAIL_SEND_ERROR);
        }
    }
}
