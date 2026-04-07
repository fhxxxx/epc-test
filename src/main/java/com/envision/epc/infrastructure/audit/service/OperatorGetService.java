package com.envision.epc.infrastructure.audit.service;

import com.envision.epc.infrastructure.security.SecurityUtils;
import com.envision.epc.infrastructure.util.ApplicationContextUtils;
import io.github.flyhero.easylog.service.IOperatorService;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * @author liang.liu
 * @since 2025/03/04
 */
@Service
public class OperatorGetService implements IOperatorService {
    @Override
    public String getOperator() {
        if(SecurityUtils.isAuthenticated()){
            return SecurityUtils.getCurrentUsername();
        }
        return "Anonymous";
    }

    @Override
    public String getTenant() {
        final Environment environment = ApplicationContextUtils.getBean(Environment.class);
        return environment.getProperty("spring.application.name");
    }
}
