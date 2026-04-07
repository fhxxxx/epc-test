package com.envision.epc.infrastructure.audit.service;

import com.envision.epc.infrastructure.audit.domain.AuditAssembler;
import com.envision.epc.infrastructure.audit.domain.AuditRepository;
import io.github.flyhero.easylog.model.EasyLogInfo;
import io.github.flyhero.easylog.service.ILogRecordService;
import io.github.flyhero.easylog.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author liang.liu
 * @since 2025/03/04
 */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OpLogRecordService implements ILogRecordService {

    private final AuditRepository  repository;
    private final AuditAssembler assembler;

    @Override
    @Async
    public void record(EasyLogInfo easyLogInfo) {
        log.info("hello easy-log:{}", JsonUtils.toJSONString(easyLogInfo));
        easyLogInfo.setResult(null);
        repository.save(assembler.toAudit(easyLogInfo));
    }
}
