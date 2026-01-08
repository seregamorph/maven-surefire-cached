package com.github.seregamorph.maven.test.core;

/**
 * @author Sergey Chernov
 */
public class Testcase {

    private final String classname;
    private final String name;

    Testcase(String classname, String name) {
        this.classname = classname;
        this.name = name;
    }

    public String getClassname() {
        return classname;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Testcase{" +
            "classname='" + classname + '\'' +
            ", name='" + name + '\'' +
            '}';
    }
}
