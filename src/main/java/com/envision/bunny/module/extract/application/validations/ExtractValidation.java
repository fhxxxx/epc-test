package com.envision.bunny.module.extract.application.validations;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wenjun.gu
 * @since 2025/9/19-11:26
 */
@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "custom.validation.extract")
public class ExtractValidation {
    private Integer maxFileCount;
    private Integer maxFilePages;
    private Integer maxSingleCount;
    private Integer maxCompositeCount;
    private Integer maxPrimitiveCount;
    private Integer maxFieldLength;
    private Integer maxDescriptionLength;
}
