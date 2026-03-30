package com.envision.bunny.facade.ip;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.envision.bunny.infrastructure.util.NetUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;


import java.util.List;
import java.util.Objects;

/**
 * @author chaoyue.zhao1
 * @since 2025/12/19-13:55
 */
@Slf4j
@Component
@EnableConfigurationProperties(IpConfig.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@EnableAspectJAutoProxy(exposeProxy = true)
public class IpRemote {
    private final IpConfig config;
    private final RestClient restClient;


    public boolean checkIpInVdi(HttpServletRequest request){
        String remoteIp = NetUtils.getRemoteIp(request);
        return self().checkIpInVdi(remoteIp);
    }

    @Cacheable(value = "is_vdi", key = "#remoteIp")
    public boolean checkIpInVdi(String remoteIp){
        String url = config.getHost() + String.format(config.getPath(), remoteIp);
        IpResponse ipResponse = restClient.get()
                .uri(url)
                .retrieve()
                .body(IpResponse.class);
        List<IpResponse.IpResp> data = Objects.requireNonNull(ipResponse).getData();
        if (CollUtil.isEmpty( data)){
            return true;
        }
        String seczoneArea = data.get(0).getSeczoneArea();
        if (CharSequenceUtil.isEmpty(seczoneArea)){
            return true;
        }
        return seczoneArea.toUpperCase().contains("VDI");
    }

    private IpRemote self() {
        return (IpRemote) AopContext.currentProxy();
    }

}
