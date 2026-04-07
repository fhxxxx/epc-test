package com.envision.epc.infrastructure.audit.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.epc.infrastructure.audit.domain.Audit;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户操作记录表 Mapper 接口
 * </p>
 *
 * @author liang.liu7
 * @since 2025-03-04
 */
@Mapper
public interface AuditMapper extends BaseMapper<Audit> {

}
