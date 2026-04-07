package com.envision.epc.infrastructure.util.redis;

import com.envision.epc.infrastructure.util.ApplicationContextUtils;
import com.envision.epc.infrastructure.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * hash相关操作
 *
 * 提示: 简单的，可以将redis中hash的数据结构看作是 Map<String, Map<HK, HV>>
 * 提示: redis中String的数据结构可参考resources/data-structure/Hash(散列)的数据结构(示例一).png
 *      redis中String的数据结构可参考resources/data-structure/Hash(散列)的数据结构(示例二).png
 *
 * @author Jingjing.dong
 * @since 2020/3/8 23:39:26
 */
@Slf4j
@Component("hashOps")
@SuppressWarnings("unused")
public class HashOps {
    private static RedisTemplate<String,Object> redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String,Object> redisTemplate){
        HashOps.redisTemplate = redisTemplate;
    }
    /**
     * 向key对应的hash中，增加一个键值对entryKey-entryValue
     *
     * 注: 同一个hash里面，若已存在相同的entryKey， 那么此操作将丢弃原来的entryKey-entryValue，
     *     而使用新的entryKey-entryValue。
     *
     *
     * @param key
     *            定位hash的key
     * @param entryKey
     *            要向hash中增加的键值对里的 键
     * @param entryValue
     *            要向hash中增加的键值对里的 值
     * @since 2020/3/8 23:49:52
     */
    public static void hPut(String key, String entryKey, Object entryValue) {
        log.info("hPut(...) => key -> {}, entryKey -> {}, entryValue -> {}", key, entryKey, entryValue);
        redisTemplate.opsForHash().put(key, entryKey, entryValue);
    }

    /**
     * 向key对应的hash中，增加maps(即: 批量增加entry集)
     *
     * 注: 同一个hash里面，若已存在相同的entryKey， 那么此操作将丢弃原来的entryKey-entryValue，
     *     而使用新的entryKey-entryValue
     *
     * @param key
     *            定位hash的key
     * @param maps
     *            要向hash中增加的键值对集
     * @since 2020/3/8 23:49:52
     */
    public static void hPutAll(String key, Map<String, Object> maps) {
        log.info("hPutAll(...) => key -> {}, maps -> {}", key, maps);
        redisTemplate.opsForHash().putAll(key, maps);
    }

    /**
     * 当key对应的hash中,不存在entryKey时，才(向key对应的hash中，)增加entryKey-entryValue
     * 否者，不进行任何操作
     *
     * @param key
     *            定位hash的key
     * @param entryKey
     *            要向hash中增加的键值对里的 键
     * @param entryValue
     *            要向hash中增加的键值对里的 值
     *
     * @return 操作是否成功。
     * @since 2020/3/8 23:49:52
     */
    public static boolean hPutIfAbsent(String key, String entryKey, Object entryValue) {
        log.info("hPutIfAbsent(...) => key -> {}, entryKey -> {}, entryValue -> {}",
                key, entryKey, entryValue);
        Boolean result = redisTemplate.opsForHash().putIfAbsent(key, entryKey, entryValue);
        log.info("hPutIfAbsent(...) => result -> {}", result);
        return result;
    }

    /**
     * 获取到key对应的hash里面的对应字段的值
     *
     * 注: 若redis中不存在对应的key, 则返回null。
     *     若key对应的hash中不存在对应的entryKey, 也会返回null。
     *
     * @param key
     *            定位hash的key
     * @param entryKey
     *            定位hash里面的entryValue的entryKey
     *
     * @return  key对应的hash里的entryKey对应的entryValue值
     * @since 2020/3/9 9:09:30
     */
    public static Object hGet(String key, Object entryKey) {
        log.info("hGet(...) => key -> {}, entryKey -> {}", key, entryKey);
        Object entryValue = redisTemplate.opsForHash().get(key, entryKey);
        log.info("hGet(...) => entryValue -> {}", entryValue);
        return entryValue;
    }

    /**
     * 获取到key对应的hash(即: 获取到key对应的Map<HK, HV>)
     *
     * 注: 若redis中不存在对应的key, 则返回一个没有任何entry的空的Map(，而不是返回null)。
     *
     * @param key
     *            定位hash的key
     *
     * @return  key对应的hash。
     * @since 2020/3/9 9:09:30
     */
    public static Map<String, Object> hGetAll(String key) {
        log.info("hGetAll(...) => key -> {}",  key);
        Map<String, Object> result = redisTemplate.<String, Object>opsForHash().entries(key);
        log.info("hGetAll(...) => result -> {}", result);
        return result;
    }

    /**
     * 批量获取(key对应的)hash中的entryKey的entryValue
     *
     * 注: 若hash中对应的entryKey不存在，那么返回的对应的entryValue值为null
     * 注: redis中key不存在，那么返回的List中，每个元素都为null。
     *     追注: 这个List本身不为null, size也不为0， 只是每个list中的每个元素为null而已。
     *
     * @param key
     *            定位hash的key
     * @param entryKeys
     *            需要获取的hash中的字段集
     * @return  hash中对应entryKeys的对应entryValue集
     * @since 2020/3/9 9:25:38
     */
    public static List<Object> hMultiGet(String key, Collection<String> entryKeys) {
        log.info("hMultiGet(...) => key -> {}, entryKeys -> {}", key, entryKeys);
        List<Object> entryValues = redisTemplate.<String, Object>opsForHash().multiGet(key, entryKeys);
        log.info("hMultiGet(...) => entryValues -> {}", entryValues);
        return entryValues;
    }

    /**
     * (批量)删除(key对应的)hash中的对应entryKey-entryValue
     *
     * 注: 1、若redis中不存在对应的key, 则返回0;
     *     2、若要删除的entryKey，在key对应的hash中不存在，在count不会+1, 如:
     *                 RedisUtil.HashOps.hPut("ds", "name", "邓沙利文");
     *                 RedisUtil.HashOps.hPut("ds", "birthday", "1994-02-05");
     *                 RedisUtil.HashOps.hPut("ds", "hobby", "女");
     *                 则调用RedisUtil.HashOps.hDelete("ds", "name", "birthday", "hobby", "non-exist-entryKey")
     *                 的返回结果为3
     * 注: 若(key对应的)hash中的所有entry都被删除了，那么该key也会被删除
     *
     * @param key
     *            定位hash的key
     * @param entryKeys
     *            定位要删除的entryKey-entryValue的entryKey
     *
     * @return 删除了对应hash中多少个entry
     * @since 2020/3/9 9:37:47
     */
    public static long hDelete(String key, Object... entryKeys) {
        log.info("hDelete(...) => key -> {}, entryKeys -> {}", key, entryKeys);
        Long count = redisTemplate.opsForHash().delete(key, entryKeys);
        log.info("hDelete(...) => count -> {}", count);
        return count;
    }

    /**
     * 查看(key对应的)hash中，是否存在entryKey对应的entry
     *
     * 注: 若redis中不存在key,则返回false。
     * 注: 若key对应的hash中不存在对应的entryKey, 也会返回false。
     *
     * @param key
     *            定位hash的key
     * @param entryKey
     *            定位hash中entry的entryKey
     *
     * @return  hash中是否存在entryKey对应的entry.
     * @since 2020/3/9 9:51:55
     */
    public static boolean hExists(String key, Object entryKey) {
        log.info("hDelete(...) => key -> {}, entryKeys -> {}", key, entryKey);
        Boolean exist = redisTemplate.opsForHash().hasKey(key, entryKey);
        log.info("hDelete(...) => exist -> {}", exist);
        return exist;
    }

    /**
     * 增/减(hash中的某个entryValue值) 整数
     *
     * 注: 负数则为减。
     * 注: 若key不存在，那么会自动创建对应的hash,并创建对应的entryKey、entryValue,entryValue的初始值为increment。
     * 注: 若entryKey不存在，那么会自动创建对应的entryValue,entryValue的初始值为increment。
     * 注: 若key对应的value值不支持增/减操作(即: value不是数字)， 那么会
     *     抛出org.springframework.data.redis.RedisSystemException
     *
     * @param key
     *            用于定位hash的key
     * @param entryKey
     *            用于定位entryValue的entryKey
     * @param increment
     *            增加多少
     * @return  增加后的总值。
     * @throws RedisSystemException key对应的value值不支持增/减操作时
     * @since 2020/3/9 10:09:28
     */
    public static long hIncrBy(String key, String entryKey, long increment) {
        log.info("hIncrBy(...) => key -> {}, entryKey -> {}, increment -> {}",
                key, entryKey, increment);
        Long result = redisTemplate.opsForHash().increment(key, entryKey, increment);
        log.info("hIncrBy(...) => result -> {}", result);
        return result;
    }

    /**
     * 增/减(hash中的某个entryValue值) 浮点数
     *
     * 注: 负数则为减。
     * 注: 若key不存在，那么会自动创建对应的hash,并创建对应的entryKey、entryValue,entryValue的初始值为increment。
     * 注: 若entryKey不存在，那么会自动创建对应的entryValue,entryValue的初始值为increment。
     * 注: 若key对应的value值不支持增/减操作(即: value不是数字)， 那么会
     *     抛出org.springframework.data.redis.RedisSystemException
     * 注: 因为是浮点数， 所以可能会和{@link ObjectOps#incrByFloat(String, double)}一样， 出现精度问题。
     *     追注: 本人简单测试了几组数据，暂未出现精度问题。
     *
     * @param key
     *            用于定位hash的key
     * @param entryKey
     *            用于定位entryValue的entryKey
     * @param increment
     *            增加多少
     * @return  增加后的总值。
     * @throws RedisSystemException key对应的value值不支持增/减操作时
     * @since 2020/3/9 10:09:28
     */
    public static double hIncrByFloat(String key, String entryKey, double increment) {
        RedisTemplate<String, Object> redisTemplate = ApplicationContextUtils.getBean(RedisTemplate.class);
        log.info("hIncrByFloat(...) => key -> {}, entryKey -> {}, increment -> {}",
                key, entryKey, increment);
        Double result = redisTemplate.opsForHash().increment(key, entryKey, increment);
        log.info("hIncrByFloat(...) => result -> {}", result);
        return result;
    }

    /**
     * 获取(key对应的)hash中的所有entryKey
     *
     * 注: 若key不存在，则返回的是一个空的Set(，而不是返回null)
     *
     * @param key
     *            定位hash的key
     *
     * @return  hash中的所有entryKey
     * @since 2020/3/9 10:30:13
     */
    public static Set<Object> hKeys(String key) {
        log.info("hKeys(...) => key -> {}", key);
        Set<Object> entryKeys = redisTemplate.opsForHash().keys(key);
        log.info("hKeys(...) => entryKeys -> {}", entryKeys);
        return entryKeys;
    }

    /**
     * 获取(key对应的)hash中的所有entryValue
     *
     * 注: 若key不存在，则返回的是一个空的List(，而不是返回null)
     *
     * @param key
     *            定位hash的key
     *
     * @return  hash中的所有entryValue
     * @since 2020/3/9 10:30:13
     */
    public static List<Object> hValues(String key) {
        log.info("hValues(...) => key -> {}", key);
        List<Object> entryValues = redisTemplate.opsForHash().values(key);
        log.info("hValues(...) => entryValues -> {}", entryValues);
        return entryValues;
    }

    /**
     * 获取(key对应的)hash中的所有entry的数量
     *
     * 注: 若redis中不存在对应的key, 则返回值为0
     *
     * @param key
     *            定位hash的key
     *
     * @return  (key对应的)hash中,entry的个数
     * @since 2020/3/9 10:41:01
     */
    public static long hSize(String key) {
        log.info("hSize(...) => key -> {}", key);
        Long count = redisTemplate.opsForHash().size(key);
        log.info("hSize(...) => count -> {}", count);
        return count;
    }

    /**
     * 根据options匹配到(key对应的)hash中的对应的entryKey, 并返回对应的entry集
     *
     *
     * 注: ScanOptions实例的创建方式举例:
     *     1、ScanOptions.NONE
     *     2、ScanOptions.scanOptions().match("n??e").build()
     *
     * @param key
     *            定位hash的key
     * @param options
     *            匹配entryKey的条件
     *            注: ScanOptions.NONE表示全部匹配。
     *            注: ScanOptions.scanOptions().match(pattern).build()表示按照pattern匹配,
     *                其中pattern中可以使用通配符 * ? 等,
     *                * 表示>=0个字符
     *                ？ 表示有且只有一个字符
     *                此处的匹配规则与{@link KeyOps#keys(String)}处的一样。
     *
     * @return  匹配到的(key对应的)hash中的entry
     * @since 2020/3/9 10:49:27
     */
    public static Cursor<Map.Entry<String, Object>> hScan(String key, ScanOptions options) {
        log.info("hScan(...) => key -> {}, options -> {}", key, JsonUtils.toJsonStr(options));
        Cursor<Map.Entry<String, Object>> cursor = redisTemplate.<String, Object>opsForHash().scan(key, options);
        log.info("hScan(...) => cursor -> {}", JsonUtils.toJsonStr(cursor));
        return cursor;
    }
}
