package com.envision.epc.infrastructure.mybatis.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author jingjing.dong
 * @since 2024/4/17-22:12
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MyDataPerm {
    /**
     * 表别名设置
     */
    String alias() default "";

    /**
     * 数据权限表字段名
     */
    String dataId() default "";
}
