package com.envision.bunny.infrastructure.opslog;

import com.envision.bunny.infrastructure.util.SpELUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author jingjing.dong
 * @since 2022/1/14-17:18
 */
@Aspect
@Component
@Slf4j
public class OperationLogAspect {
    @Autowired
    IOperationLogService bizLogService;

    @Pointcut("@annotation(com.envision.bunny.infrastructure.opslog.OperationLogAnnotation)")
    public void operationLog() {
    }

    @Around("operationLog()")
    public void doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLogAnnotation annotation = AnnotationUtils.getAnnotation(method, OperationLogAnnotation.class);
        Object[] args = joinPoint.getArgs();
        SpELUtils spELUtils = new SpELUtils(method, args);
        OperationLog operationLog = null;
        MethodExecuteResult methodExecuteResult = new MethodExecuteResult(true, null, "");
        try {
            operationLog = OperationLog.initBeforeExecute(Objects.requireNonNull(annotation), spELUtils);
        } catch (Exception e) {
            log.error("log record parse exception", e);
        }
        try {
            Object ret = joinPoint.proceed();
            spELUtils.successWithResult(ret);
        } catch (Throwable e) {
            methodExecuteResult = new MethodExecuteResult(false, e, e.getMessage());
            spELUtils.failWithMsg(e.getMessage());
        }
        try {
            if (Objects.nonNull(operationLog) && operationLog.getCondition()) {
                String rawMsg = methodExecuteResult.isSuccess() ? operationLog.getSuccessMsg() : operationLog.getFailMsg();
                String msg = spELUtils.parseExpression(rawMsg, SpELUtils.customerTemplateParserContext);
                operationLog.setMessage(msg);
                operationLog.setSuccess(methodExecuteResult.isSuccess());
                bizLogService.record(operationLog);
            }
        } catch (Exception e) {
            //记录日志错误不要影响业务
            log.error("log record parse exception", e);
        }
        if (methodExecuteResult.throwable != null) {
            throw methodExecuteResult.throwable;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class MethodExecuteResult {
        private boolean success;
        private Throwable throwable;
        private String errorMsg;
    }
}
