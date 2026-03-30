package com.envision.bunny.infrastructure.notice.mail;

import com.envision.bunny.infrastructure.notice.whitelist.WhitelistChecking;
import lombok.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jingjing.dong
 * @since 2021/4/14-11:28
 */
@Getter
@Setter
@Builder
@ToString
@Mail
public class MailBody implements WhitelistChecking {
    @MailAddress
    @Singular("addBcc")
    private List<String> bcc;
    @Singular("addTo")
    @MailAddress
    private List<String> to;
    @Singular("addCc")
    @MailAddress
    private List<String> cc;
    private String subject;
    private String content;
    @Singular("addAttachment")
    private List<File> attachments;
    private boolean textHtml;

    @Override
    public List<String> getSendTo() {
        List<String> ans = new ArrayList<>();
        ans.addAll(this.getTo());
        ans.addAll(this.getCc());
        ans.addAll(this.getBcc());
        return ans;
    }

    @Override
    public String getMsgType() {
        return "email";
    }
}