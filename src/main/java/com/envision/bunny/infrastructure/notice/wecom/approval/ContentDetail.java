package com.envision.bunny.infrastructure.notice.wecom.approval;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author jingjing.dong
 * @since 2021/4/27-19:30
 */
@Getter
@Setter
public class ContentDetail {
    private String icon;
    private String key;
    private String title;
    private String type;
    private String avatar;
    private List<Data> data;
}
