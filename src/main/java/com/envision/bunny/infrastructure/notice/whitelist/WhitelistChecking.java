package com.envision.bunny.infrastructure.notice.whitelist;

import java.util.List;

/**
 * @author jingjing.dong
 * @since 2021/5/27-12:09
 */
public interface WhitelistChecking {
    List<String> getSendTo();

    String getMsgType();
}
