package com.github.seregamorph.maven.test.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
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
        .setDefaultPrettyPrinter(createPrettyPrinter())
        .registerModules(ObjectMapper.findModules(JsonSerializers.class.getClassLoader()))
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static byte[] serialize(Object obj) {
        try {
            return mapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String serializeAsString(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T deserialize(byte[] content, Class<T> type, String fileName) {
        try {
            return mapper.readValue(content, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to deserialize " + fileName + " " + e, e);
        }
    }

    private static PrettyPrinter createPrettyPrinter() {
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("  ", DefaultIndenter.SYS_LF);
        prettyPrinter.indentArraysWith(indenter);
        return prettyPrinter;
    }

    private JsonSerializers() {
    }
}
