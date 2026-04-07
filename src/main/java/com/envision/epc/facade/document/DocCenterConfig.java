package com.envision.epc.facade.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;

/**
 * @author jingjing.dong
 * @since 2021/6/19-16:56
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "custom.document")
public class DocCenterConfig {
    String service;
    String uploadUrl;
    String downloadUrl;
    HashMap<String, String> policy;
}
