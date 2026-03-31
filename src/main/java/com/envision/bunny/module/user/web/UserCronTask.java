package com.envision.bunny.module.user.web;

import com.envision.extract.facade.platform.PlatformRemote;
import com.envision.extract.module.user.application.UserCommandService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author yakun.meng
 * @since 2024/4/30
 */
@Service
@Profile("prod")
public class UserCronTask {

    @Resource
    private UserCommandService userCommandService;
    @Resource
    private PlatformRemote platformRemote;
    @Value("${custom.platform.token.domain}")
    private String sysUserUrl;


    @Scheduled(cron = "${custom.user.cron-expression}", zone = "Asia/Shanghai")
    @SchedulerLock(name = "UserCronTask_syncSysUser", lockAtMostFor = "PT1M", lockAtLeastFor = "PT1M")
    public void syncSysUserData() {
        userCommandService.syncWithDataLake();
    }
}
