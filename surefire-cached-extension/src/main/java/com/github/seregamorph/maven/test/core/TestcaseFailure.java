package com.github.seregamorph.maven.test.core;

/**
 * @author Sergey Chernov
 */
public class TestcaseFailure {

    private final Testcase testcase;
    private final String type;
    private final String message;

    TestcaseFailure(Testcase testcase, String type, String message) {
        this.testcase = testcase;
        this.type = type;
        this.message = message;
    }

    public Testcase getTestcase() {
        return testcase;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "TestcaseFailure{" +
            "testcase=" + testcase +
            ", type='" + type + '\'' +
            ", message='" + message + '\'' +
            '}';
    }
}
