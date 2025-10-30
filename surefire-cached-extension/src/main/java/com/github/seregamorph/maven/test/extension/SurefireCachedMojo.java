package com.github.seregamorph.maven.test.extension;

import com.github.seregamorph.maven.test.common.OutputArtifact;
import com.github.seregamorph.maven.test.common.PluginName;
import com.github.seregamorph.maven.test.common.TestTaskOutput;
import com.github.seregamorph.maven.test.core.TestSuiteReport;
import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
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
        File[] testReports = reportsDirectory.listFiles((dir, name) ->
            name.startsWith("TEST-") && name.endsWith(".xml"));

        if (testReports == null) {
            testReports = new File[0];
        }

        BigDecimal totalTestTimeSeconds = BigDecimal.ZERO;
        int totalTests = 0;
        int totalErrors = 0;
        int totalFailures = 0;
        int totalTestcaseFlakyErrors = 0;
        int totalTestcaseFlakyFailures = 0;
        int totalTestcaseErrors = 0;
        for (File testReport : testReports) {
            TestSuiteReport testSuiteSummary = TestSuiteReport.fromFile(testReport);
            totalTestTimeSeconds = totalTestTimeSeconds.add(testSuiteSummary.timeSeconds());
            totalTests += testSuiteSummary.tests();
            totalErrors += testSuiteSummary.errors();
            totalFailures += testSuiteSummary.failures();
            totalTestcaseFlakyErrors += testSuiteSummary.testcaseFlakyErrors();
            totalTestcaseFlakyFailures += testSuiteSummary.testcaseFlakyFailures();
            totalTestcaseErrors += testSuiteSummary.testcaseErrors();
            if (testSuiteSummary.errors() > 0 || testSuiteSummary.failures() > 0
                || testSuiteSummary.testcaseErrors() > 0) {
                log.warn("{} has errors or failures, skipping cache", testReport);
            } else if (testSuiteSummary.testcaseFlakyErrors() > 0
                || testSuiteSummary.testcaseFlakyFailures() > 0) {
                log.warn("{} has flaky errors", testReport);
            }
        }

        // artifacts are filled before saving
        Map<String, OutputArtifact> artifacts = new TreeMap<>();
        return new TestTaskOutput(startTime, endTime, getTotalTimeSeconds(startTime, endTime),
            totalTestTimeSeconds, totalTests, totalErrors, totalFailures, null,
            totalTestcaseFlakyErrors, totalTestcaseFlakyFailures, totalTestcaseErrors,
            artifacts);
    }
}
