package com.envision.epc.module.extract.application.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/13-15:06
 */
@Getter
@Setter
@ToString
public class ExtractConfigDTO {
    private Long id;
    private String name;
    /**
     * 参数配置
     */
    private List<ParameterConfigDTO> parameterConfigs = new ArrayList<>();
    /**
     * 每次提取页数
     */
    @Min(value = 1, message = "{min.pages.per.extraction}")
    @Max(value = 50, message = "{max.pages.per.extraction}")
    private Integer pagesPerExtraction;
}
