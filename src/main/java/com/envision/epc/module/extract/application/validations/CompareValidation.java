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
@ConfigurationProperties(prefix = "custom.validation.compare")
public class CompareValidation {
    //公司code最大长度限制
    private Integer companyCodeLimit;
    //对比最大可选文件数量
    private Integer maxFileCount;
    //数据湖捞取最大公司可选数量
    private Integer maxCompanyCodeCount;
}
