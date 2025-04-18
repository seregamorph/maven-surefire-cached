package com.github.seregamorph.maven.test.core;

final class ReflectionUtils {

    static <T> T call(Object obj, Class<T> returnType, String name) {
        try {
            var method = obj.getClass().getMethod(name);
            return returnType.cast(method.invoke(obj));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private ReflectionUtils() {
    }
}
