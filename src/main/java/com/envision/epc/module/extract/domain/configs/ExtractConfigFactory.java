package com.envision.epc.module.extract.domain.configs;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.extract.domain.ExtractConfig;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;


/**
 * @author gangxiang.guan
 * @date 2025/10/16 14:02
 */
public class ExtractConfigFactory {

    private static final Map<Long, Class<? extends ExtractConfig>> registeredConfigs = new HashMap<>();

    static {
        registeredConfigs.put(624691423410432L, InvoiceConfig.class);
    }

    public static void registerConfig(Long id, Class<? extends ExtractConfig> configClass) {
        registeredConfigs.put(id, configClass);
    }

    public static ExtractConfig getExtractConfig(ExtractConfig config) {
        if (config == null || !registeredConfigs.containsKey(config.getId())) {
            return config;
        }
        Class<? extends ExtractConfig> configClass = registeredConfigs.get(config.getId());

        try {
            ExtractConfig extractConfig = configClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(config, extractConfig);
            return extractConfig;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new BizException(ErrorCode.SYS_ERROR, "ExtractConfigFactory创建config失败: " + e.getMessage());
        }
    }

}
