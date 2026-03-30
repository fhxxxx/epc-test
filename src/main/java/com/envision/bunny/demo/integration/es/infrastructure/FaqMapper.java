package com.envision.bunny.demo.integration.es.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.bunny.demo.integration.es.domain.Faq;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author jingjing.dong
 * @since 2021/4/26-15:30
 */
@Mapper
public interface FaqMapper extends BaseMapper<Faq> {
}
