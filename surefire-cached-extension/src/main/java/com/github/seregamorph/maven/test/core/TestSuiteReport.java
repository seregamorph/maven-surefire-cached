package com.github.seregamorph.maven.test.core;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * surefire/failsafe report files TEST-*.xml ("testsuite" tag)
 *
 * @author Sergey Chernov
 */
public record TestSuiteReport(String name, BigDecimal time, int tests, int errors, int failures) {

    public static TestSuiteReport fromFile(File file) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            var docBuilder = factory.newDocumentBuilder();
            var root = docBuilder.parse(file);

            // test class name
            var name = root.getDocumentElement().getAttribute("name");
            var time = new BigDecimal(root.getDocumentElement().getAttribute("time"));
            var tests = Integer.parseInt(root.getDocumentElement().getAttribute("tests"));
            var errors = Integer.parseInt(root.getDocumentElement().getAttribute("errors"));
            var failures = Integer.parseInt(root.getDocumentElement().getAttribute("failures"));

            return new TestSuiteReport(name, time, tests, errors, failures);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException("Failed to read " + file, e);
        }
    }
}
