package com.envision.epc.infrastructure.mybatis;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StopWatch;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 * @author MySqlInterceptor
 * @since 2024/08/02-14:48
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
@Slf4j
public class MySqlInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object proceed = invocation.proceed();
        stopWatch.stop();
        String printSql = null;
        try {
            printSql = generateSql(invocation);
        } catch (Exception exception) {
            log.error("获取sql异常", exception);
        } finally {
            log.info("执行SQL耗时：[{}ms] 执行SQL：[{}]", stopWatch.getTotalTimeMillis(), printSql);
        }
        return proceed;
    }

    private static String generateSql(Invocation invocation) {
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        Object[] args = invocation.getArgs();
        Configuration configuration = statement.getConfiguration();
        Object parameter;
        BoundSql boundSql;
        if (args.length <= 1) {
            throw new RuntimeException("参数数量小于1");
        }
        if (args.length>=6 &&  args[5] instanceof BoundSql){
            boundSql = (BoundSql) args[5];
        } else {
            parameter = args[1];
            boundSql = statement.getBoundSql(parameter);
        }
        return buildSql(configuration, boundSql);
    }

    private static String buildSql(Configuration configuration, BoundSql boundSql) {
        String sql = boundSql.getSql();
        sql = sql.replaceAll("\\s+", " ");
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> params = boundSql.getParameterMappings();
        if (!ObjectUtils.isEmpty(params) && !ObjectUtils.isEmpty(parameterObject)) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(parameterObject)));
            } else {
                for (ParameterMapping param : params) {
                    String propertyName = param.getProperty();
                    MetaObject metaObject = configuration.newMetaObject(parameterObject);
                    if (metaObject.hasGetter(propertyName)) {
                        Object obj = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                    } else {
                        sql = sql.replaceFirst("\\?", "缺失");
                    }
                }
            }
        }
        return sql;
    }

    private static String getParameterValue(Object object) {
        String value = "";
        if (object instanceof String) {
            value = "'" + object + "'";
        } else if (object instanceof Date) {
            DateFormat format = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            value = "'" + format.format((Date) object) + "'";
        } else if (!ObjectUtils.isEmpty(object)) {
            value = object.toString();
        }
        return value;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }
}
