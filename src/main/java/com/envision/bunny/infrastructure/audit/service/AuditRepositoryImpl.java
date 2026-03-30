package com.envision.bunny.infrastructure.audit.service;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.envision.bunny.infrastructure.audit.domain.Audit;
import com.envision.bunny.infrastructure.audit.domain.AuditRepository;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户操作记录表 服务实现类
 * </p>
 *
 * @author liang.liu7
 * @since 2025-03-04
 */
@Service
public class AuditRepositoryImpl extends ServiceImpl<AuditMapper, Audit> implements AuditRepository {

}
