package com.envision.epc.module.project.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.epc.module.project.domain.Project;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:37
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
