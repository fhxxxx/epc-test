package com.envision.epc.infrastructure.opslog;

import org.springframework.scheduling.annotation.Async;

import java.util.List;

/**
 * 持久化操作日志的方法 包括持久化、查询
 * @author jingjing.dong
 * @since 2022/1/20-14:41
 */
public interface IOperationLogService {
    /**
     * 保存log
     */
    @Async
    void record(OperationLog operationLog);

    List<OperationLog> queryLog(String bizKey);

    List<OperationLog> queryLogByBizNo(String bizNo);
}