package com.github.seregamorph.maven.test.extension;

import com.github.seregamorph.maven.test.common.FlakyFailure;
import com.github.seregamorph.maven.test.common.OutputArtifact;
import com.github.seregamorph.maven.test.common.PluginName;
import com.github.seregamorph.maven.test.common.TestTaskOutput;
import com.github.seregamorph.maven.test.core.TestSuiteReport;
import com.github.seregamorph.maven.test.core.TestcaseFailure;
import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;

/**
 * Delegates to org.apache.maven.plugin.surefire.SurefireMojo (surefire:test)
 * (org.apache.maven.plugin.surefire.SurefirePlugin for older maven-surefire-plugin versions)
 *
 * @author Sergey Chernov
 */
class SurefireCachedMojo extends AbstractCachedSurefireMojo {

    SurefireCachedMojo(
        TestTaskCacheHelper testTaskCacheHelper,
        MavenSession session,
        MavenProject project,
        Mojo delegate
    ) {
        super(testTaskCacheHelper, session, project, delegate, PluginName.SUREFIRE_CACHED);
    }

    @Override
    TestTaskOutput getTaskOutput(Instant startTime, Instant endTime) {
        Map<File, TestSuiteReport> testReports = TestSuiteReport.fromDirectory(reportsDirectory);
        BigDecimal totalTestTimeSeconds = BigDecimal.ZERO;
        int totalTests = 0;
        int totalErrors = 0;
        int totalFailures = 0;
        List<FlakyFailure> testcaseFlakyErrors = new ArrayList<>();
        List<FlakyFailure> testcaseFlakyFailures = new ArrayList<>();
        List<FlakyFailure> testcaseErrors = new ArrayList<>();
        for (Map.Entry<File, TestSuiteReport> entry : testReports.entrySet()) {
            File testReport = entry.getKey();
            TestSuiteReport testSuiteSummary = entry.getValue();
            totalTestTimeSeconds = totalTestTimeSeconds.add(testSuiteSummary.timeSeconds());
            totalTests += testSuiteSummary.tests();
            totalErrors += testSuiteSummary.errors();
            totalFailures += testSuiteSummary.failures();
            testcaseFlakyErrors.addAll(formatFailures(testSuiteSummary.testcaseFlakyErrors()));
            testcaseFlakyFailures.addAll(formatFailures(testSuiteSummary.testcaseFlakyFailures()));
            testcaseErrors.addAll(formatFailures(testSuiteSummary.testcaseErrors()));
            if (testSuiteSummary.errors() > 0 || testSuiteSummary.failures() > 0
                || !testSuiteSummary.testcaseErrors().isEmpty()) {
                log.warn("{} has errors or failures, skipping cache", testReport);
            } else if (!testSuiteSummary.testcaseFlakyErrors().isEmpty()
                || !testSuiteSummary.testcaseFlakyFailures().isEmpty()) {
                log.warn("{} has flaky errors", testReport);
            }
        }

        // artifacts are filled before saving
        Map<String, OutputArtifact> artifacts = new TreeMap<>();
        return new TestTaskOutput(startTime, endTime, getTotalTimeSeconds(startTime, endTime),
            totalTestTimeSeconds, totalTests, totalErrors, totalFailures, null,
            testcaseFlakyErrors, testcaseFlakyFailures, testcaseErrors,
            artifacts);
    }

    static List<FlakyFailure> formatFailures(List<TestcaseFailure> failures) {
        List<FlakyFailure> result = new ArrayList<>();
        for (TestcaseFailure failure : failures) {
            FlakyFailure flakyFailure = new FlakyFailure(failure.getTestcase().getClassname(),
                failure.getTestcase().getName());
            result.add(flakyFailure);
        }
        return result;
    }
}
