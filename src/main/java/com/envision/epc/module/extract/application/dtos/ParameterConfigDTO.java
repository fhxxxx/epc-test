package com.envision.epc.module.extract.application.dtos;

import com.envision.epc.module.extract.domain.ParameterTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/13-15:06
 */
@Getter
@Setter
@ToString
public class ParameterConfigDTO {
    /**
     * 名称
     */
    private String name;
    /**
     * 描述
     */
    private String description;
    /**
     * 参数类型
     */
    private ParameterTypeEnum type;
    /**
     * 原子类型
     */
    private List<PrimitiveConfigDTO> primitiveConfigs = new ArrayList<>();

}
