package com.envision.bunny.module.extract.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/12-18:45
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrimitiveConfig {
    /**
     * 名称
     * @mock riqi
     */
    private String name;
    /**
     * key
     * @mock riqi
     */
    private String key;
    /**
     * 类型 STRING, STRING
     * @mock STRING
     */
    private PrimitiveTypeEnum type;
    /**
     * 选项
     */
    private List<?> options;
    /**
     * 描述
     */
    private String description;
    /**
     * 是否必填
     */
    private boolean required;
    /**
     * 是否是手动新增的，不用于提取流程
     */
    private boolean generate;

}
