package com.envision.epc.module.extract.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.epc.module.extract.domain.ExtractTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:39
 */
@Mapper
public interface ExtractTaskMapper extends BaseMapper<ExtractTask> {
}
