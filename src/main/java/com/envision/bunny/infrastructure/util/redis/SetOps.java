package com.envision.bunny.infrastructure.util.redis;

import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.envision.bunny.infrastructure.util.ApplicationContextUtils;
import com.envision.bunny.infrastructure.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * set相关操作
 *
 * 提示: set中的元素，不可以重复。
 * 提示: set是无序的。
 * 提示: redis中String的数据结构可参考resources/data-structure/Set(集合)的数据结构(示例一).png
 *      redis中String的数据结构可参考resources/data-structure/Set(集合)的数据结构(示例二).png
 *
 * @author jingjing.dong
 * @since 2021/3/29-15:40
 */
@Slf4j
@Component("setOps")
@SuppressWarnings("unused")
public class SetOps {
    private static RedisTemplate<String,Object> redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String,Object> redisTemplate){
        SetOps.redisTemplate = redisTemplate;
    }

    /**
     * 向(key对应的)set中添加items
     *
     * 注: 若key不存在，则会自动创建。
     * 注: set中的元素会去重。
     *
     * @param key
     *            定位set的key
     * @param items
     *            要向(key对应的)set中添加的items
     *
     * @return 此次添加操作,添加到set中的元素的个数
     * @since 2020/3/11 8:16:00
     */
    public static long sAdd(String key, Object... items) {
        log.info("sAdd(...) => key -> {}, items -> {}", key, items);
        Long count = redisTemplate.opsForSet().add(key, items);
        log.info("sAdd(...) => count -> {}", count);
        if (count == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return count;
    }

    /**
     * 从(key对应的)set中删除items
     *
     * 注: 若key不存在, 则返回0。
     * 注: 若已经将(key对应的)set中的项删除完了，那么对应的key也会被删除。
     *
     * @param key
     *            定位set的key
     * @param items
     *            要移除的items
     *
     * @return 实际删除了的个数
     * @since 2020/3/11 8:26:43
     */
    public static long sRemove(String key, Object... items) {
        log.info("sRemove(...) => key -> {}, items -> {}", key, items);
        Long count = redisTemplate.opsForSet().remove(key, items);
        log.info("sRemove(...) => count -> {}", count);
        if (count == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return count;
    }

    /**
     * 从(key对应的)set中随机移出一个item, 并返回这个item
     *
     * 注: 因为set是无序的，所以移出的这个item,是随机的; 并且，哪怕
     *     是数据一样的set,多次测试移出操作,移除的元素也是随机的。
     *
     * 注: 若已经将(key对应的)set中的项pop完了，那么对应的key会被删除。
     *
     * @param key
     *            定位set的key
     *
     * @return  移出的项
     * @since 2020/3/11 8:32:40
     */
    public static Object sPop(String key) {
        log.info("sPop(...) => key -> {}", key);
        Object popItem = redisTemplate.opsForSet().pop(key);
        log.info("sPop(...) => popItem -> {}", popItem);
        return popItem;
    }

    /**
     * 将(sourceKey对应的)sourceSet中的元素item, 移动到(destinationKey对应的)destinationSet中
     *
     * 注: 当sourceKey不存在时， 返回false
     * 注: 当item不存在时， 返回false
     * 注: 若destinationKey不存在， 那么在移动时会自动创建
     * 注: 若已经将(sourceKey对应的)set中的项move出去完了，那么对应的sourceKey会被删除。
     *
     * @param sourceKey
     *            定位sourceSet的key
     * @param item
     *            要移动的项目
     * @param destinationKey
     *            定位destinationSet的key
     *
     * @return  移动成功与否
     * @since 2020/3/11 8:43:32
     */
    public static boolean sMove(String sourceKey, Object item, String destinationKey) {
        Boolean result = redisTemplate.opsForSet().move(sourceKey, item, destinationKey);
        log.info("sMove(...) => sourceKey -> {}, destinationKey -> {}, item -> {}",
                sourceKey, destinationKey, item);
        log.info("sMove(...) =>  result -> {}", result);
        if (result == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return result;
    }

    /**
     * 获取(key对应的)set中的元素个数
     *
     * 注: 若key不存在，则返回0
     *
     * @param key
     *            定位set的key
     *
     * @return  (key对应的)set中的元素个数
     * @since 2020/3/11 8:57:19
     */
    public static long sSize(String key) {
        log.info("sSize(...) => key -> {}", key);
        Long size = redisTemplate.opsForSet().size(key);
        log.info("sSize(...) => size -> {}", size);
        if (size == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return size;
    }

    /**
     * 判断(key对应的)set中是否含有item
     *
     * 注: 若key不存在，则返回false。
     *
     * @param key
     *            定位set的key
     * @param item
     *            被查找的项
     *
     * @return  (key对应的)set中是否含有item
     * @since 2020/3/11 9:03:29
     */
    public static boolean sIsMember(String key, Object item) {
        log.info("sSize(...) => key -> {}, size -> {}", key, item);
        Boolean result = redisTemplate.opsForSet().isMember(key, item);
        log.info("sSize(...) => result -> {}", result);
        if (result == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return result;
    }

    /**
     * 获取两个(key对应的)Set的交集
     *
     * 注: 若不存在任何交集，那么返回空的集合(, 而不是null)
     * 注: 若其中一个key不存在(或两个key都不存在)，那么返回空的集合(, 而不是null)
     *
     * @param key
     *            定位其中一个set的键
     * @param otherKey
     *            定位其中另一个set的键
     *
     * @return  item交集
     * @since 2020/3/11 9:31:25
     */
    public static Set<Object> sIntersect(String key, String otherKey) {
        log.info("sIntersect(...) => key -> {}, otherKey -> {}", key, otherKey);
        Set<Object> intersectResult = redisTemplate.opsForSet().intersect(key, otherKey);
        log.info("sIntersect(...) => intersectResult -> {}", intersectResult);
        return intersectResult;
    }

    /**
     * 获取多个(key对应的)Set的交集
     *
     * 注: 若不存在任何交集，那么返回空的集合(, 而不是null)
     * 注: 若>=1个key不存在，那么返回空的集合(, 而不是null)
     *
     * @param key
     *            定位其中一个set的键
     * @param otherKeys
     *            定位其它set的键集
     *
     * @return  item交集
     * @since 2020/3/11 9:39:23
     */
    public static Set<Object> sIntersect(String key, Collection<String> otherKeys) {
        log.info("sIntersect(...) => key -> {}, otherKeys -> {}", key, otherKeys);
        Set<Object> intersectResult = redisTemplate.opsForSet().intersect(key, otherKeys);
        log.info("sIntersect(...) => intersectResult -> {}", intersectResult);
        return intersectResult;
    }

    /**
     * 获取两个(key对应的)Set的交集, 并将结果add到storeKey对应的Set中。
     *
     * case1: 交集不为空, storeKey不存在， 则 会创建对应的storeKey，并将交集添加到(storeKey对应的)set中
     * case2: 交集不为空, storeKey已存在， 则 会清除原(storeKey对应的)set中所有的项，然后将交集添加到(storeKey对应的)set中
     * case3: 交集为空, 则不进行下面的操作, 直接返回0
     *
     * 注: 求交集的部分，详见{@link SetOps#sIntersect(String, String)}
     *
     * @param key
     *            定位其中一个set的键
     * @param otherKey
     *            定位其中另一个set的键
     * @param storeKey
     *            定位(要把交集添加到哪个)set的key
     *
     * @return  add到(storeKey对应的)Set后, 该set对应的size
     * @since 2020/3/11 9:46:46
     */
    public static long sIntersectAndStore(String key, String otherKey, String storeKey) {
        log.info("sIntersectAndStore(...) => key -> {}, otherKey -> {}, storeKey -> {}",
                key, otherKey, storeKey);
        Long size = redisTemplate.opsForSet().intersectAndStore(key, otherKey, storeKey);
        log.info("sIntersectAndStore(...) => size -> {}", size);
        if (size == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return size;
    }

    /**
     * 获取多个(key对应的)Set的交集, 并将结果add到storeKey对应的Set中。
     *
     * case1: 交集不为空, storeKey不存在， 则 会创建对应的storeKey，并将交集添加到(storeKey对应的)set中
     * case2: 交集不为空, storeKey已存在， 则 会清除原(storeKey对应的)set中所有的项，然后将交集添加到(storeKey对应的)set中
     * case3: 交集为空, 则不进行下面的操作, 直接返回0
     *
     * 注: 求交集的部分，详见{@link SetOps#sIntersect(String, Collection)}
     *
     * @since 2020/3/11 11:04:29
     */
    public static long sIntersectAndStore(String key, Collection<String> otherKeys, String storeKey) {
        log.info("sIntersectAndStore(...) => key -> {}, otherKeys -> {}, storeKey -> {}", key, otherKeys, storeKey);
        Long size = redisTemplate.opsForSet().intersectAndStore(key, otherKeys, storeKey);
        log.info("sIntersectAndStore(...) => size -> {}", size);
        if (size == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return size;
    }

    /**
     * 获取两个(key对应的)Set的并集
     *
     * 注: 并集中的元素也是唯一的，这是Set保证的。
     *
     * @param key
     *            定位其中一个set的键
     * @param otherKey
     *            定位其中另一个set的键
     *
     * @return item并集
     * @since 2020/3/11 11:18:35
     */
    public static Set<Object> sUnion(String key, String otherKey) {
        log.info("sUnion(...) => key -> {}, otherKey -> {}", key, otherKey);
        Set<Object> unionResult = redisTemplate.opsForSet().union(key, otherKey);
        log.info("sUnion(...) => unionResult -> {}", unionResult);
        return unionResult;
    }

    /**
     * 获取两个(key对应的)Set的并集
     *
     * 注: 并集中的元素也是唯一的，这是Set保证的。
     *
     * @param key
     *            定位其中一个set的键
     * @param otherKeys
     *            定位其它set的键集
     *
     * @return item并集
     * @since 2020/3/11 11:18:35
     */
    public static Set<Object> sUnion(String key, Collection<String> otherKeys) {
        log.info("sUnion(...) => key -> {}, otherKeys -> {}", key, otherKeys);
        Set<Object> unionResult = redisTemplate.opsForSet().union(key, otherKeys);
        log.info("sUnion(...) => unionResult -> {}", unionResult);
        return unionResult;
    }

    /**
     * 获取两个(key对应的)Set的并集, 并将结果add到storeKey对应的Set中。
     *
     * case1: 并集不为空, storeKey不存在， 则 会创建对应的storeKey，并将并集添加到(storeKey对应的)set中
     * case2: 并集不为空, storeKey已存在， 则 会清除原(storeKey对应的)set中所有的项，然后将并集添加到(storeKey对应的)set中
     * case3: 并集为空, 则不进行下面的操作, 直接返回0
     *
     * 注: 求并集的部分，详见{@link SetOps#sUnion(String, String)}
     *
     * @param key
     *            定位其中一个set的键
     * @param otherKey
     *            定位其中另一个set的键
     * @param storeKey
     *            定位(要把并集添加到哪个)set的key
     *
     * @return  add到(storeKey对应的)Set后, 该set对应的size
     * @since 2020/3/11 12:26:24
     */
    public static long sUnionAndStore(String key, String otherKey, String storeKey) {
        log.info("sUnionAndStore(...) => key -> {}, otherKey -> {}, storeKey -> {}",
                key, otherKey, storeKey);
        Long size = redisTemplate.opsForSet().unionAndStore(key, otherKey, storeKey);
        log.info("sUnionAndStore(...) => size -> {}", size);
        if (size == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return size;
    }

    /**
     * 获取两个(key对应的)Set的并集, 并将结果add到storeKey对应的Set中。
     *
     * case1: 并集不为空, storeKey不存在， 则 会创建对应的storeKey，并将并集添加到(storeKey对应的)set中
     * case2: 并集不为空, storeKey已存在， 则 会清除原(storeKey对应的)set中所有的项，然后将并集添加到(storeKey对应的)set中
     * case3: 并集为空, 则不进行下面的操作, 直接返回0
     *
     * 注: 求并集的部分，详见{@link SetOps#sUnion(String, Collection)}
     *
     * @param key
     *            定位其中一个set的键
     * @param otherKeys
     *            定位其它set的键集
     * @param storeKey
     *            定位(要把并集添加到哪个)set的key
     *
     * @return  add到(storeKey对应的)Set后, 该set对应的size
     * @since 2020/3/11 12:26:24
     */
    public static long sUnionAndStore(String key, Collection<String> otherKeys, String storeKey) {
        log.info("sUnionAndStore(...) => key -> {}, otherKeys -> {}, storeKey -> {}",
                key, otherKeys, storeKey);
        Long size = redisTemplate.opsForSet().unionAndStore(key, otherKeys, storeKey);
        log.info("sUnionAndStore(...) => size -> {}", size);
        if (size == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return size;
    }

    /**
     * 获取 (key对应的)Set 减去 (otherKey对应的)Set 的差集
     *
     * 注: 如果被减数key不存在， 那么结果为空的集合(，而不是null)
     * 注: 如果被减数key存在，但减数key不存在， 那么结果即为(被减数key对应的)Set
     *
     * @param key
     *            定位"被减数set"的键
     * @param otherKey
     *            定位"减数set"的键
     *
     * @return item差集
     * @since 2020/3/11 14:03:57
     */
    public static Set<Object> sDifference(String key, String otherKey) {
        log.info("sDifference(...) => key -> {}, otherKey -> {}",
                key, otherKey);
        Set<Object> differenceResult = redisTemplate.opsForSet().difference(key, otherKey);
        log.info("sDifference(...) => differenceResult -> {}", differenceResult);
        return differenceResult;
    }

    /**
     * 获取 (key对应的)Set 减去 (otherKeys对应的)Sets 的差集
     *
     * 注: 如果被减数key不存在， 那么结果为空的集合(，而不是null)
     * 注: 如果被减数key存在，但减数key不存在， 那么结果即为(被减数key对应的)Set
     *
     * 提示: 当有多个减数时， 被减数先减去哪一个减数，后减去哪一个减数，是无所谓的，是不影响最终结果的。
     *
     * @param key
     *            定位"被减数set"的键
     * @param otherKeys
     *            定位"减数集sets"的键集
     *
     * @return item差集
     * @since 2020/3/11 14:03:57
     */
    public static Set<Object> sDifference(String key, Collection<String> otherKeys) {
        log.info("sDifference(...) => key -> {}, otherKeys -> {}", key, otherKeys);
        Set<Object> differenceResult = redisTemplate.opsForSet().difference(key, otherKeys);
        log.info("sDifference(...) => differenceResult -> {}", differenceResult);
        return differenceResult;
    }

    /**
     * 获取 (key对应的)Set 减去 (otherKey对应的)Set 的差集, 并将结果add到storeKey对应的Set中。
     *
     * case1: 差集不为空, storeKey不存在， 则 会创建对应的storeKey，并将差集添加到(storeKey对应的)set中
     * case2: 差集不为空, storeKey已存在， 则 会清除原(storeKey对应的)set中所有的项，然后将差集添加到(storeKey对应的)set中
     * case3: 差集为空, 则不进行下面的操作, 直接返回0
     *
     * 注: 求并集的部分，详见{@link SetOps#sDifference(String, String)}
     *
     * @param key
     *            定位"被减数set"的键
     * @param otherKey
     *            定位"减数set"的键
     * @param storeKey
     *            定位(要把差集添加到哪个)set的key
     *
     * @return  add到(storeKey对应的)Set后, 该set对应的size
     * @since 2020/3/11 14:33:36
     */
    public static long sDifferenceAndStore(String key, String otherKey, String storeKey) {
        log.info("sDifferenceAndStore(...) => key -> {}, otherKey -> {}, storeKey -> {}",
                key, otherKey, storeKey);
        Long size = redisTemplate.opsForSet().differenceAndStore(key, otherKey, storeKey);
        log.info("sDifferenceAndStore(...) => size -> {}", size);
        if (size == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return size;
    }

    /**
     * 获取 (key对应的)Set 减去 (otherKey对应的)Set 的差集, 并将结果add到storeKey对应的Set中。
     *
     * case1: 差集不为空, storeKey不存在， 则 会创建对应的storeKey，并将差集添加到(storeKey对应的)set中
     * case2: 差集不为空, storeKey已存在， 则 会清除原(storeKey对应的)set中所有的项，然后将差集添加到(storeKey对应的)set中
     * case3: 差集为空, 则不进行下面的操作, 直接返回0
     *
     * 注: 求并集的部分，详见{@link SetOps#sDifference(String, String)}
     *
     * @param key
     *            定位"被减数set"的键
     * @param otherKeys
     *            定位"减数集sets"的键集
     * @param storeKey
     *            定位(要把差集添加到哪个)set的key
     *
     * @return  add到(storeKey对应的)Set后, 该set对应的size
     * @since 2020/3/11 14:33:36
     */
    public static long sDifferenceAndStore(String key, Collection<String> otherKeys, String storeKey) {
        log.info("sDifferenceAndStore(...) => key -> {}, otherKeys -> {}, storeKey -> {}",
                key, otherKeys, storeKey);
        Long size = redisTemplate.opsForSet().differenceAndStore(key, otherKeys, storeKey);
        log.info("sDifferenceAndStore(...) => size -> {}", size);
        if (size == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return size;
    }

    /**
     * 获取key对应的set
     *
     * 注: 若key不存在, 则返回的是空的set(, 而不是null)
     *
     * @param key
     *            定位set的key
     * @return  (key对应的)set
     * @since 2020/3/11 14:49:39
     */
    public static Set<Object> sMembers(String key) {
        log.info("sMembers(...) => key -> {}", key);
        Set<Object> members = redisTemplate.opsForSet().members(key);
        log.info("sMembers(...) => members -> {}", members);
        return members;
    }

    /**
     * 从key对应的set中随机获取一项
     *
     * @param key
     *            定位set的key
     * @return  随机获取到的项
     * @since 2020/3/11 14:54:58
     */
    public static Object sRandomMember(String key) {
        log.info("sRandomMember(...) => key -> {}", key);
        Object randomItem = redisTemplate.opsForSet().randomMember(key);
        log.info("sRandomMember(...) => randomItem -> {}", randomItem);
        return randomItem;
    }

    /**
     * 从key对应的set中获取count次随机项(, set中的同一个项可能被多次获取)
     *
     * 注: count可大于set的size。
     * 注: 取出来的结果里可能存在相同的值。
     *
     * @param key
     *            定位set的key
     * @param count
     *            要取多少项
     *
     * @return  随机获取到的项集
     * @since 2020/3/11 14:54:58
     */
    public static List<Object> sRandomMembers(String key, long count) {
        log.info("sRandomMembers(...) => key -> {}, count -> {}", key, count);
        List<Object> randomItems = redisTemplate.opsForSet().randomMembers(key, count);
        log.info("sRandomMembers(...) => randomItems -> {}", randomItems);
        return randomItems;
    }

    /**
     * 从key对应的set中随机获取count个项
     *
     * 注: 若count >= set的size, 那么返回的即为这个key对应的set。
     * 注: 取出来的结果里没有重复的项。
     *
     * @param key
     *            定位set的key
     * @param count
     *            要取多少项
     *
     * @return  随机获取到的项集
     * @since 2020/3/11 14:54:58
     */
    public static Set<Object> sDistinctRandomMembers(String key, long count) {
        log.info("sDistinctRandomMembers(...) => key -> {}, count -> {}", key, count);
        Set<Object> distinctRandomItems = redisTemplate.opsForSet().distinctRandomMembers(key, count);
        log.info("sDistinctRandomMembers(...) => distinctRandomItems -> {}", distinctRandomItems);
        return distinctRandomItems;
    }

    /**
     * 根据options匹配到(key对应的)set中的对应的item, 并返回对应的item集
     *
     *
     * 注: ScanOptions实例的创建方式举例:
     *     1、ScanOptions.NONE
     *     2、ScanOptions.scanOptions().match("n??e").build()
     *
     * @param key
     *            定位set的key
     * @param options
     *            匹配set中的item的条件
     *            注: ScanOptions.NONE表示全部匹配。
     *            注: ScanOptions.scanOptions().match(pattern).build()表示按照pattern匹配,
     *                其中pattern中可以使用通配符 * ? 等,
     *                * 表示>=0个字符
     *                ？ 表示有且只有一个字符
     *                此处的匹配规则与{@link KeyOps#keys(String)}处的一样。
     *
     * @return  匹配到的(key对应的)set中的项
     * @since 2020/3/9 10:49:27
     */
    public static Cursor<Object> sScan(String key, ScanOptions options) {
        log.info("sScan(...) => key -> {}, options -> {}", key, JsonUtils.toJsonStr(options));
        Cursor<Object> cursor = redisTemplate.opsForSet().scan(key, options);
        log.info("sScan(...) => cursor -> {}", JsonUtils.toJsonStr(cursor));
        return cursor;
    }
}
