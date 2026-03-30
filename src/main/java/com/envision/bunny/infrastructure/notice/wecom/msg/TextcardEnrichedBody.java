package com.envision.bunny.infrastructure.notice.wecom.msg;


import lombok.Getter;

/**
 * 转换为此对象后，才能调用企业微信接口进行消息发送
 *
 * @author jingjing.dong
 * @since 2021/4/27-15:47
 */
@Getter
public class TextcardEnrichedBody {
    private String touser;
    private final String msgtype = "textcard";
    private long agentid;
    private String secret;
    private Textcard textcard;

    public TextcardEnrichedBody() {
    }

    public TextcardEnrichedBody(long agentid, String secret, TextcardMsgBody body) {
        this.agentid = agentid;
        this.secret = secret;
        this.touser = String.join("|", body.getSendTo());
        this.textcard = new Textcard(body);
    }

    @Getter
    private static class Textcard {
        private final String title;
        private final String description;
        private final String url;
        private final String btntxt;

        private Textcard(TextcardMsgBody body) {
            this.title = body.getTitle();
            this.description = body.getDescription();
            this.url = body.getUrl();
            this.btntxt = body.getBtntxt();
        }
    }
}