package com.github.seregamorph.maven.test.core;

import com.github.seregamorph.maven.test.util.XmlUtils;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
    private final List<TestcaseFailure> testcaseFlakyErrors;
    private final List<TestcaseFailure> testcaseFlakyFailures;
    private final List<TestcaseFailure> testcaseErrors;
    private final List<TestcaseFailure> testcaseFailures;

    public TestSuiteReport(
        String name,
        BigDecimal timeSeconds,
        int tests,
        int errors,
        int failures,
        List<TestcaseFailure> testcaseFlakyErrors,
        List<TestcaseFailure> testcaseFlakyFailures,
        List<TestcaseFailure> testcaseErrors,
        List<TestcaseFailure> testcaseFailures
    ) {
        this.name = name;
        this.timeSeconds = timeSeconds;
        this.tests = tests;
        this.errors = errors;
        this.failures = failures;
        this.testcaseFlakyErrors = testcaseFlakyErrors;
        this.testcaseFlakyFailures = testcaseFlakyFailures;
        this.testcaseErrors = testcaseErrors;
        this.testcaseFailures = testcaseFailures;
    }

    public static SortedMap<File, TestSuiteReport> fromDirectory(File reportsDirectory) {
        File[] testReportFiles = reportsDirectory.listFiles((dir, name) ->
            name.startsWith("TEST-") && name.endsWith(".xml"));

        if (testReportFiles == null) {
            testReportFiles = new File[0];
        }

        SortedMap<File, TestSuiteReport> reports = new TreeMap<>();
        for (File testReportFile : testReportFiles) {
            TestSuiteReport testSuiteSummary = fromFile(testReportFile);
            reports.put(testReportFile, testSuiteSummary);
        }
        return reports;
    }

    static TestSuiteReport fromFile(File file) {
        Document root = XmlUtils.parseXml(file);
        Element rootElement = root.getDocumentElement();
        String tagName = rootElement.getTagName();
        if (!"testsuite".equals(tagName)) {
            throw new IllegalArgumentException("Not a test suite report file: "
                + file + " with root element '" + tagName + "'");
        }

        // test class name
        String testSuiteName = rootElement.getAttribute("name");
        BigDecimal timeSeconds = new BigDecimal(rootElement.getAttribute("time"));
        int tests = Integer.parseInt(rootElement.getAttribute("tests"));
        int errors = Integer.parseInt(rootElement.getAttribute("errors"));
        int failures = Integer.parseInt(rootElement.getAttribute("failures"));

        NodeList testcaseList = rootElement.getElementsByTagName("testcase");
        List<TestcaseFailure> testcaseFlakyErrors = new ArrayList<>();
        List<TestcaseFailure> testcaseFlakyFailures = new ArrayList<>();
        List<TestcaseFailure> testcaseErrors = new ArrayList<>();
        List<TestcaseFailure> testcaseFailures = new ArrayList<>();
        for (int i = 0; i < testcaseList.getLength(); i++) {
            Element testcaseNode = (Element) testcaseList.item(i);

            // Any other than AssertionError
            testcaseFlakyErrors.addAll(getTestcaseFailures(testSuiteName, testcaseNode, "flakyError"));
            // AssertionError
            testcaseFlakyFailures.addAll(getTestcaseFailures(testSuiteName, testcaseNode, "flakyFailure"));
            testcaseErrors.addAll(getTestcaseFailures(testSuiteName, testcaseNode, "error"));
            testcaseFailures.addAll(getTestcaseFailures(testSuiteName, testcaseNode, "failure"));
        }

        return new TestSuiteReport(testSuiteName, timeSeconds, tests, errors, failures,
            testcaseFlakyErrors, testcaseFlakyFailures, testcaseErrors, testcaseFailures);
    }

    private static List<TestcaseFailure> getTestcaseFailures(String testSuiteName, Element testcaseNode, String tagName) {
        List<TestcaseFailure> failures = new ArrayList<>();
        NodeList failureTagList = testcaseNode.getElementsByTagName(tagName);
        for (int j = 0; j < failureTagList.getLength(); j++) {
            String testcaseName = testcaseNode.getAttribute("name");
            // Note: we use testSuiteName (testsuite "name" attribute)
            // instead of failure tag "classname" attribute as it can be wrong
            // (e.g. for JUnit 4 reports "classname" and "name" attributes have
            // the same value of method name)
            Testcase testcase = new Testcase(testSuiteName, testcaseName);

            Element failureNode = (Element) failureTagList.item(j);
            String failureType = failureNode.getAttribute("type");
            String failureMessage = failureNode.getAttribute("message");

            TestcaseFailure failure = new TestcaseFailure(testcase, failureType, failureMessage);
            failures.add(failure);
        }
        return failures;
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

    public List<TestcaseFailure> testcaseFlakyErrors() {
        return testcaseFlakyErrors;
    }

    public List<TestcaseFailure> testcaseFlakyFailures() {
        return testcaseFlakyFailures;
    }

    public List<TestcaseFailure> testcaseErrors() {
        return testcaseErrors;
    }

    public List<TestcaseFailure> testcaseFailures() {
        return testcaseFailures;
    }

    @Override
    public String toString() {
        return "TestSuiteReport{" +
            "name='" + name + '\'' +
            ", timeSeconds=" + timeSeconds +
            ", tests=" + tests +
            ", errors=" + errors +
            ", failures=" + failures +
            ", testcaseFlakyErrors=" + testcaseFlakyErrors +
            ", testcaseFlakyFailures=" + testcaseFlakyFailures +
            ", testcaseErrors=" + testcaseErrors +
            ", testcaseFailures=" + testcaseFailures +
            '}';
    }
}
