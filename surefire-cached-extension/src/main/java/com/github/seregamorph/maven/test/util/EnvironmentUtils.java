package com.github.seregamorph.maven.test.util;

/**
 * @author Sergey Chernov
 */
public class EnvironmentUtils {

    public static boolean isCi() {
        return "true".equals(System.getenv("CI"));
    }
}
