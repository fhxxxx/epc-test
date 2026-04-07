package com.envision.epc.infrastructure.idempotent;

import java.lang.annotation.*;

/**
 * @author jingjing.dong
 * @since 2021/3/26-10:03
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    //注解自定义redis的key的前缀，后面拼接参数
    String prefix() default "";
    /**
     * 通过SpEL表达式来指定key值。
     * 使用方法：@Idempotent(SpELKey = "#user.name + #user.phone", prefix = "YourPrefix")
     */
    String spelKey() default "";
    //过期时间
    int expireSeconds() default 120;
}
