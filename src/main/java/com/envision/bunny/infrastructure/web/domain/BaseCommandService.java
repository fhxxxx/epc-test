package com.envision.bunny.infrastructure.web.domain;

import com.baomidou.mybatisplus.extension.service.IService;
import com.envision.bunny.infrastructure.mybatis.AuditingEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author jingjing.dong
 * @since 2024/4/29-18:24
 */
public class BaseCommandService<S extends IService<E>, E extends AuditingEntity> {
    @Autowired
    protected S repository;

    @Transactional(rollbackFor = Exception.class)
    public boolean insert(E entity) {
        return repository.save(entity);
    }

    public boolean insertBatch(List<E> entities) {
        return repository.saveBatch(entities);
    }

    public boolean delete(Long id) {
        return repository.removeById(id);
    }

    public boolean delete(List<Long> ids) {
        return repository.removeByIds(ids);
    }

    public boolean updateById(E entity) {
        return repository.updateById(entity);
    }

    public boolean saveOrUpdate(@RequestBody E entity) {
        return repository.saveOrUpdate(entity);
    }
}
