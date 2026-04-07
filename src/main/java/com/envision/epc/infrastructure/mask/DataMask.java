package com.envision.epc.infrastructure.mask;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.*;

/**
 * @author jingjing.dong
 * @since 2023/3/29-17:25
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = DataMaskingSerializer.class)
public @interface DataMask {
    MaskEnum function();
}
