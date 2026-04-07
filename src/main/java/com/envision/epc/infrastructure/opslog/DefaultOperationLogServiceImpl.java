package com.envision.epc.infrastructure.opslog;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 默认的一个持久化操作日志的方法，仅仅打印出来了，后续完全可以持久化至数据库 记得进行异步操作
 * @author jingjing.dong
 * @since 2022/1/20-14:55
 */
@Slf4j
@Component
public class DefaultOperationLogServiceImpl implements IOperationLogService {

//    @Resource
//    private LogRecordMapper logRecordMapper;

    @Override
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(OperationLog operationLog) {
        log.info("【logRecord】log={}1", operationLog);
//        logRecordMapper.insertSelective(logRecord);
    }

    @Override
    public List<OperationLog> queryLog(String bizKey) {
//        return logRecordMapper.queryByBizKey(bizKey);
        return Lists.newArrayList();
    }

    @Override
    public List<OperationLog> queryLogByBizNo(String bizNo) {
//        return logRecordMapper.queryByBizNo(bizNo);
        return Lists.newArrayList();
    }
}

