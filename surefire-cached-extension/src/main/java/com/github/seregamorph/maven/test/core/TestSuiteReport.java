package com.github.seregamorph.maven.test.core;

import com.github.seregamorph.maven.test.util.XmlUtils;
import java.io.File;
import java.math.BigDecimal;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * surefire/failsafe report files TEST-*.xml ("testsuite" tag)
 *
 * @author Sergey Chernov
 */
public final class TestSuiteReport {

    private final String name;
    private final BigDecimal timeSeconds;
    private final int tests;
    private final int errors;
    private final int failures;

    public TestSuiteReport(String name, BigDecimal timeSeconds, int tests, int errors, int failures) {
        this.name = name;
        this.timeSeconds = timeSeconds;
        this.tests = tests;
        this.errors = errors;
        this.failures = failures;
    }

    public static TestSuiteReport fromFile(File file) {
        Document root = XmlUtils.parseXml(file);
        Element rootElement = root.getDocumentElement();
        String tagName = rootElement.getTagName();
        if (!"testsuite".equals(tagName)) {
            throw new IllegalArgumentException("Not a test suite report file: "
                + file + " with root element '" + tagName + "'");
        }

        // test class name
        String name = rootElement.getAttribute("name");
        BigDecimal timeSeconds = new BigDecimal(rootElement.getAttribute("time"));
        int tests = Integer.parseInt(rootElement.getAttribute("tests"));
        int errors = Integer.parseInt(rootElement.getAttribute("errors"));
        int failures = Integer.parseInt(rootElement.getAttribute("failures"));

        return new TestSuiteReport(name, timeSeconds, tests, errors, failures);
    }

    public String name() {
        return name;
    }

    public BigDecimal timeSeconds() {
        return timeSeconds;
    }

    public int tests() {
        return tests;
    }

    public int errors() {
        return errors;
    }

    public int failures() {
        return failures;
    }

    @Override
    public String toString() {
        return "TestSuiteReport[" +
            "name=" + name + ", " +
            "timeSeconds=" + timeSeconds + ", " +
            "tests=" + tests + ", " +
            "errors=" + errors + ", " +
            "failures=" + failures + ']';
    }
}
