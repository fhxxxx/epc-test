package com.envision.epc.module.extract.domain;

import lombok.*;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/12-18:42
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ParameterConfig {
    /**
     * 名称
     * @mock 日期
     */
    private String name;
    /**
     * 描述
     * @mock 生产日期
     */
    private String description;
    /**
     * 参数类型:SINGLE,COMPOSITE
     * @mock SINGLE
     */
    private ParameterTypeEnum type;
    /**
     * 原子类型
     */
    private List<PrimitiveConfig> primitiveConfigs;
}
