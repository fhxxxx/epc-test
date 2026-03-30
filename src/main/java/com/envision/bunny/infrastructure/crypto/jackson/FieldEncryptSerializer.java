package com.envision.bunny.infrastructure.crypto.jackson;

import com.envision.bunny.infrastructure.crypto.CryptoUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

/**
 * @author jingjing.dong
 * @since 2023/3/29-17:27
 */
public final class FieldEncryptSerializer extends JsonSerializer<Object> implements ContextualSerializer {

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(CryptoUtils.aes().encryptHex(String.valueOf(value)));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        FieldEncrypt annotation = property.getAnnotation(FieldEncrypt.class);
        Set<Class<?>> clz = ImmutableSet.of(String.class,Integer.class,Long.class);
        final Class<?> rawClass = property.getType().getRawClass();
        if (Objects.nonNull(annotation)&&clz.contains(rawClass)) {
            return this;
        }
        return prov.findValueSerializer(property.getType(), property);
    }

}
