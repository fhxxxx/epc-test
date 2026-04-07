package com.envision.epc.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 使用Redis作为Session的存储
 * server.session.timeout 配置将失效
 * @author jingjing.dong
 * @since 2021/4/8-15:29
 */
@Configuration
//过期时间默认30分钟;namespace 不同应用应该有所区分;还有默认是每分钟清理一次过期session
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800,redisNamespace = "bunny")
public class RedisSessionConfig {
}
