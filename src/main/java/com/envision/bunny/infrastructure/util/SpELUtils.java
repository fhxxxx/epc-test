package com.envision.bunny.infrastructure.util;

import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;

/**
 * @author jingjing.dong
 * @since 2022/1/21-13:42
 */
public class SpELUtils {

    private static final ExpressionParser parser = new SpelExpressionParser();
    public static TemplateParserContext defaultTemplateParserContext = new TemplateParserContext();
    public static TemplateParserContext customerTemplateParserContext = new TemplateParserContext("%{", "}");
    private final MethodBasedEvaluationContext evaluationContext;

    public SpELUtils(Method method, Object[] arguments) {
        this.evaluationContext = new MethodBasedEvaluationContext(TypedValue.NULL, method, arguments,
                new DefaultParameterNameDiscoverer());
        evaluationContext.setBeanResolver(new BeanFactoryResolver(ApplicationContextUtils.getApplicationContext()));
    }

    public void successWithResult(Object ret) {
        evaluationContext.setVariable("_ret", ret);
    }

    public void failWithMsg(String errorMsg) {
        evaluationContext.setVariable("_errorMsg", errorMsg);
    }

    public String parseExpression(String raw) {
        Expression expression = parser.parseExpression(raw, defaultTemplateParserContext);
        return expression.getValue(evaluationContext, String.class);
    }

    public String parseExpression(String raw, TemplateParserContext templateParserContext) {
        Expression expression = parser.parseExpression(raw, templateParserContext);
        return expression.getValue(evaluationContext, String.class);
    }

    public static String simpleParse(String raw, Method method, Object[] arguments) {
        SpELUtils spELUtils = new SpELUtils(method,arguments);
        return spELUtils.parseExpression(raw);
    }
}
