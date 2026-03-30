package com.envision.bunny.demo.integration.es.infrastructure;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.envision.bunny.demo.integration.es.domain.Faq;
import com.envision.bunny.demo.integration.es.domain.FaqService;
import org.springframework.stereotype.Service;

/**
 * @author jingjing.dong
 * @since 2021/4/26-16:56
 */
@Service
public class FaqServiceImpl extends ServiceImpl<FaqMapper, Faq> implements FaqService {

}