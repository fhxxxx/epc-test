package com.envision.epc.infrastructure.mybatis;

import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.*;

import java.util.List;
import java.util.function.Function;

/**
 * 基础分页结果
 *
 * @author jingjing.dong
 * @since 2021/5/25-10:21
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BasicPagination<T> {
    /**
     * 总数
     *
     * @mock 155
     */
    long totalCount;
    /**
     * 详情
     */
    List<T> items;

    public static <T> BasicPagination<T> of(IPage<T> page) {
        return BasicPagination.<T>builder().items(page.getRecords()).totalCount(page.getTotal()).build();
    }

    public static <T, R> BasicPagination<R> of(IPage<T> page, Function<T, R> function) {
        // List<R> result = page.getRecords().stream().map(function).collect(Collectors.toList());
        IPage<R> newPage = page.convert(function);
        return BasicPagination.of(newPage);
    }

    public static <T> BasicPagination<T> empty() {
        return BasicPagination.<T>builder().items(ListUtil.empty()).totalCount(0L).build();
    }
}