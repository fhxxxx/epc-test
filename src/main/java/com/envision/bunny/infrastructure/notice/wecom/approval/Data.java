package com.envision.bunny.infrastructure.notice.wecom.approval;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jingjing.dong
 * @since 2021/4/27-19:30
 */
@Getter
@Setter
public class Data {
    private String key;
    private String label;
    private String value;
    private boolean hidden;
}
