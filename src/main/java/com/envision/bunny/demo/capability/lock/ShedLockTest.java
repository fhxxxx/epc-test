package com.envision.bunny.demo.capability.lock;

import com.envision.bunny.infrastructure.crontask.shedlock.DistributedLockExecutor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

/**
 * 分布式锁
 * @author jingjing.dong
 * @since 2021/4/26-14:30
 */
@RestController
@Slf4j
public class ShedLockTest {
    @Autowired
    DistributedLockExecutor distributedLockExecutor;

    /**
     * 使用Shedlock当作分布式锁
     * @param key Key
     */
    @GetMapping("/shedlock")
    public String test(String key){
          String str = distributedLockExecutor.executeWithLock(() -> "Hello World.",
                 new LockConfiguration(Instant.now(),key, Duration.ofSeconds(10),Duration.ofSeconds(3)));
          log.info(str);
          return "Success";
    }
}
