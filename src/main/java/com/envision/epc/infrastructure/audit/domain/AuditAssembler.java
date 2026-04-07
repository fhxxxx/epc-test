package com.envision.epc.infrastructure.audit.domain;

import io.github.flyhero.easylog.model.EasyLogInfo;
import org.mapstruct.Mapper;

/**
 * @author liang.liu
 * @since 2025/03/04
 */
@Mapper(componentModel = "spring")
public abstract class AuditAssembler {

    public abstract Audit toAudit(EasyLogInfo easyLogInfo);
}
