package com.envision.epc.infrastructure.web.domain;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.envision.epc.infrastructure.crud.ApprenticeUtil;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import com.envision.epc.infrastructure.mybatis.BasicPagination;
import com.envision.epc.infrastructure.mybatis.BasicPaging;
import com.envision.epc.infrastructure.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author jingjing.dong
 * @since 2024/4/29-18:24
 */
public class BaseQueryService<S extends IService<E>, E extends AuditingEntity> {
    @Autowired
    protected S repository;

    public List<E> list(E entity) {
        QueryWrapper<E> queryWrapper = ApprenticeUtil.getQueryWrapper(entity);
        return repository.list(queryWrapper);
    }

    public BasicPagination<E> page(BasicPaging pagingQuery, E entity) {
        return page(pagingQuery, entity, Function.identity());
    }

    public <R> BasicPagination<R> page(BasicPaging pagingQuery, E entity, Function<E, R> function) {
        QueryWrapper<E> queryWrapper = ApprenticeUtil.getQueryWrapper(entity);
        //限制条件
        if (pagingQuery.getPageNum() < 1) {
            pagingQuery.setPageNum(1);
        }

        if (pagingQuery.getPageSize() > 100) {
            pagingQuery.setPageSize(100);
        }
        Page<E> page = new Page<>(pagingQuery.getPageNum(), pagingQuery.getPageSize());
        if (pagingQuery.needSort()) {
            final List<Pair<String, String>> allOrderPair = pagingQuery.getAllOrder();
            allOrderPair.forEach(pair -> {
                String tableField = ApprenticeUtil.getBbField(pair.getKey(),entity);
                if (CharSequenceUtil.equals(pair.getValue(),"+")){
                    queryWrapper.orderByAsc(tableField);
                } else {
                    queryWrapper.orderByDesc(tableField);
                }
            });
        }
        queryWrapper.orderByAsc("id");
        return BasicPagination.of(repository.page(page, queryWrapper), function);
    }

    public long count(E entity) {
        QueryWrapper<E> queryWrapper = ApprenticeUtil.getQueryWrapper(entity);
        return repository.count(queryWrapper);
    }

    public E getOne(E entity) {
        QueryWrapper<E> queryWrapper = ApprenticeUtil.getQueryWrapper(entity);
        return repository.getOne(queryWrapper, true);
    }

    public E getOne(E entity, boolean throwEx) {
        QueryWrapper<E> queryWrapper = ApprenticeUtil.getQueryWrapper(entity);
        return repository.getOne(queryWrapper, throwEx);
    }

    public Optional<E> getOneOpt(E entity) {
        QueryWrapper<E> queryWrapper = ApprenticeUtil.getQueryWrapper(entity);
        return repository.getOneOpt(queryWrapper, true);
    }

    public Optional<E> getOneOpt(E entity, boolean throwEx) {
        QueryWrapper<E> queryWrapper = ApprenticeUtil.getQueryWrapper(entity);
        return repository.getOneOpt(queryWrapper, throwEx);
    }
}
