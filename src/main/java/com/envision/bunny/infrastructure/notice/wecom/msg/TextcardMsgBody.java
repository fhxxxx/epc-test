package com.envision.bunny.infrastructure.notice.wecom.msg;

import com.envision.bunny.infrastructure.notice.whitelist.WhitelistChecking;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.util.List;

/**
 * 提供给外部调用的消息请求体，需要转换为EnrichedBody
 *
 * @author jingjing.dong
 * @since 2021/4/27-16:52
 */
@Setter
@Getter
@Builder
public class TextcardMsgBody implements WhitelistChecking {
    @Singular("addSendTo")
    private List<String> sendTo;
    private String title;
    private String description;
    private String url;
    private String btntxt;

    @Override
    public String getMsgType() {
        return "wecom";
    }
}
