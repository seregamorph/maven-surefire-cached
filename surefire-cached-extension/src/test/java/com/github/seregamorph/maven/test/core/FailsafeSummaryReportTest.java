package com.github.seregamorph.maven.test.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import org.junit.jupiter.api.Test;

class FailsafeSummaryReportTest {

    @Test
    public void shouldParseFailsafeSummaryReport() {
        var testSuiteReport = FailsafeSummaryReport.fromFile(getResourceFile(
            "failsafe-reports/failsafe-summary.xml"));

        assertEquals(1, testSuiteReport.getCompleted());
        assertEquals(2, testSuiteReport.getErrors());
        assertEquals(3, testSuiteReport.getFailures());
        assertEquals(4, testSuiteReport.getSkipped());
        assertEquals(5, testSuiteReport.getFlakes());
    }

    private static File getResourceFile(String name) {
        var resource = TestSuiteReportTest.class.getClassLoader().getResource(name);
        assertNotNull(resource, "Resource not found: " + name);
        return new File(resource.getFile());
    }
}
