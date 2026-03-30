package com.envision.bunny.infrastructure.opslog;

import java.lang.annotation.*;

/**
 * 默认目标方法执行前的解析以#{}进行包裹，而执行后可以进行二次解析使用%{}进行包裹：
 * 1、success可以使用目标方法的返回结果
 * 2、fail 可以使用errorMsg
 * 所有除success fail其他都是执行前解析，可以使用入参 或 spring bean 用@beanName即可
 * Example：success = "#{#p0+',你好呀'+@serviceDemo.sayHi(#name)}，%{#_ret} 你好"
 * @author jingjing.dong
 * @since 2022/1/14-11:52
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface OperationLogAnnotation {
    // 操作日志的文本模板
    String success();
    // 操作日志失败的文本版本
    String fail() default "";
    // 操作日志的执行人
    String operator() default "";
    // 操作日志绑定的业务对象标识
    String bizNo();
    // 操作日志的种类
    String category() default "";
    // 记录日志的条件
    String condition() default "";
}
