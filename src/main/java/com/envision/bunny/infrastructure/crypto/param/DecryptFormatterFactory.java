package com.envision.bunny.infrastructure.crypto.param;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @author jingjing.dong
 * @since 2023/4/2-13:12
 */
@Component
public class DecryptFormatterFactory implements AnnotationFormatterFactory<DecryptAnnotation> {

    static Set<Class<?>> classSet = Sets.newHashSet(String.class, Long.class, Integer.class);

    @NotNull
    @Override
    public Set<Class<?>> getFieldTypes() {
        return classSet;
    }

    @NotNull
    @Override
    public Parser<?> getParser(@NotNull DecryptAnnotation annotation, @NotNull Class<?> fieldType) {
        return configureFormatterFrom(fieldType);
    }

    @NotNull
    @Override
    public Printer<?> getPrinter(@NotNull DecryptAnnotation annotation, @NotNull Class<?> fieldType) {
        return configureFormatterFrom(fieldType);
    }

    private Formatter<?> configureFormatterFrom(Class<?> fieldType) {
        if (fieldType == Long.class) {
            return new LongDecrypt();
        }
        if (fieldType == Integer.class) {
            return new IntegerDecrypt();
        }
        return new StringDecrypt();
    }
}