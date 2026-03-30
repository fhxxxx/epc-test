package com.envision.bunny.infrastructure.notice.wecom.msg;


import lombok.Getter;

/**
 * 转换为此对象后，才能调用企业微信接口进行消息发送
 * @author jingjing.dong
 * @since 2021/4/27-15:47
 */
@Getter
public class TextEnrichedBody {
    private String touser;
    private final String msgtype = "text";
    private long agentid;
    private String secret;
    private Text text;
    public TextEnrichedBody(){
    }
    public TextEnrichedBody(long agentid, String secret, TextMsgBody body){
        this.agentid = agentid;
        this.secret = secret;
        this.touser = String.join("|",body.getSendTo());
        this.text = new Text(body.getContent());
    }
    private static class Text {
        private String content;
        private Text(String content) {
          this.content = content;
        }
        public String getContent() {
            return this.content;
        }
    }
}