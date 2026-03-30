package com.envision.bunny.infrastructure.opslog;

import com.envision.bunny.infrastructure.util.SpELUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author jingjing.dong
 * @since 2022/1/20-13:57
 */
@Getter
@Setter
@ToString(exclude = {"successMsg", "failMsg"})
public class OperationLog {
    // 解析后的操作日志的文本
    private String message;
    // 成功的文本
    private String successMsg;
    // 失败的文本
    private String failMsg;
    // 操作日志的执行人
    private String operator;
    // 操作日志绑定的业务对象标识
    private String bizNo;
    // 操作日志的种类
    private String category;
    // 是否执行成功
    private Boolean success;
    // 是否需要进行记录
    private Boolean condition;
    // 操作日志绑定的业务对象标识
    private String bizKey;

    static OperationLog initBeforeExecute(OperationLogAnnotation annotation, SpELUtils spELUtils) {
        OperationLog operationLog = new OperationLog();
        if (StringUtils.isEmpty(annotation.condition())) {
            operationLog.condition = true;
        } else {
            String conditionValue = spELUtils.parseExpression(annotation.condition());
            operationLog.condition = "true".equalsIgnoreCase(conditionValue);
        }
        if (!operationLog.condition) {
            return operationLog;
        }
        operationLog.success = true;
        operationLog.bizNo = spELUtils.parseExpression(annotation.bizNo());
        operationLog.successMsg = spELUtils.parseExpression(annotation.success());
        operationLog.failMsg = spELUtils.parseExpression(annotation.fail());
        if (StringUtils.isNotBlank(annotation.operator())) {
            operationLog.operator = spELUtils.parseExpression(annotation.operator());
        } else {
            operationLog.operator = SecurityContextHolder.getContext().getAuthentication().getName();
        }
        operationLog.category = spELUtils.parseExpression(annotation.category());
        operationLog.bizKey = operationLog.category + "_" + operationLog.bizNo;
        return operationLog;
    }
}
