package com.envision.epc.module.user.application.dto;

import com.envision.epc.module.extract.application.validations.CompareValidation;
import com.envision.epc.module.extract.application.validations.ExtractValidation;
import com.envision.epc.module.extract.application.validations.UploadFileValidation;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 项目配置
 * @author gangxiang.guan
 * @date 2025/9/28 11:31
 */
@Data
@ConfigurationProperties(prefix = "custom.validation")
public class ConfigDTO {

    //提取校验配置
    private ExtractValidation extract;

    //文件校验配置
    private UploadFileValidation uploadFile;

    //对比校验配置
    private CompareValidation compare;
}
