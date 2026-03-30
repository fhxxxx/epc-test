package com.envision.bunny.infrastructure.mask;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;
import java.util.Objects;

/**
 * @author jingjing.dong
 * @since 2023/3/29-17:27
 */
public final class DataMaskingSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private MaskEnum dataMaskEnum;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(dataMaskEnum.function().apply(value));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        DataMask annotation = property.getAnnotation(DataMask.class);
        if (Objects.nonNull(annotation)&&Objects.equals(String.class, property.getType().getRawClass())) {
            this.dataMaskEnum = annotation.function();
            return this;
        }
        return prov.findValueSerializer(property.getType(), property);
    }

}
