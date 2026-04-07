package com.envision.epc.module.extract.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Objects;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:10
 */
@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "custom.extract")
@NoArgsConstructor
public class DefaultExtractConfig {

    List<ExtractConfig> configList;

    /**
     * 根绝id取提取配置
     *
     */
    public ExtractConfig getExtractConfig(Long id) {
        if (CollectionUtils.isNotEmpty(configList) && id != null) {
            List<ExtractConfig> list = configList.stream().filter(config -> Objects.equals(config.getId(), id)).toList();
            if (CollectionUtils.isNotEmpty(list)) {
                return list.get(0);
            }
        }
        return null;
    }
}
