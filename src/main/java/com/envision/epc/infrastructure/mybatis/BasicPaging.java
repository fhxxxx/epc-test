package com.envision.epc.infrastructure.mybatis;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import com.envision.epc.infrastructure.crud.ApprenticeUtil;
import com.envision.epc.infrastructure.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基础分页请求
 * @author jingjing.dong
 * @since 2021/5/25-10:21
 */
@Setter
@Getter
@ToString
public class BasicPaging {
    /**
     * 分页大小
     * @mock 15
     */

    int pageSize = 15;
    /**
     * 页标
     * @mock 1
     */
    int pageNum = 1;

    /**
     * 排序
     */
    private String orderBy;

    public boolean needSort() {
        return CharSequenceUtil.isNotBlank(orderBy);
    }
    public List<Pair<String,String>> getAllOrder() {
        if (CharSequenceUtil.isEmpty(orderBy)) {
            return ListUtil.empty();
        }
        final List<String> orderPairStrList = CharSequenceUtil.split(orderBy, CharPool.COMMA);
        return orderPairStrList.stream().filter(StrUtil::isNotEmpty).filter(str -> CharSequenceUtil.endWithAny(str, "+", "-"))
                .map(str -> new Pair<>(str.substring(0, str.length() - 1), str.substring(str.length() - 1))).collect(Collectors.toList());
    }

    /**
     *  配合lambda query的Last SQL使用
     *  默认必须使用id做一个排序
     * @param clz 查询的clz
     * @return order by 语句
     */
    public String getAllOrderSql(Class<?> clz) {
        final List<Pair<String, String>> allOrderPair = getAllOrder();
        if (CollUtil.isEmpty(allOrderPair)){
            return " order by id asc";
        }
        return " order by " + allOrderPair.stream().map(pair -> {
            String tableField = ApprenticeUtil.getBbField(pair.getKey(), clz);
            if (StrUtil.equals(pair.getValue(), "+")) {
                return "CONVERT(" + tableField + " USING gbk)" + " asc";
            } else {
                return "CONVERT(" + tableField + " USING gbk)" + " desc";
            }
        }).collect(Collectors.joining(StrPool.COMMA)) + ",id asc";
    }
}