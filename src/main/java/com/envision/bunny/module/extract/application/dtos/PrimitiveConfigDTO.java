package com.envision.bunny.module.extract.application.dtos;

import com.envision.extract.module.extract.domain.PrimitiveTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/13-15:07
 */
@Getter
@Setter
@ToString
public class PrimitiveConfigDTO {
    /**
     * 名称
     */
    private String name;
    /**
     * 类型 string, number
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
    private Boolean required;
    /**
     * 是否是手动新增的，不用于提取流程
     */
    private boolean generate;
}
