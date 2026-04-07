package com.envision.epc.infrastructure.util.redis;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Key相关操作
 * @author jingjing.dong
 * @since 2021/3/29-11:27
 */
@Slf4j
@Component("keyOps")
@SuppressWarnings("unused")
public class KeyOps {
    private static RedisTemplate<String,Object> redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String,Object> redisTemplate){
        KeyOps.redisTemplate = redisTemplate;
    }

    /**
     * 根据key, 删除redis中的对应key-value
     *
     *  注: 若删除失败, 则返回false。
     *
     *      若redis中，不存在该key, 那么返回的也是false。
     *      所以，不能因为返回了false,就认为redis中一定还存
     *      在该key对应的key-value。
     *
     * @param key
     *            要删除的key
     * @return  删除是否成功
     * @since 2020/3/7 17:15:02
     */
    public static boolean delete(String key) {
        log.info("delete(...) => key -> {}", key);
        // 返回值只可能为true/false, 不可能为null
        Boolean result = redisTemplate.delete(key);
        log.info("delete(...) => result -> {}", result);
        if (result == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return result;
    }

    /**
     * 根据keys, 批量删除key-value
     *
     * 注: 若redis中，不存在对应的key, 那么计数不会加1, 即:
     *     redis中存在的key-value里，有名为a1、a2的key，
     *     删除时，传的集合是a1、a2、a3，那么返回结果为2。
     *
     * @param keys
     *            要删除的key集合
     * @return  删除了的key-value个数
     * @since 2020/3/7 17:48:04
     */
    public static long delete(Collection<String> keys) {
        log.info("delete(...) => keys -> {}", keys);
        Long count = redisTemplate.delete(keys);
        log.info("delete(...) => count -> {}", count);
        if (count == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return count;
    }

    /**
     * 将key对应的value值进行序列化，并返回序列化后的value值。
     *
     * 注: 若不存在对应的key, 则返回null。
     * 注: dump时，并不会删除redis中的对应key-value。
     * 注: dump功能与restore相反。
     *
     * @param key
     *            要序列化的value的key
     * @return  序列化后的value值
     * @since 2020/3/8 11:34:13
     */
    public static byte[] dump(String key) {
        log.info("dump(...) =>key -> {}", key);
        byte[] result = redisTemplate.dump(key);
        log.info("dump(...) => result -> {}", result);
        return result;
    }

    /**
     * 将给定的value值，反序列化到redis中, 形成新的key-value。
     *
     * @param key
     *            value对应的key
     * @param value
     *            要反序列的value值。
     *            注: 这个值可以由{@link this#dump(String)}获得
     * @param timeToLive
     *            反序列化后的key-value的存活时长
     * @param unit
     *            timeToLive的单位
     *
     * @throws RedisSystemException
     *             如果redis中已存在同样的key时，抛出此异常
     * @since 2020/3/8 11:36:45
     */
    public static void restore(String key, byte[] value, long timeToLive, TimeUnit unit) {
        restore(key, value, timeToLive, unit, false);
    }

    /**
     * 将给定的value值，反序列化到redis中, 形成新的key-value。
     *
     * @param key
     *            value对应的key
     * @param value
     *            要反序列的value值。
     *            注: 这个值可以由{@link this#dump(String)}获得
     * @param timeout
     *            反序列化后的key-value的存活时长
     * @param unit
     *            timeout的单位
     * @param replace
     *            若redis中已经存在了相同的key, 是否替代原来的key-value
     *
     * @throws RedisSystemException
     *             如果redis中已存在同样的key, 且replace为false时，抛出此异常
     * @since 2020/3/8 11:36:45
     */
    public static void restore(String key, byte[] value, long timeout, TimeUnit unit, boolean replace) {
        log.info("restore(...) => key -> {}, value -> {}, timeout -> {}, unit -> {}, replace -> {}",
                key, value, timeout, unit, replace);
        redisTemplate.restore(key, value, timeout, unit, replace);
    }

    /**
     * redis中是否存在,指定key的key-value
     *
     * @param key
     *            指定的key
     * @return  是否存在对应的key-value
     * @since 2020/3/8 12:16:46
     */
    public static boolean hasKey(String key) {
        log.info("hasKey(...) => key -> {}", key);
        Boolean result = redisTemplate.hasKey(key);
        log.info("hasKey(...) => result -> {}", result);
        if (result == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return result;
    }

    /**
     * 给指定的key对应的key-value设置: 多久过时
     *
     * 注:过时后，redis会自动删除对应的key-value。
     * 注:若key不存在，那么也会返回false。
     *
     * @param key
     *            指定的key
     * @param timeout
     *            过时时间
     * @param unit
     *            timeout的单位
     * @return  操作是否成功
     * @since 2020/3/8 12:18:58
     */
    public static boolean expire(String key, long timeout, TimeUnit unit) {
        log.info("expire(...) => key -> {}, timeout -> {}, unit -> {}", key, timeout, unit);
        Boolean result = redisTemplate.expire(key, timeout, unit);
        log.info("expire(...) => result is -> {}", result);
        if (result == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return result;
    }

    /**
     * 给指定的key对应的key-value设置: 什么时候过时
     *
     * 注:过时后，redis会自动删除对应的key-value。
     * 注:若key不存在，那么也会返回false。
     *
     * @param key
     *            指定的key
     * @param date
     *            啥时候过时
     *
     * @return  操作是否成功
     * @since 2020/3/8 12:19:29
     */
    public static boolean expireAt(String key, Date date) {
        log.info("expireAt(...) => key -> {}, date -> {}", key, date);
        Boolean result = redisTemplate.expireAt(key, date);
        log.info("expireAt(...) => result is -> {}", result);
        if (result == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return result;
    }

    /**
     * 找到所有匹配pattern的key,并返回该key的结合.
     *
     * 提示:若redis中键值对较多，此方法耗时相对较长，慎用！慎用！慎用！
     *
     * @param pattern
     *            匹配模板。
     *            注: 常用的通配符有:
     *                 ?    有且只有一个;
     *                 *     >=0;
     *
     * @return  匹配pattern的key的集合。 可能为null。
     * @since 2020/3/8 12:38:38
     */
    public static Set<String> keys(String pattern) {
        log.info("keys(...) => pattern -> {}", pattern);
        Set<String> keys = redisTemplate.keys(pattern);
        log.info("keys(...) => keys -> {}", keys);
        return keys;
    }

    /**
     * 将当前数据库中的key对应的key-value,移动到对应位置的数据库中。
     *
     * 注:单机版的redis,默认将存储分为16个db, index为0 到 15。
     * 注:同一个db下，key唯一； 但是在不同db中，key可以相同。
     * 注:若目标db下，已存在相同的key, 那么move会失败，返回false。
     *
     * @param key
     *            定位要移动的key-value的key
     * @param dbIndex
     *            要移动到哪个db
     * @return 移动是否成功。
     *         注: 若目标db下，已存在相同的key, 那么move会失败，返回false。
     * @since 2020/3/8 13:01:00
     */
    public static boolean move(String key, int dbIndex) {
        log.info("move(...) => key  -> {}, dbIndex -> {}", key, dbIndex);
        Boolean result = redisTemplate.move(key, dbIndex);
        log.info("move(...) =>result -> {}", result);
        if (result == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return result;
    }

    /**
     * 移除key对应的key-value的过期时间, 使该key-value一直存在
     *
     * 注: 若key对应的key-value，本身就是一直存在(无过期时间的)， 那么persist方法会返回false;
     *    若没有key对应的key-value存在，本那么persist方法会返回false;
     *
     * @param key
     *            定位key-value的key
     * @return 操作是否成功
     * @since 2020/3/8 13:10:02
     */
    public static boolean persist(String key) {
        log.info("persist(...) => key -> {}", key);
        Boolean result = redisTemplate.persist(key);
        log.info("persist(...) => result -> {}", result);
        if (result == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return result;
    }

    /**
     * 获取key对应的key-value的过期时间
     *
     * 注: 若key-value永不过期， 那么返回的为-1。
     * 注: 若不存在key对应的key-value， 那么返回的为-2
     * 注:若存在零碎时间不足1 SECONDS,则(大体上)四舍五入到SECONDS级别。
     *
     * @param key
     *            定位key-value的key
     * @return  过期时间(单位s)
     * @since 2020/3/8 13:17:35
     */
    public static long getExpire(String key) {
        return getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 获取key对应的key-value的过期时间
     *
     * 注: 若key-value永不过期， 那么返回的为-1。
     * 注: 若不存在key对应的key-value， 那么返回的为-2
     * 注:若存在零碎时间不足1 unit,则(大体上)四舍五入到unit别。
     *
     * @param key
     *            定位key-value的key
     * @return  过期时间(单位unit)
     * @since 2020/3/8 13:17:35
     */
    public static long getExpire(String key, TimeUnit unit) {
        log.info("getExpire(...) =>key -> {}, unit is -> {}", key, unit);
        Long result = redisTemplate.getExpire(key, unit);
        log.info("getExpire(...) => result ->  {}", result);
        if (result == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return result;
    }

    /**
     * 从redis的所有key中，随机获取一个key
     *
     * 注: 若redis中不存在任何key-value, 那么这里返回null
     *
     * @return  随机获取到的一个key
     * @since 2020/3/8 14:11:43
     */
    public static String randomKey() {
        String result = redisTemplate.randomKey();
        log.info("randomKey(...) => result is -> {}", result);
        return result;
    }

    /**
     * 重命名对应的oldKey为新的newKey
     *
     * 注: 若oldKey不存在， 则会抛出异常.
     * 注: 若redis中已存在与newKey一样的key,
     *     那么原key-value会被丢弃，
     *     只留下新的key，以及原来的value
     *     示例说明: 假设redis中已有 (keyAlpha, valueAlpha) 和 (keyBeta, valueBeat),
     *              在使用rename(keyAlpha, keyBeta)替换后, redis中只会剩下(keyBeta, valueAlpha)
     *
     * @param oldKey
     *            旧的key
     * @param newKey
     *            新的key
     * @throws RedisSystemException
     *             若oldKey不存在时， 抛出此异常
     * @since 2020/3/8 14:14:17
     */
    public static void rename(String oldKey, String newKey) {
        log.info("rename(...) => oldKey -> {}, newKey -> {}", oldKey, newKey);
        redisTemplate.rename(oldKey, newKey);
    }

    /**
     * 当redis中不存在newKey时, 重命名对应的oldKey为新的newKey。
     * 否者不进行重命名操作。
     *
     * 注: 若oldKey不存在， 则会抛出异常.
     *
     * @param oldKey
     *            旧的key
     * @param newKey
     *            新的key
     * @throws RedisSystemException
     *             若oldKey不存在时， 抛出此异常
     * @since 2020/3/8 14:14:17
     */
    public static boolean renameIfAbsent(String oldKey, String newKey) {
        log.info("renameIfAbsent(...) => oldKey -> {}, newKey -> {}", oldKey, newKey);
        Boolean result = redisTemplate.renameIfAbsent(oldKey, newKey);
        log.info("renameIfAbsent(...) => result -> {}", result);
        if (result == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return result;
    }

    /**
     * 获取key对应的value的数据类型
     *
     * 注: 若redis中不存在该key对应的key-value， 那么这里返回NONE。
     *
     * @param key
     *            用于定位的key
     * @return  key对应的value的数据类型
     * @since 2020/3/8 14:40:16
     */
    public static DataType type(String key) {
        log.info("type(...) => key -> {}", key);
        DataType result = redisTemplate.type(key);
        log.info("type(...) => result -> {}", result);
        return result;
    }
}
