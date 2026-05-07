package com.github.seregamorph.maven.test.util;

import tools.jackson.core.JacksonException;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Sergey Chernov
 */
public final class JsonSerializers {

    private static final JsonMapper mapper = JsonMapper.builder()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .defaultPrettyPrinter(createPrettyPrinter())
        .findAndAddModules()
        .build();

    public static byte[] serialize(Object obj) {
        return mapper.writeValueAsBytes(obj);
    }

    public static String serializeAsString(Object obj) {
        return mapper.writeValueAsString(obj);
    }

    public static <T> T deserialize(byte[] content, Class<T> type, String fileName) {
        try {
            return mapper.readValue(content, type);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to deserialize " + fileName, e);
        }
    }

    private static DefaultPrettyPrinter createPrettyPrinter() {
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("  ", DefaultIndenter.SYS_LF);
        prettyPrinter.indentArraysWith(indenter);
        return prettyPrinter;
    }

    private JsonSerializers() {
    }
}
