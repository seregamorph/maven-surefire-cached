package com.github.seregamorph.maven.test.core;

import static com.github.seregamorph.maven.test.TestFileUtils.getResourceFile;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        assertThat(testSuiteReport.getFailureMessage())
            .startsWith("org.apache.maven.surefire.booter.SurefireBooterForkException: The forked VM "
                + "terminated without properly saying goodbye. VM crash or System.exit called?");
    }

    @Test
    public void shouldParseFailsafeSummarySuccessReport() {
        var testSuiteReport = FailsafeSummaryReport.fromFile(getResourceFile(
            "failsafe-reports/failsafe-summary-success.xml"));

        assertEquals(16, testSuiteReport.getCompleted());
        assertEquals(0, testSuiteReport.getErrors());
        assertEquals(0, testSuiteReport.getFailures());
        assertEquals(0, testSuiteReport.getSkipped());
        assertEquals(0, testSuiteReport.getFlakes());
        assertNull(testSuiteReport.getFailureMessage());
    }
}
