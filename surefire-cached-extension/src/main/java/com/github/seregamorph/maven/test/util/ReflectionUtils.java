package com.github.seregamorph.maven.test.util;

import java.lang.reflect.Method;

/**
 * @author Sergey Chernov
 */
public final class ReflectionUtils {

    public static <T> T callProtected(Object obj, Class<T> returnType, String name) {
        try {
            Method method = obj.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            return returnType.cast(method.invoke(obj));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T call(Object obj, Class<T> returnType, String name) {
        try {
            Method method = obj.getClass().getMethod(name);
            return returnType.cast(method.invoke(obj));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private ReflectionUtils() {
    }
}
