package com.envision.bunny.facade.azure;

import com.envision.extract.infrastructure.response.BizException;
import com.envision.extract.infrastructure.response.ErrorCode;
import com.envision.extract.module.project.domain.enums.ProjectTypeEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wenjun.gu
 * @since 2025/8/14-16:45
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "custom.azure.blob")
public class BlobStorageConfig {
    private String endpoint;
    private String accountName;
    private String accountKey;
    private String containerName;
    private String stgContainerName;

    public String getContainerNameByProject(ProjectTypeEnum type) {
        if (type == ProjectTypeEnum.TURBINE) {
            return containerName;
        } else if (type == ProjectTypeEnum.ENERGY_STORAGE) {
            return stgContainerName;
        } else {
            throw new BizException(ErrorCode.BAD_REQUEST, "项目类型不存在");
        }
    }

}
