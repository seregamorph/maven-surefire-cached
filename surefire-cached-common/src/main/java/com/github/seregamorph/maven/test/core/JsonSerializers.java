package com.github.seregamorph.maven.test.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * @author Sergey Chernov
 */
public final class JsonSerializers {

    private static final ObjectMapper mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .registerModules(ObjectMapper.findModules(JsonSerializers.class.getClassLoader()))
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static byte[] serialize(Object obj) {
        try {
            return mapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T deserialize(byte[] content, Class<T> type) {
        try {
            return mapper.readValue(content, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private JsonSerializers() {
    }
}
