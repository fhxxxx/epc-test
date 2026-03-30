package com.envision.bunny.infrastructure.crypto.jackson;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.lang.annotation.*;

/**
 * @author jingjing.dong
 * @since 2023/3/29-17:25
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonDeserialize(using = FieldDecryptDeserializer.class)
public @interface FieldDecrypt {
}
