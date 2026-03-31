package com.envision.bunny.module.user.web;

import com.envision.extract.module.user.application.dto.ConfigDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目配置
 * @author gangxiang.guan
 * @date 2025/9/28 11:05
 */
@RestController
@RequestMapping("/config")
@EnableConfigurationProperties(ConfigDTO.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ConfigController {
    private final ConfigDTO configDTO;

    /**
     * 查询项目配置
     * @return ConfigDTO
     */
    @GetMapping
    public ConfigDTO getConfig() {
        return configDTO;
    }
}
