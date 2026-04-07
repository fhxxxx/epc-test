package com.envision.epc.infrastructure.notice.wecom.approval;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jingjing.dong
 * @since 2021/4/27-19:30
 */
@Getter
@Setter
public class Content {
    private ContentDetail userInfo;
    private ContentDetail detailInfo;
}
