package com.envision.bunny.infrastructure.util.redis;

import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.envision.bunny.infrastructure.util.ApplicationContextUtils;
import com.envision.bunny.infrastructure.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

/**
 * ZSet相关操作
 *
 * 特别说明: ZSet是有序的,
 *             不仅体现在： redis中的存储上有序。
 *             还体现在:   此工具类ZSetOps中返回值类型为Set<?>的方法, 实际返回类型是LinkedHashSet<?>
 *
 * 提示: redis中的ZSet, 一定程度等于redis中的Set + redis中的Hash的结合体。
 * 提示: redis中String的数据结构可参考resources/data-structure/ZSet(有序集合)的数据结构(示例一).png
 *      redis中String的数据结构可参考resources/data-structure/ZSet(有序集合)的数据结构(示例二).png
 * 提示: ZSet中的entryKey即为成员项， entryValue即为这个成员项的分值, ZSet根据成员的分值，来堆成员进行排序。
 *
 * @author jingjing.dong
 * @since 2021/3/29-15:42
 */
@Slf4j
@Component("zSetOps")
@SuppressWarnings("unused")
public class ZSetOps {
    private static RedisTemplate<String,Object> redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String,Object> redisTemplate){
        ZSetOps.redisTemplate = redisTemplate;
    }

    /**
     * 向(key对应的)zset中添加(item, score)
     *
     * 注: item为entryKey成员项， score为entryValue分数值。
     *
     * 注: 若(key对应的)zset中已存在(与此次要添加的项)相同的item项，那么此次添加操作会失败，返回false；
     *     但是！！！ zset中原item的score会被更新为此次add的相同item项的score。
     *     所以, 也可以通过zAdd达到更新item对应score的目的。
     *
     * 注: score可为正、可为负、可为0; 总之, double范围内都可以。
     *
     * 注: 若score的值一样，则按照item排序。
     *
     * @param key
     *            定位set的key
     * @param item
     *            要往(key对应的)zset中添加的成员项
     * @param score
     *            item的分值
     *
     * @return 是否添加成功
     * @since 2020/3/11 15:35:30
     */
    public static boolean zAdd(String key, Object item, double score) {
        log.info("zAdd(...) => key -> {}, item -> {}, score -> {}", key, item, score);
        Boolean result = redisTemplate.opsForZSet().add(key, item, score);
        log.info("zAdd(...) => result -> {}", result);
        if (result == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return result;
    }

    /**
     * 批量添加entry<item, score>
     *
     * 注: 若entry<item, score>集中存在item相同的项(, score不一样)，那么redis在执行真正的批量add操作前,会
     *     将其中一个item过滤掉。
     * 注: 同样的，若(key对应的)zset中已存在(与此次要添加的项)相同的item项，那么此次批量添加操作中，
     *    对该item项的添加会失败，会失败，成功计数器不会加1；但是！！！ zset中原item的score会被更新为此
     *    次add的相同item项的score。所以, 也可以通过zAdd达到更新item对应score的目的。
     *
     * @param key
     *            定位set的key
     * @param entries
     *            要添加的entry<item, score>集
     *
     * @return 本次添加进(key对应的)zset中的entry的个数
     * @since 2020/3/11 16:45:45
     */
    public static long zAdd(String key, Set<TypedTuple<Object>> entries) {
        log.info("zAdd(...) => key -> {}, entries -> {}", key, JsonUtils.toJsonStr(entries));
        Long count = redisTemplate.opsForZSet().add(key, entries);
        log.info("zAdd(...) => count -> {}", count);
        if (count == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return count;
    }

    /**
     * 从(key对应的)zset中移除项
     *
     * 注:若key不存在，则返回0
     *
     * @param key
     *            定位set的key
     * @param items
     *            要移除的项集
     *
     * @return  实际移除了的项的个数
     * @since 2020/3/11 17:20:12
     */
    public static long zRemove(String key, Object... items) {
        log.info("zRemove(...) => key -> {}, items -> {}", key, items);
        Long count = redisTemplate.opsForZSet().remove(key, items);
        log.info("zRemove(...) => count -> {}", count);
        if (count == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return count;
    }

    /**
     * 移除(key对应的)zset中, 排名范围在[startIndex, endIndex]内的item
     *
     * 注:默认的，按score.item升序排名， 排名从0开始
     *
     * 注: 类似于List中的索引, 排名可以分为多个方式:
     *     从前到后(正向)的排名: 0、1、2...
     *     从后到前(反向)的排名: -1、-2、-3...
     *
     * 注: 不论是使用正向排名，还是使用反向排名, 使用此方法时, 应保证 startRange代表的元素的位置
     *     在endRange代表的元素的位置的前面， 如:
     *      示例一: ZSetOps.zRemoveRange("name", 0, 2);
     *      示例二: ZSetOps.zRemoveRange("site", -2, -1);
     *      示例三: ZSetOps.zRemoveRange("foo", 0, -1);
     *
     * 注:若key不存在，则返回0
     *
     * @param key
     *            定位set的key
     * @param startRange
     *            开始项的排名
     * @param endRange
     *            结尾项的排名
     *
     * @return  实际移除了的项的个数
     * @since 2020/3/11 17:20:12
     */
    public static long zRemoveRange(String key, long startRange, long endRange) {
        log.info("zRemoveRange(...) => key -> {}, startRange -> {}, endRange -> {}",
                key, startRange, endRange);
        Long count = redisTemplate.opsForZSet().removeRange(key, startRange, endRange);
        log.info("zRemoveRange(...) => count -> {}", count);
        if (count == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return count;
    }

    /**
     * 移除(key对应的)zset中, score范围在[minScore, maxScore]内的item
     *
     * 提示: 虽然删除范围包含两侧的端点(即:包含minScore和maxScore), 但是由于double存在精度问题，所以建议:
     *          设置值时，minScore应该设置得比要删除的项里，最小的score还小一点
     *                   maxScore应该设置得比要删除的项里，最大的score还大一点
     *          追注: 本人简单测试了几组数据，暂未出现精度问题。
     *
     * 注:若key不存在，则返回0
     *
     * @param key
     *            定位set的key
     * @param minScore
     *            score下限(含这个值)
     * @param maxScore
     *            score上限(含这个值)
     *
     * @return  实际移除了的项的个数
     * @since 2020/3/11 17:20:12
     */
    public static long zRemoveRangeByScore(String key, double minScore, double maxScore) {
        log.info("zRemoveRangeByScore(...) => key -> {}, startIndex -> {}, startIndex -> {}",
                key, minScore, maxScore);
        Long count = redisTemplate.opsForZSet().removeRangeByScore(key, minScore, maxScore);
        log.info("zRemoveRangeByScore(...) => count -> {}", count);
        if (count == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return count;
    }

    /**
     * 增/减 (key对应的zset中,)item的分数值
     *
     * @param key
     *            定位zset的key
     * @param item
     *            项
     * @param delta
     *            变化量(正 - 增, 负 - 减)
     * @return 修改后的score值
     * @since 2020/3/12 8:55:38
     */
    public static double zIncrementScore(String key, Object item, double delta) {
        log.info("zIncrementScore(...) => key -> {}, item -> {}, delta -> {}", key, item, delta);
        Double scoreValue = redisTemplate.opsForZSet().incrementScore(key, item, delta);
        log.info("zIncrementScore(...) => scoreValue -> {}", scoreValue);
        if (scoreValue == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return scoreValue;
    }

    /**
     * 返回item在(key对应的)zset中的(按score从小到大的)排名
     *
     * 注: 排名从0开始。 即意味着，此方法等价于: 返回item在(key对应的)zset中的位置索引。
     * 注: 若key或item不存在， 返回null。
     * 注: 排序规则是score,item, 即:优先以score排序，若score相同，则再按item排序。
     *
     * @param key
     *            定位zset的key
     * @param item
     *            项
     *
     * @return 排名(等价于: 索引)
     * @since 2020/3/12 9:14:09
     */
    public static long zRank(String key, Object item) {
        log.info("zRank(...) => key -> {}, item -> {}", key, item);
        Long rank = redisTemplate.opsForZSet().rank(key, item);
        log.info("zRank(...) => rank -> {}", rank);
        if (rank == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return rank;
    }

    /**
     * 返回item在(key对应的)zset中的(按score从大到小的)排名
     *
     * 注: 排名从0开始。补充: 因为是按score从大到小排序的, 所以最大score对应的item的排名为0。
     * 注: 若key或item不存在， 返回null。
     * 注: 排序规则是score,item, 即:优先以score排序，若score相同，则再按item排序。
     *
     * @param key
     *            定位zset的key
     * @param item
     *            项
     *
     * @return 排名(等价于: 索引)
     * @since 2020/3/12 9:14:09
     */
    public static long zReverseRank(String key, Object item) {
        log.info("zReverseRank(...) => key -> {}, item -> {}", key, item);
        Long reverseRank = redisTemplate.opsForZSet().reverseRank(key, item);
        log.info("zReverseRank(...) => reverseRank -> {}", reverseRank);
        if (reverseRank == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return reverseRank;
    }

    /**
     * 根据索引位置， 获取(key对应的)zset中排名处于[start, end]中的item项集
     *
     * 注: 不论是使用正向排名，还是使用反向排名, 使用此方法时, 应保证 startIndex代表的元素的
     *      位置在endIndex代表的元素的位置的前面， 如:
     *      示例一: ZSetOps.zRange("name", 0, 2);
     *      示例二: ZSetOps.zRange("site", -2, -1);
     *      示例三: ZSetOps.zRange("foo", 0, -1);
     *
     * 注: 若key不存在, 则返回空的集合。
     *
     * 注: 当[start, end]的范围比实际zset的范围大时, 返回范围上"交集"对应的项集合。
     *
     * @param key
     *            定位zset的key
     * @param start
     *            排名开始位置
     * @param end
     *            排名结束位置
     *
     * @return  对应的item项集
     * @since 2020/3/12 9:50:40
     */
    public static Set<Object> zRange(String key, long start, long end) {
        log.info("zRange(...) => key -> {}, start -> {}, end -> {}", key, start, end);
        Set<Object> result = redisTemplate.opsForZSet().range(key, start, end);
        log.info("zRange(...) => result -> {}", result);
        return result;
    }

    /**
     * 获取(key对应的)zset中的所有item项
     *
     * @see ZSetOps#zRange(String, long, long)
     *
     * @param key
     *            定位zset的键
     *
     * @return  (key对应的)zset中的所有item项
     * @since 2020/3/12 10:02:07
     */
    public static Set<Object> zWholeZSetItem(String key) {
        log.info("zWholeZSetItem(...) => key -> {}", key);
        Set<Object> result = redisTemplate.opsForZSet().range(key, 0, -1);
        log.info("zWholeZSetItem(...) =>result -> {}", result);
        return result;
    }

    /**
     * 根据索引位置， 获取(key对应的)zset中排名处于[start, end]中的entry集
     *
     * 注: 不论是使用正向排名，还是使用反向排名, 使用此方法时, 应保证 startIndex代表的元素的
     *      位置在endIndex代表的元素的位置的前面， 如:
     *      示例一: ZSetOps.zRange("name", 0, 2);
     *      示例二: ZSetOps.zRange("site", -2, -1);
     *      示例三: ZSetOps.zRange("foo", 0, -1);
     *
     * 注: 若key不存在, 则返回空的集合。
     *
     * 注: 当[start, end]的范围比实际zset的范围大时, 返回范围上"交集"对应的项集合。
     *
     * 注: 此方法和{@link ZSetOps#zRange(String, long, long)}类似，不过此方法返回的不是item集， 而是entry集
     *
     * @param key
     *            定位zset的key
     * @param start
     *            排名开始位置
     * @param end
     *            排名结束位置
     *
     * @return  对应的entry集
     * @since 2020/3/12 9:50:40
     */
    public static Set<TypedTuple<Object>> zRangeWithScores(String key, long start, long end) {
        log.info("zRangeWithScores(...) => key -> {}, start -> {}, end -> {}", key, start, end);
        Set<TypedTuple<Object>> entries = redisTemplate.opsForZSet().rangeWithScores(key, start, end);
        log.info("zRangeWithScores(...) => entries -> {}", JsonUtils.toJsonStr(entries));
        return entries;
    }

    /**
     * 获取(key对应的)zset中的所有entry
     *
     * @see ZSetOps#zRangeWithScores(String, long, long)
     *
     * @param key
     *            定位zset的键
     *
     * @return  (key对应的)zset中的所有entry
     * @since 2020/3/12 10:02:07
     */
    public static Set<TypedTuple<Object>> zWholeZSetEntry(String key) {
        log.info("zWholeZSetEntry(...) => key -> {}", key);
        Set<TypedTuple<Object>> entries = redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
        log.info("zWholeZSetEntry(...) => entries -> {}", JsonUtils.toJsonStr(entries));
        return entries;
    }

    /**
     * 根据score， 获取(key对应的)zset中分数值处于[minScore, maxScore]中的item项集
     *
     * 注: 若key不存在, 则返回空的集合。
     * 注: 当[minScore, maxScore]的范围比实际zset中score的范围大时, 返回范围上"交集"对应的项集合。
     *
     * 提示: 虽然删除范围包含两侧的端点(即:包含minScore和maxScore), 但是由于double存在精度问题，所以建议:
     *          设置值时，minScore应该设置得比要删除的项里，最小的score还小一点
     *                   maxScore应该设置得比要删除的项里，最大的score还大一点
     *          追注: 本人简单测试了几组数据，暂未出现精度问题。
     *
     * @param key
     *            定位zset的key
     * @param minScore
     *            score下限
     * @param maxScore
     *            score上限
     *
     * @return  对应的item项集
     * @since 2020/3/12 9:50:40
     */
    public static Set<Object> zRangeByScore(String key, double minScore, double maxScore) {
        log.info("zRangeByScore(...) => key -> {}, minScore -> {}, maxScore -> {}", key, minScore, maxScore);
        Set<Object> items = redisTemplate.opsForZSet().rangeByScore(key, minScore, maxScore);
        log.info("zRangeByScore(...) => items -> {}", items);
        return items;
    }

    /**
     * 根据score， 获取(key对应的)zset中分数值处于[minScore, maxScore]中的, score处于[minScore,
     * 排名大于等于offset的count个item项
     *
     * 特别注意: 对于不是特别熟悉redis的人来说, offset 和 count最好都使用正数， 避免引起理解上的歧义。
     *
     * 注: 若key不存在, 则返回空的集合。
     *
     * 提示: 虽然删除范围包含两侧的端点(即:包含minScore和maxScore), 但是由于double存在精度问题，所以建议:
     *          设置值时，minScore应该设置得比要删除的项里，最小的score还小一点
     *                   maxScore应该设置得比要删除的项里，最大的score还大一点
     *          追注: 本人简单测试了几组数据，暂未出现精度问题。
     *
     * @param key
     *            定位zset的key
     * @param minScore
     *            score下限
     * @param maxScore
     *            score上限
     * @param offset
     *            偏移量(即:排名下限)
     * @param count
     *            期望获取到的元素个数
     *
     * @return  对应的item项集
     * @since 2020/3/12 9:50:40
     */
    public static Set<Object> zRangeByScore(String key, double minScore, double maxScore,
                                            long offset, long count) {
        log.info("zRangeByScore(...) => key -> {}, minScore -> {}, maxScore -> {}, offset -> {}, "
                + "count -> {}", key, minScore, maxScore, offset, count);
        Set<Object> items = redisTemplate.opsForZSet().rangeByScore(key, minScore, maxScore, offset, count);
        log.info("zRangeByScore(...) => items -> {}", items);
        return items;
    }

    /**
     * 获取(key对应的)zset中的所有score处于[minScore, maxScore]中的entry
     *
     * @see ZSetOps#zRangeByScore(String, double, double)
     *
     * 注: 若key不存在, 则返回空的集合。
     * 注: 当[minScore, maxScore]的范围比实际zset中score的范围大时, 返回范围上"交集"对应的项集合。
     *
     * @param key
     *            定位zset的键
     * @param minScore
     *            score下限
     * @param maxScore
     *            score上限
     *
     * @return  (key对应的)zset中的所有score处于[minScore, maxScore]中的entry
     * @since 2020/3/12 10:02:07
     */
    public static Set<TypedTuple<Object>> zRangeByScoreWithScores(String key, double minScore, double maxScore) {
        log.info("zRangeByScoreWithScores(...) => key -> {}, minScore -> {}, maxScore -> {}",
                key, minScore, maxScore);
        Set<TypedTuple<Object>> entries = redisTemplate.opsForZSet().rangeByScoreWithScores(key, minScore, maxScore);
        log.info("zRangeByScoreWithScores(...) => entries -> {}", JsonUtils.toJsonStr(entries));
        return entries;
    }

    /**
     * 获取(key对应的)zset中, score处于[minScore, maxScore]里的、排名大于等于offset的count个entry
     *
     * 特别注意: 对于不是特别熟悉redis的人来说, offset 和 count最好都使用正数， 避免引起理解上的歧义。
     *
     * @param key
     *            定位zset的键
     * @param minScore
     *            score下限
     * @param maxScore
     *            score上限
     * @param offset
     *            偏移量(即:排名下限)
     * @param count
     *            期望获取到的元素个数
     *
     * @return [startIndex, endIndex] & [minScore, maxScore]里的entry
     * @since 2020/3/12 11:09:06
     */
    public static Set<TypedTuple<Object>> zRangeByScoreWithScores(String key, double minScore,
                                                                  double maxScore, long offset,
                                                                  long count) {
        log.info("zRangeByScoreWithScores(...) => key -> {}, minScore -> {}, maxScore -> {},"
                        + " offset -> {}, count -> {}",
                key, minScore, maxScore, offset, count);
        Set<TypedTuple<Object>> entries = redisTemplate.opsForZSet().rangeByScoreWithScores(key, minScore,
                maxScore, offset, count);
        log.info("zRangeByScoreWithScores(...) => entries -> {}", JsonUtils.toJsonStr(entries));
        return entries;
    }


    /**
     * 获取时, 先按score倒序, 然后根据索引位置， 获取(key对应的)zset中排名处于[start, end]中的item项集
     *
     * @see ZSetOps#zRange(String, long, long)。 只是zReverseRange这里会提前多一个倒序。
     */
    public static Set<Object> zReverseRange(String key, long start, long end) {
        log.info("zReverseRange(...) => key -> {}, start -> {}, end -> {}", key, start, end);
        Set<Object> entries = redisTemplate.opsForZSet().reverseRange(key, start, end);
        log.info("zReverseRange(...) => entries -> {}", entries);
        return entries;
    }

    /**
     * 获取时, 先按score倒序, 然后根据索引位置， 获取(key对应的)zset中排名处于[start, end]中的entry集
     *
     * @see ZSetOps#zRangeWithScores(String, long, long)。 只是zReverseRangeWithScores这里会提前多一个倒序。
     */
    public static Set<TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end) {
        log.info("zReverseRangeWithScores(...) => key -> {}, start -> {}, end -> {}", key, start, end);
        Set<TypedTuple<Object>> entries = redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        log.info("zReverseRangeWithScores(...) => entries -> {}", JsonUtils.toJsonStr(entries));
        return entries;
    }

    /**
     * 获取时, 先按score倒序, 然后根据score， 获取(key对应的)zset中分数值处于[minScore, maxScore]中的item项集
     *
     * @see ZSetOps#zRangeByScore(String, double, double)。 只是zReverseRangeByScore这里会提前多一个倒序。
     */
    public static Set<Object> zReverseRangeByScore(String key, double minScore, double maxScore) {
        log.info("zReverseRangeByScore(...) => key -> {}, minScore -> {}, maxScore -> {}",
                key, minScore, maxScore);
        Set<Object> items = redisTemplate.opsForZSet().reverseRangeByScore(key, minScore, maxScore);
        log.info("zReverseRangeByScore(...) => items -> {}", items);
        return items;
    }

    /**
     * 获取时, 先按score倒序, 然后获取(key对应的)zset中的所有score处于[minScore, maxScore]中的entry
     *
     * @see ZSetOps#zRangeByScoreWithScores(String, double, double)。 只是zReverseRangeByScoreWithScores这里会提前多一个倒序。
     */
    public static Set<TypedTuple<Object>> zReverseRangeByScoreWithScores(String key, double minScore, double maxScore) {
        log.info("zReverseRangeByScoreWithScores(...) => key -> {}, minScore -> {}, maxScore -> {}",
                key, minScore, maxScore);
        Set<TypedTuple<Object>> entries = redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key,
                minScore, maxScore);
        log.info("zReverseRangeByScoreWithScores(...) => entries -> {}", JsonUtils.toJsonStr(entries));
        return entries;
    }

    /**
     * 获取时, 先按score倒序, 然后根据score， 获取(key对应的)zset中分数值处于[minScore, maxScore]中的,
     * score处于[minScore,排名大于等于offset的count个item项
     *
     * @see ZSetOps#zRangeByScore(String, double, double, long, long)。 只是zReverseRangeByScore这里会提前多一个倒序。
     */
    public static Set<Object> zReverseRangeByScore(String key, double minScore, double maxScore, long offset, long count) {
        log.info("zReverseRangeByScore(...) => key -> {}, minScore -> {}, maxScore -> {}, offset -> {}, "
                + "count -> {}", key, minScore, maxScore, offset, count);
        Set<Object> items = redisTemplate.opsForZSet().reverseRangeByScore(key, minScore, maxScore, offset, count);
        log.info("items -> {}", items);
        return items;
    }

    /**
     * 统计(key对应的zset中)score处于[minScore, maxScore]中的item的个数
     *
     * @param key
     *            定位zset的key
     * @param minScore
     *            score下限
     * @param maxScore
     *            score上限
     *
     * @return  [minScore, maxScore]中item的个数
     * @since 2020/3/13 12:20:43
     */
    public static long zCount(String key, double minScore, double maxScore) {
        log.info("zCount(...) => key -> {}, minScore -> {}, maxScore -> {}",key, minScore, maxScore);
        Long count = redisTemplate.opsForZSet().count(key, minScore, maxScore);
        log.info("zCount(...) => count -> {}", count);
        if (count == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return count;
    }

    /**
     * 统计(key对应的)zset中item的个数
     *
     * 注: 此方法等价于{@link ZSetOps#zZCard(String)}
     *
     * @param key
     *            定位zset的key
     *
     * @return  zset中item的个数
     * @since 2020/3/13 12:20:43
     */
    public static long zSize(String key) {
        log.info("zSize(...) => key -> {}", key);
        Long size = redisTemplate.opsForZSet().size(key);
        log.info("zSize(...) => size -> {}", size);
        if (size == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return size;
    }

    /**
     * 统计(key对应的)zset中item的个数
     *
     * 注: 此方法等价于{@link ZSetOps#zSize(String)}
     *
     * @param key
     *            定位zset的key
     *
     * @return  zset中item的个数
     * @since 2020/3/13 12:20:43
     */
    public static long zZCard(String key) {
        log.info("zZCard(...) => key -> {}", key);
        Long size = redisTemplate.opsForZSet().zCard(key);
        log.info("zZCard(...) => size -> {}", size);
        if (size == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return size;
    }

    /**
     * 统计(key对应的)zset中指定item的score
     *
     * @param key
     *            定位zset的key
     * @param item
     *            zset中的item
     *
     * @return  item的score
     * @since 2020/3/13 14:51:43
     */
    public static double zScore(String key, Object item) {
        log.info("zScore(...) => key -> {}, item -> {}", key, item);
        Double score = redisTemplate.opsForZSet().score(key, item);
        log.info("zScore(...) => score -> {}", score);
        if (score == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return score;
    }

    /**
     * 获取两个(key对应的)ZSet的并集, 并将结果add到storeKey对应的ZSet中。
     *
     * 注: 和set一样，zset中item是唯一的， 在多个zset进行Union时, 处理相同的item时， score的值会变为对应的score之和，如：
     *         ZSetOps.zAdd("name1", "a", 1);和ZSetOps.zAdd("name2", "a", 2);
     *         对(name1和name2对应的)zset进行zUnionAndStore之后，新的zset中的项a,对应的score值为3
     *
     * case1: 交集不为空, storeKey不存在， 则 会创建对应的storeKey，并将并集添加到(storeKey对应的)ZSet中
     * case2: 交集不为空, storeKey已存在， 则 会清除原(storeKey对应的)ZSet中所有的项，然后将并集添加到(storeKey对应的)ZSet中
     * case3: 交集为空, 则不进行下面的操作, 直接返回0
     *
     * @param key
     *            定位其中一个zset的键
     * @param otherKey
     *            定位另外的zset的键
     * @param storeKey
     *            定位(要把交集添加到哪个)set的key
     *
     * @return  add到(storeKey对应的)ZSet后, 该ZSet对应的size
     * @since 2020/3/11 12:26:24
     */
    public static long zUnionAndStore(String key, String otherKey, String storeKey) {
        log.info("zUnionAndStore(...) => key -> {}, otherKey -> {}, storeKey -> {}", key, otherKey, storeKey);
        Long size = redisTemplate.opsForZSet().unionAndStore(key, otherKey, storeKey);
        log.info("zUnionAndStore(...) => size -> {}", size);
        if (size == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return size;
    }

    /**
     * 获取两个(key对应的)ZSet的并集, 并将结果add到storeKey对应的ZSet中。
     *
     * 注: 和set一样，zset中item是唯一的， 在多个zset进行Union时, 处理相同的item时， score的值会变为对应的score之和，如：
     *         ZSetOps.zAdd("name1", "a", 1);和ZSetOps.zAdd("name2", "a", 2);
     *         对(name1和name2对应的)zset进行zUnionAndStore之后，新的zset中的项a,对应的score值为3
     *
     * case1: 并集不为空, storeKey不存在， 则 会创建对应的storeKey，并将并集添加到(storeKey对应的)ZSet中
     * case2: 并集不为空, storeKey已存在， 则 会清除原(storeKey对应的)ZSet中所有的项，然后将并集添加到(storeKey对应的)ZSet中
     * case3: 并集为空, 则不进行下面的操作, 直接返回0
     *
     * @param key
     *            定位其中一个set的键
     * @param otherKeys
     *            定位其它set的键集
     * @param storeKey
     *            定位(要把并集添加到哪个)set的key
     *
     * @return  add到(storeKey对应的)ZSet后, 该ZSet对应的size
     * @since 2020/3/11 12:26:24
     */
    public static long zUnionAndStore(String key, Collection<String> otherKeys, String storeKey) {
        log.info("zUnionAndStore(...) => key -> {}, otherKeys -> {}, storeKey -> {}", key, otherKeys, storeKey);
        Long size = redisTemplate.opsForZSet().unionAndStore(key, otherKeys, storeKey);
        log.info("zUnionAndStore(...) => size -> {}", size);
        if (size == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return size;
    }

    /**
     * 获取两个(key对应的)ZSet的交集, 并将结果add到storeKey对应的ZSet中。
     *
     * 注: 和set一样，zset中item是唯一的， 在多个zset进行Intersect时, 处理相同的item时， score的值会变为对应的score之和，如：
     *         ZSetOps.zAdd("name1", "a", 1);
     *         ZSetOps.zAdd("name1", "b", 100);
     *         和R
     *         ZSetOps.zAdd("name2", "a", 2);
     *         ZSetOps.zAdd("name2", "c", 200);
     *         对(name1和name2对应的)zset进行zIntersectAndStore之后，新的zset中的项a,对应的score值为3
     *
     * case1: 交集不为空, storeKey不存在， 则 会创建对应的storeKey，并将交集添加到(storeKey对应的)ZSet中
     * case2: 交集不为空, storeKey已存在， 则 会清除原(storeKey对应的)ZSet中所有的项，然后将交集添加到(storeKey对应的)ZSet中
     * case3: 交集为空, 则不进行下面的操作, 直接返回0
     *
     * @param key
     *            定位其中一个ZSet的键
     * @param otherKey
     *            定位其中另一个ZSet的键
     * @param storeKey
     *            定位(要把交集添加到哪个)ZSet的key
     *
     * @return  add到(storeKey对应的)ZSet后, 该ZSet对应的size
     * @since 2020/3/11 9:46:46
     */
    public static long zIntersectAndStore(String key, String otherKey, String storeKey) {
        log.info("zIntersectAndStore(...) => key -> {}, otherKey -> {}, storeKey -> {}", key, otherKey, storeKey);
        Long size = redisTemplate.opsForZSet().intersectAndStore(key, otherKey, storeKey);
        log.info("zIntersectAndStore(...) => size -> {}", size);
        if (size == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return size;
    }

    /**
     * 获取多个(key对应的)ZSet的交集, 并将结果add到storeKey对应的ZSet中。
     *
     * case1: 交集不为空, storeKey不存在， 则 会创建对应的storeKey，并将交集添加到(storeKey对应的)ZSet中
     * case2: 交集不为空, storeKey已存在， 则 会清除原(storeKey对应的)ZSet中所有的项，然后将交集添加到(storeKey对应的)ZSet中
     * case3: 交集为空, 则不进行下面的操作, 直接返回0
     *
     * @param key
     *            定位其中一个set的键
     * @param otherKeys
     *            定位其它set的键集
     * @param storeKey
     *            定位(要把并集添加到哪个)set的key
     *
     * @return  add到(storeKey对应的)ZSet后, 该ZSet对应的size
     * @since 2020/3/11 11:04:29
     */
    public static long zIntersectAndStore(String key, Collection<String> otherKeys, String storeKey) {
        log.info("zIntersectAndStore(...) => key -> {}, otherKeys -> {}, storeKey -> {}",
                key, otherKeys, storeKey);
        Long size = redisTemplate.opsForZSet().intersectAndStore(key, otherKeys, storeKey);
        log.info("zIntersectAndStore(...) => size -> {}", size);
        if (size == null) {
            throw new BizException(ErrorCode.REDIS_OPS_RESULT_IS_NULL);
        }
        return size;
    }
}
