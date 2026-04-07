package com.envision.epc.infrastructure.mask;

import java.util.function.Function;

/**
 * @author jingjing.dong
 * @since 2023/3/29-17:26
 */
public enum MaskEnum {

    /**
     * 名称脱敏
     */
    USERNAME(s -> s.replaceAll("(\\S)\\S(\\S*)", "$1*$2"))
    ,
    /**
     * Phone sensitive type.
     */
    PHONE(s -> s.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"))
    ,
    /**
     * Address sensitive type.
     */
    ADDRESS(s -> s.replaceAll("(\\S{3})\\S{2}(\\S*)\\S{2}", "$1****$2****"))
    ;

    /**
     * 成员变量  是一个接口类型
     */
    private final Function<String, String> function;

    MaskEnum(Function<String, String> function) {
        this.function = function;
    }

    public Function<String, String> function() {
        return this.function;
    }

}


