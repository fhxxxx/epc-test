package com.envision.bunny.infrastructure.notice.wecom.msg;

import lombok.*;

/**
 * @author jingjing.dong
 * @since 2023/6/26-17:31
 */

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecallBody {
    private String msgid;
    private long agentid;
    private String secrect;
}
