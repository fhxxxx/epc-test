package com.envision.bunny.facade.platform;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author jingjing.dong
 * @since 2021/4/8-16:50
 */
@Setter
@Getter
class PltResp {
    private List<JsonNode> data;
    private Meta meta;
    private Links links;

    @Setter
    @Getter
    static class Meta {
        private Integer totalResourceCount;
    }

    @Setter
    @Getter
    static class Links {
        private String first;
        private String last;
        private String next;
    }
}
