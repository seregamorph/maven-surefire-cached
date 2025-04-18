package com.github.seregamorph.maven.test.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.seregamorph.maven.test.core.TestSuiteReport;
import java.io.File;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TestSuiteReportTest {

    @Test
    public void testFailsafeReports() {
        var testSuiteReport = TestSuiteReport.fromFile(getResourceFile(
            "failsafe-reports/TEST-com.github.seregamorph.testsmartcontext.demo.SampleIT.xml"));

        assertEquals("com.github.seregamorph.testsmartcontext.demo.SampleIT", testSuiteReport.name());
        assertEquals(new BigDecimal("0.155"), testSuiteReport.time());
        assertEquals(1, testSuiteReport.tests());
        assertEquals(0, testSuiteReport.errors());
        assertEquals(0, testSuiteReport.failures());
    }

    @Test
    public void testSurefireReports() {
        var testSuiteReport = TestSuiteReport.fromFile(getResourceFile(
            "surefire-reports/TEST-com.github.seregamorph.testsmartcontext.demo.Unit1Test.xml"));

        assertEquals("com.github.seregamorph.testsmartcontext.demo.Unit1Test", testSuiteReport.name());
        assertEquals(new BigDecimal("0.005"), testSuiteReport.time());
        assertEquals(1, testSuiteReport.tests());
        assertEquals(0, testSuiteReport.errors());
        assertEquals(0, testSuiteReport.failures());
    }

    private static File getResourceFile(String name) {
        var resource = TestSuiteReportTest.class.getClassLoader().getResource(name);
        assertNotNull(resource, "Resource not found: " + name);
        return new File(resource.getFile());
    }
}
