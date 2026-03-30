package com.envision.bunny.infrastructure.util.redis;

import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.envision.bunny.infrastructure.util.ApplicationContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * redis分布式锁.
 *
 * 使用方式(示例):
 * 			boolean flag = false;
 * 			String lockName = "sichuan";
 * 			String lockValue = UUID.randomUUID().toString();
 * 			try {
 * 		        //	非阻塞获取(锁的最大存活时间采用默认值)
 * 				flag = LockOps.getLock(lockName, lockValue);
 * 				//	非阻塞获取e.g.
 * 				flag = LockOps.getLock(lockName, lockValue, 3, TimeUnit.SECONDS);
 * 			    // 阻塞获取(锁的最大存活时间采用默认值)
 * 		        flag = LockOps.getLockUntilTimeout(lockName, lockValue, 2000);
 * 		        // 阻塞获取e.g.
 * 		        flag = LockOps.getLockUntilTimeout(lockName, lockValue, 2, TimeUnit.SECONDS, 2000);
 * 				// your logic
 * 			    //	...
 *          } finally {
 * 				if (flag) {
 * 					LockOps.releaseLock(lockName, lockValue);
 *              }
 *          }
 *
 * @author jingjing.dong
 * @since 2021/3/29-16:49
 */
@Slf4j
@Component
@SuppressWarnings("unused")
public class LockOps {
    /** lua脚本, 保证 释放锁脚本 的原子性(以避免, 并发场景下, 释放了别人的锁) */
    private static final RedisScript<Boolean> RELEASE_LOCK_LUA;

    /** 分布式锁默认(最大)存活时长 */
    public static final long DEFAULT_LOCK_TIMEOUT = 30;

    /** DEFAULT_LOCK_TIMEOUT的单位 */
    public static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

    static {
        // 不论lua中0是否代表失败; 对于java的Boolean而言, 返回0, 则会被解析为false
        RELEASE_LOCK_LUA = RedisScript.of("if redis.call('get',KEYS[1]) == ARGV[1] "
                + "then "
                + "    return redis.call('del',KEYS[1]) "
                + "else "
                + "    return 0 "
                + "end ", Boolean.class);
    }

    /**
     * 获取(分布式)锁.
     *
     * 注: 获取结果是即时返回的、是非阻塞的。
     *
     * @see LockOps#getLock(String, String, long, TimeUnit)
     */
    public static boolean getLock(final String key, final String value) {
        return getLock(key, value, DEFAULT_LOCK_TIMEOUT, DEFAULT_TIMEOUT_UNIT);
    }

    /**
     * 获取(分布式)锁。
     * 若成功, 则直接返回;
     * 若失败, 则进行重试, 直到成功 或 超时为止。
     *
     * 注: 获取结果是阻塞的， 要么成功, 要么超时, 才返回。
     *
     * @param retryTimeoutLimit
     *            重试的超时时长(ms)
     * 其它参数可详见:
     *    @see LockOps#getLock(String, String, long, TimeUnit)
     *
     * @return 是否成功
     */
    public static boolean getLockUntilTimeout(final String key, final String value,
                                              final long retryTimeoutLimit) {
        return getLockUntilTimeout(key, value, DEFAULT_LOCK_TIMEOUT, DEFAULT_TIMEOUT_UNIT, retryTimeoutLimit);
    }

    /**
     * 获取(分布式)锁。
     * 若成功, 则直接返回;
     * 若失败, 则进行重试, 直到成功 或 超时为止。
     *
     * 注: 获取结果是阻塞的， 要么成功, 要么超时, 才返回。
     *
     * @param retryTimeoutLimit
     *            重试的超时时长(ms)
     * 其它参数可详见:
     *    @see LockOps#getLock(String, String, long, TimeUnit, boolean)
     *
     * @return 是否成功
     */
    public static boolean getLockUntilTimeout(final String key, final String value,
                                              final long timeout, final TimeUnit unit,
                                              final long retryTimeoutLimit) {
        long startTime = Instant.now().toEpochMilli();
        long now = startTime;
        do {
            try {
                boolean alreadyGotLock = getLock(key, value, timeout, unit, false);
                if (alreadyGotLock) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("getLockUntilTimeout(...) => try to get lock failure! e.getMessage -> {}",
                        e.getMessage());
            }
            now = Instant.now().toEpochMilli();
        } while (now < startTime + retryTimeoutLimit);
        return false;
    }

    /**
     * 获取(分布式)锁
     *
     * 注: 获取结果是即时返回的、是非阻塞的。
     *
     * @see LockOps#getLock(String, String, long, TimeUnit, boolean)
     */
    public static boolean getLock(final String key, final String value,
                                  final long timeout, final TimeUnit unit) {
        return getLock(key, value, timeout, unit, true);
    }

    /**
     * 获取(分布式)锁
     *
     * 注: 获取结果是即时返回的、是非阻塞的。
     *
     * @param key
     *            锁名
     * @param value
     *            锁名对应的value
     *            注: value一般采用全局唯一的值， 如: requestId、uuid等。
     *               这样， 释放锁的时候, 可以再次验证value值,
     *               保证自己上的锁只能被自己释放, 而不会被别人释放。
     *               当然, 如果锁超时时, 会被redis自动删除释放。
     * @param timeout
     *            锁的(最大)存活时长
     *            注: 一般的， 获取锁与释放锁 都是成对使用的, 在锁在达到(最大)存活时长之前，都会被主动释放。
     *                但是在某些情况下(如:程序获取锁后,释放锁前,崩了),锁得不到释放, 这时就需要等锁过
     *                了(最大)存活时长后，被redis自动删除清理了。这样就能保证redis中不会留下死数据。
     * @param unit
     *            timeout的单位
     * @param recordLog
     *            是否记录日志
     *
     * @return 是否成功
     */
    public static boolean getLock(final String key, final String value,
                                  final long timeout, final TimeUnit unit,
                                  boolean recordLog) {
        if (recordLog) {
            log.info("getLock(...) => key -> {}, value -> {}, timeout -> {}, unit -> {}, recordLog -> {}",
                    key, value, timeout, unit, true);
        }
        StringRedisTemplate redisTemplate = ApplicationContextUtils.getBean(StringRedisTemplate.class);
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
        if (recordLog) {
            log.info("getLock(...) => result -> {}", result);
        }
        if (result == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return result;
    }

    /**
     * 释放(分布式)锁
     *
     * 注: 此方式能(通过value的唯一性)保证: 自己加的锁, 只能被自己释放。
     * 注: 锁超时时, 也会被redis自动删除释放。
     *
     * @param key
     *            锁名
     * @param value
     *            锁名对应的value
     *
     * @return 释放锁是否成功
     * @since 2020/3/15 17:00:45
     */
    public static boolean releaseLock(final String key, final String value) {
        StringRedisTemplate redisTemplate = ApplicationContextUtils.getBean(StringRedisTemplate.class);
        log.info("releaseLock(...) => key -> {}, lockValue -> {}", key, value);
        Boolean result = redisTemplate.execute(
                RELEASE_LOCK_LUA,
                Collections.singletonList(key),
                value
        );
        log.info("releaseLock(...) => result -> {}", result);
        if (result == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return result;
    }

    /**
     * 释放锁, 不校验该key对应的value值
     *
     * 注: 此方式释放锁，可能导致: 自己加的锁, 结果被别人释放了。
     *     所以不建议使用此方式释放锁。
     *
     * @param key
     *            锁名
     * @since 2020/3/15 18:56:59
     */
    @Deprecated
    public static void releaseLock(final String key) {
        KeyOps.delete(key);
    }
}

