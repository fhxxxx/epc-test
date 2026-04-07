package com.envision.epc.infrastructure.crypto.jackson;

import cn.hutool.crypto.CryptoException;
import com.envision.epc.infrastructure.crypto.CryptoUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;
import java.util.Objects;

/**
 * @author jingjing.dong
 * @since 2023/3/29-17:27
 */
public final class FieldDecryptDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {

    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctx) {
        return null;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext prov, BeanProperty property) throws JsonMappingException {
        FieldDecrypt annotation = property.getAnnotation(FieldDecrypt.class);
        if (Objects.isNull(annotation)) {
            return prov.findContextualValueDeserializer(property.getType(), property);
        }
        final Class<?> rawClass = property.getType().getRawClass();
        if (Objects.equals(String.class, rawClass)) {
            return new StringJsonDeserializer();
        }
        if (Objects.equals(Long.class, rawClass)) {
            return new LongJsonDeserializer();
        }
        if (Objects.equals(Integer.class, rawClass)) {
            return new IntegerJsonDeserializer();
        }
        return prov.findContextualValueDeserializer(property.getType(), property);
    }

    static class StringJsonDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
            String rawText = jp.getText();
            try {
                return CryptoUtils.aes().decryptStr(rawText);
            } catch (CryptoException ex) {
                return rawText;
            }
        }
    }

    static class LongJsonDeserializer extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
            String rawText = jp.getText();
            try {
                final String decryptText = CryptoUtils.aes().decryptStr(rawText);
                return Long.valueOf(decryptText);
            } catch (CryptoException ex) {
                return Long.valueOf(rawText);
            }
        }
    }

    static class IntegerJsonDeserializer extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
            String rawText = jp.getText();
            try {
                final String decryptText = CryptoUtils.aes().decryptStr(rawText);
                return Integer.valueOf(decryptText);
            } catch (CryptoException ex) {
                return Integer.valueOf(rawText);
            }
        }
    }
}
