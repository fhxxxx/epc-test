package com.envision.epc.module.extract.application.validations;

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
@ConfigurationProperties(prefix = "custom.validation.upload-file")
public class UploadFileValidation {
    private Integer maxSize;
    private Integer maxPageCount;
}
